#!/usr/bin/env bash
# Rebuilds the JavaScript fixture src/test/resources/wasm/js/sample_encrypt.wasm with Javy.
#
# The WAT fixtures need no build step: ConformanceFixtures compiles them with Endive's wabt port
# (run.endive:wabt) when the tests run.
#
# Usage: build-fixtures.sh [path/to/javy]
#   Without an argument, downloads Javy v9.1.0 for this platform into a temporary directory and
#   checks its published SHA-256.
set -euo pipefail

JAVY_VERSION=v9.1.0
here="$(cd "$(dirname "$0")" && pwd)"
js_dir="$here/src/test/resources/wasm/js"

javy="${1:-}"
if [[ -z "$javy" ]]; then
  case "$(uname -s)-$(uname -m)" in
    Darwin-arm64) asset="javy-arm-macos-$JAVY_VERSION" ;;
    Darwin-x86_64) asset="javy-x86_64-macos-$JAVY_VERSION" ;;
    Linux-x86_64) asset="javy-x86_64-linux-$JAVY_VERSION" ;;
    Linux-aarch64) asset="javy-arm-linux-$JAVY_VERSION" ;;
    *) echo "no Javy build for $(uname -s)-$(uname -m); pass the javy binary as an argument" >&2; exit 1 ;;
  esac
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  gh release download "$JAVY_VERSION" --repo bytecodealliance/javy --pattern "$asset.gz*" --dir "$tmp"
  expected="$(cut -d' ' -f1 < "$tmp/$asset.gz.sha256")"
  actual="$(shasum -a 256 "$tmp/$asset.gz" | cut -d' ' -f1)"
  if [[ "$expected" != "$actual" ]]; then
    echo "SHA-256 mismatch for $asset.gz: expected $expected, got $actual" >&2
    exit 1
  fi
  gunzip "$tmp/$asset.gz"
  chmod +x "$tmp/$asset"
  javy="$tmp/$asset"
fi

"$javy" --version
# Static build: the module embeds QuickJS and needs no plugin at run time.
"$javy" build -o "$js_dir/sample_encrypt.wasm" "$js_dir/sample_encrypt.js"
ls -l "$js_dir/sample_encrypt.wasm"
