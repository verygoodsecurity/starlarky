#!/usr/bin/env bash
# Rebuilds the JavaScript fixture larky-wasm/src/test/resources/wasm/js/sample_encrypt.wasm with Javy.
#
# The WAT fixtures need no build step: Fixtures compiles them with Endive's wabt port
# (run.endive:wabt) when the tests run.
#
# Usage: build-fixtures.sh [path/to/javy]
#   Builds a SIMD-free Javy plugin first (build-javy-plugin.sh; installs Rust in a temp dir).
#   Without an argument, uses the Javy v9.1.0 CLI that build-javy-plugin.sh downloads (pinned by
#   SHA-256).
set -euo pipefail

JAVY_VERSION=v9.1.0
here="$(cd "$(dirname "$0")" && pwd)"
js_dir="$here/../src/test/resources/wasm/js"

# Build against a QuickJS plugin compiled without WebAssembly SIMD (see build-javy-plugin.sh), so
# both runtimes run the module on any supported JDK. The module still embeds QuickJS (a static
# build) and needs no plugin at run time.
javy="${1:-}"
plugin_dir="$(mktemp -d)"
trap 'rm -rf "$plugin_dir"' EXIT
JAVY="$javy" "$here/build-javy-plugin.sh" "$plugin_dir/plugin-nosimd.wasm" "$plugin_dir/work"
if [[ -z "$javy" ]]; then
  javy="$(ls "$plugin_dir"/work/javy-*-"$JAVY_VERSION")"
fi
"$javy" --version
"$javy" build -C plugin="$plugin_dir/plugin-nosimd.wasm" \
  -o "$js_dir/sample_encrypt.wasm" "$js_dir/sample_encrypt.js"
ls -l "$js_dir/sample_encrypt.wasm"
