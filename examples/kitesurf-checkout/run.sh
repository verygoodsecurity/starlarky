#!/usr/bin/env bash
set -euo pipefail

: "${KITESURF_SOURCE:?Set KITESURF_SOURCE to the built Kitesurf source directory}"
fixture_dir="$(cd "$(dirname "$0")" && pwd -P)"
engine_dir="$(cd "$KITESURF_SOURCE" && pwd -P)"
port="${KITESURF_DEMO_PORT:-18788}"
exec docker run --init --rm --name "${KITESURF_DEMO_CONTAINER:-larky-kitesurf-checkout}" \
  -p "127.0.0.1:${port}:18788" -e WRANGLER_SEND_METRICS=false \
  --mount "type=bind,source=$engine_dir,target=/source,readonly" \
  --mount "type=bind,source=$fixture_dir,target=/fixture,readonly" \
  node:24.13.0-bookworm-slim sh -ec '
    mkdir -p /tmp/kitesurf
    tar -C /source --exclude=node_modules --exclude=.git --exclude=.env --exclude=".env.*" \
      --exclude=.tools --exclude=.simba --exclude=.wrangler --exclude="bazel-*" -cf - . \
      | tar -C /tmp/kitesurf -xf -
    node /fixture/fixture.mjs &
    cd /tmp/kitesurf
    corepack pnpm install --frozen-lockfile
    cd packages/engine
    cp /fixture/engine.ts src/checkout-fixture.ts
    sed -i "s#src/index.ts#src/checkout-fixture.ts#" wrangler.jsonc
    export MINIFLARE_WORKERD_V8_FLAGS=--expose-gc
    exec corepack pnpm exec wrangler dev --env local -c wrangler.jsonc \
      -c test/fixtures-worker/wrangler.jsonc --ip 0.0.0.0 --port 18788 --inspector-port 19230
  '
