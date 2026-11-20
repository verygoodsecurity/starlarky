import { WorkerEntrypoint } from 'cloudflare:workers';
import { KitesurfEngine } from './index';
export * from './index';

export class CheckoutFixtureEgress extends WorkerEntrypoint {
  async fetch(request: Request) {
    if (new URL(request.url).origin !== 'http://localhost:3000' || request.method !== 'GET') {
      return new Response(null, { status: 403 });
    }
    return fetch(request, { redirect: 'manual' });
  }
}

export default class CheckoutFixtureEngine extends KitesurfEngine {
  protected override host() {
    return {
      egress(_env, ctx) {
        return ctx.exports.CheckoutFixtureEgress({});
      },
    };
  }
}
