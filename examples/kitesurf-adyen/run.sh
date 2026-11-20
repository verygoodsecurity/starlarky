#!/usr/bin/env bash
set -euo pipefail

: "${KITESURF_SOURCE:?Set KITESURF_SOURCE to the built Kitesurf source directory}"
: "${ADYEN_CLIENT_KEY:?Export a test client key for localhost:3000}"
[[ "$ADYEN_CLIENT_KEY" == test_* ]] || { echo "An Adyen test client key is required" >&2; exit 1; }

fixture_dir="$(cd "$(dirname "$0")" && pwd -P)"
engine_dir="$(cd "$KITESURF_SOURCE" && pwd -P)"
port="${KITESURF_DEMO_PORT:-18788}"
exec docker run --init --rm --name "${KITESURF_DEMO_CONTAINER:-larky-kitesurf-adyen}" \
  -p "127.0.0.1:${port}:18788" -e ADYEN_CLIENT_KEY -e WRANGLER_SEND_METRICS=false \
  --mount "type=bind,source=$engine_dir,target=/source,readonly" \
  --mount "type=bind,source=$fixture_dir,target=/fixture,readonly" \
  node:24.13.0-bookworm-slim sh -ec '
    mkdir -p /tmp/kitesurf /tmp/demo
    cp /fixture/*.js /fixture/*.mjs /fixture/*.html /fixture/engine.ts /fixture/package*.json /tmp/demo/
    tar -C /source --exclude=node_modules --exclude=.git --exclude=.env --exclude=".env.*" \
      --exclude=.tools --exclude=.simba --exclude=.wrangler --exclude="bazel-*" -cf - . \
      | tar -C /tmp/kitesurf -xf -
    cd /tmp/demo
    npm ci --no-audit --no-fund
    npm run build
    node fixture.mjs &
    cd /tmp/kitesurf
    node /tmp/demo/configure-kitesurf.mjs
    corepack pnpm install --frozen-lockfile
    corepack pnpm --dir packages/page-script run build
    cd packages/engine
    export MINIFLARE_WORKERD_V8_FLAGS=--expose-gc
    exec corepack pnpm exec wrangler dev --env local -c wrangler.jsonc \
      -c test/fixtures-worker/wrangler.jsonc --ip 0.0.0.0 --port 18788 --inspector-port 19230
  '
