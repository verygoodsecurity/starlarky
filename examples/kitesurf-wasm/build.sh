#!/usr/bin/env bash
set -euo pipefail

revision=a58b15dfabc0a238a4ab37c9ba9c1813c865bf21
example_dir="$(cd "$(dirname "$0")" && pwd)"
output="${1:-$example_dir/../../target/kitesurf-guest.wasm}"
mkdir -p "$(dirname "$output")"
output="$(cd "$(dirname "$output")" && pwd)/$(basename "$output")"
scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT

curl --fail --silent --show-error --location \
  "https://codeload.github.com/cloudflare/kitesurf/tar.gz/$revision" \
  --output "$scratch/source.tar.gz"
mkdir "$scratch/source"
tar -xf "$scratch/source.tar.gz" -C "$scratch/source" --strip-components=1
cd "$scratch/source"
# Kitesurf carries patches for its native DOM, layout and JavaScript dependencies.
node scripts/get-patched-vendor-crates.mjs
cp -R "$example_dir/guest" packages/larky-guest
printf '\ninclude!("larky_wasi.rs");\n' >> packages/core/src/lib.rs
cp "$example_dir/core-wasi.rs" packages/core/src/larky_wasi.rs

node --input-type=module <<'NODE'
import { readFileSync, writeFileSync } from 'node:fs';
const manifest = readFileSync('Cargo.toml', 'utf8');
const anchor = 'members = ["packages/core",';
if (!manifest.includes(anchor)) throw new Error('Unexpected Kitesurf workspace');
writeFileSync('Cargo.toml', manifest.replace(anchor, 'members = ["packages/larky-guest", "packages/core",'));

const netPath = 'packages/core/src/resources/net.rs';
const net = readFileSync(netPath, 'utf8');
const request = '        let id = {';
if (net.split(request).length !== 2) throw new Error('Unexpected Kitesurf net provider');
writeFileSync(netPath, net.replace(request, `        // The WASI port currently owns only the top-level document fetch.
        if cfg!(target_os = "wasi") {
            settle_unfetchable(handler, url, infer_resource_kind(&request));
            return;
        }

` + request));

const lock = readFileSync('Cargo.lock', 'utf8');
const packageAnchor = '[[package]]\nname = "lazy_static"';
if (!lock.includes(packageAnchor)) throw new Error('Unexpected Kitesurf lockfile');
writeFileSync('Cargo.lock', lock.replace(packageAnchor,
  '[[package]]\nname = "larky-kitesurf-guest"\nversion = "0.1.0"\n'
  + 'dependencies = [\n "kitesurf-core",\n "serde_json",\n]\n\n' + packageAnchor));
NODE

export CARGO_TARGET_WASM32_WASIP1_RUSTFLAGS="-C target-cpu=mvp -C target-feature=+bulk-memory,+mutable-globals -C link-arg=--strip-all"
cargo build --locked --target wasm32-wasip1 --profile release-test -p larky-kitesurf-guest
cp target/wasm32-wasip1/release-test/larky_kitesurf_guest.wasm "$output"
printf 'Built %s\n' "$output"
