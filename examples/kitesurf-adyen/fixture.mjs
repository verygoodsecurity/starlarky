import { createServer } from 'node:http';
import { readFileSync } from 'node:fs';
import { randomUUID, timingSafeEqual } from 'node:crypto';

const clientKey = process.env.ADYEN_CLIENT_KEY;
if (!clientKey?.startsWith('test_')) throw new Error('Adyen test client key required');
const authorize = process.env.ADYEN_DEMO_AUTHORIZE === '1';
const {
  ADYEN_API_KEY: apiKey, ADYEN_MERCHANT_ACCOUNT: merchantAccount, ADYEN_DEMO_TOKEN: demoToken,
} = process.env;
if (authorize && (!apiKey || !merchantAccount || !/^[a-f0-9]{64}$/.test(demoToken ?? ''))) {
  throw new Error('Sandbox authorization needs an API key, merchant account, and a 32-byte hex demo token');
}

const allowedHosts = new Set([
  'checkoutshopper-test.adyen.com',
  'checkoutshopper-test.cdn.adyen.com',
  'checkoutanalytics-test.adyen.com',
]);
const encryptedFields = [
  'encryptedCardNumber',
  'encryptedExpiryMonth',
  'encryptedExpiryYear',
  'encryptedSecurityCode',
];
const assets = Object.fromEntries([
  ['/', 'text/html; charset=utf-8', './checkout.html'],
  ['/drop-in', 'text/html; charset=utf-8', './drop-in.html'],
  ['/checkout-bundle.js', 'application/javascript', './checkout-bundle.js'],
  ['/adyen.css', 'text/css', './node_modules/@adyen/adyen-web/dist/es/adyen.css'],
].map(([path, type, file]) => [path, [type, readFileSync(new URL(file, import.meta.url))]]));

function json(response, status, value) {
  response.writeHead(status, { 'Content-Type': 'application/json' });
  response.end(JSON.stringify(value));
}

async function relay(request, response) {
  const upstream = URL.parse(request.headers['x-demo-upstream'] ?? '');
  if (!upstream || upstream.origin !== `https://${upstream.hostname}`
      || !allowedHosts.has(upstream.hostname) || upstream.username || upstream.password) {
    return json(response, 403, { error: 'Unapproved test egress' });
  }
  const headers = new Headers(request.headers);
  for (const name of ['host', 'x-demo-upstream', 'connection', 'content-length', 'transfer-encoding']) {
    headers.delete(name);
  }
  try {
    const { method } = request;
    const options = { method, headers, redirect: 'manual', signal: AbortSignal.timeout(12000) };
    if (method !== 'GET' && method !== 'HEAD') {
      options.body = request;
      options.duplex = 'half';
    }
    const reply = await fetch(upstream, options);
    for (const [key, value] of reply.headers) {
      if (!['content-encoding', 'content-length', 'connection', 'transfer-encoding'].includes(key)) {
        response.setHeader(key, value);
      }
    }
    response.statusCode = reply.status;
    response.end(Buffer.from(await reply.arrayBuffer()));
  } catch {
    if (!response.headersSent) json(response, 502, { error: 'Test egress failed' });
    else response.end();
  }
}

function isCompactJwe(value) {
  if (typeof value !== 'string') return false;
  const parts = value.split('.');
  return parts.length === 5 && parts.every(part => /^[A-Za-z0-9_-]+$/.test(part));
}

async function sandboxPayment(paymentMethod, browserInfo) {
  const reference = `kitesurf-demo-${randomUUID()}`;
  const upstream = await fetch('https://checkout-test.adyen.com/v72/payments', {
    method: 'POST',
    redirect: 'error',
    signal: AbortSignal.timeout(12000),
    headers: {
      'content-type': 'application/json',
      'x-api-key': apiKey,
      'idempotency-key': reference,
    },
    body: JSON.stringify({
      merchantAccount,
      reference,
      amount: { currency: 'EUR', value: 1000 },
      paymentMethod,
      browserInfo,
      channel: 'Web',
      origin: 'http://localhost:3000',
      returnUrl: 'http://localhost:3000/',
    }),
  });
  const result = await upstream.json();
  if (!upstream.ok) {
    return { resultCode: 'SandboxRequestRejected', upstreamStatus: upstream.status,
      errorCode: /^[0-9]{3}$/.test(result.errorCode) ? result.errorCode : undefined };
  }
  return {
    resultCode: typeof result.resultCode === 'string' && /^[A-Za-z]+$/.test(result.resultCode)
      ? result.resultCode : 'UnexpectedSandboxResponse',
    sandboxAuthorised: result.resultCode === 'Authorised' && Boolean(result.pspReference),
  };
}

async function receipt(request, response) {
  if (authorize) {
    const token = Buffer.from(request.headers['x-demo-token'] ?? '');
    const expected = Buffer.from(demoToken);
    if (token.length !== expected.length || !timingSafeEqual(token, expected)) {
      return json(response, 403, { resultCode: 'UnauthorizedDemoCaller' });
    }
  }
  let body = '';
  try {
    for await (const data of request) {
      body += data;
      if (body.length > 16384) return json(response, 413, { resultCode: 'TooLarge' });
    }
    const data = JSON.parse(body);
    const method = data.paymentMethod ?? {};
    const present = encryptedFields.filter(name => isCompactJwe(method[name]));
    const valid = present.length === encryptedFields.length && !/5555[ -]?5555[ -]?5555[ -]?4444/.test(body);
    if (!valid) {
      return json(response, 422, { resultCode: 'InvalidEncryptedFields', encryptedFields: present });
    }
    if (!authorize) {
      return json(response, 200, { resultCode: 'EncryptedFieldsReceived', encryptedFields: present });
    }
    try {
      const result = await sandboxPayment(method, data.browserInfo);
      return json(response, 200, { ...result, encryptedFields: present });
    } catch {
      return json(response, 200, { resultCode: 'SandboxTransportFailed', encryptedFields: present });
    }
  } catch {
    return json(response, 400, { resultCode: 'InvalidPayload' });
  }
}

createServer(async (request, response) => {
  if (request.url === '/__demo_egress') return relay(request, response);
  if (request.url === '/api/payments' && request.method === 'POST') return receipt(request, response);
  if (request.url === '/config.js') {
    response.writeHead(200, { 'Content-Type': 'application/javascript' });
    return response.end('window.__ADYEN_CONFIG__ = ' + JSON.stringify({ clientKey }));
  }
  const asset = assets[request.url];
  if (asset) {
    response.writeHead(200, { 'Content-Type': asset[0] });
    return response.end(asset[1]);
  }
  response.writeHead(404);
  response.end();
}).listen(Number(process.env.ADYEN_FIXTURE_PORT ?? 3000), '127.0.0.1', () => console.log('Adyen demo fixture listening on loopback'));
