import { WorkerEntrypoint } from 'cloudflare:workers';
import { KitesurfEngine } from './index';
export * from './index';

export class AdyenDemoEgress extends WorkerEntrypoint {
  async fetch(request: Request) {
    const url = new URL(request.url);
    if (url.origin === 'http://localhost:3000') return fetch(request);
    const headers = new Headers(request.headers);
    headers.set('x-demo-upstream', url.href);
    return fetch('http://localhost:3000/__demo_egress', {
      method: request.method,
      headers,
      body: request.body,
      redirect: 'manual',
    });
  }
}

export default class AdyenDemoEngine extends KitesurfEngine {
  protected override host() {
    return {
      egress(_env, ctx) {
        return ctx.exports.AdyenDemoEgress({});
      },
    };
  }
}
