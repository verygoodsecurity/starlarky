import { createServer } from 'node:http';

const details = {
  document: ['Documento de identidad', 'text', 'off'],
  phone: ['Celular', 'tel', 'tel'],
  email: ['Correo electrónico', 'email', 'email'],
};

function checkout(alternate) {
  const next = alternate ? '/contact?layout=alternate' : '/contact';
  return `<h1>Demo clothing store</h1>
    <p>Shopping bag · 80,000 COP</p>
    <a href="${next}">Pay with Addi</a>`;
}

function contact(alternate) {
  const fields = alternate ? ['email', 'document', 'phone'] : ['document', 'phone', 'email'];
  const controls = fields.map((name, index) => {
    const [label, type, autocomplete] = details[name];
    const id = (alternate ? 'contact-' : 'billing-') + (index + 1);
    const input = `<input id="${id}" name="${id}" type="${type}" autocomplete="${autocomplete}" required>`;
    return alternate ? `<p><label>${label}${input}</label></p>` : `<p><label for="${id}">${label}</label>${input}</p>`;
  }).join('');
  return `<h1>Contact details</h1>
    <form method="post" action="/continue">
      ${controls}
      <button>Continue</button>
    </form>`;
}

createServer((request, response) => {
  if (request.method !== 'GET') {
    response.writeHead(405).end();
    return;
  }
  const url = new URL(request.url, 'http://localhost:3000');
  const alternate = url.searchParams.get('layout') === 'alternate';
  if (url.pathname !== '/' && url.pathname !== '/contact') {
    response.writeHead(404).end();
    return;
  }
  const content = url.pathname === '/' ? checkout(alternate) : contact(alternate);
  response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  response.end(`<!doctype html><html lang="es">
    <meta charset="utf-8"><title>Demo checkout</title>
    <style>
      body { font: 18px sans-serif; max-width: 28em; margin: 3em auto; }
      label { display: block; }
      input { display: block; width: 90%; margin-top: .3em; }
    </style>
    <main>${content}</main>
  </html>`);
}).listen(3000, '127.0.0.1', () => console.log('Checkout fixture listening on loopback'));
