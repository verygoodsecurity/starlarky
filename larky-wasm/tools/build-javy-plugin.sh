#!/usr/bin/env bash
# Builds Javy's QuickJS plugin without WebAssembly SIMD, for `javy build -C plugin=...`.
#
# Javy's workspace builds every wasm32-wasip1 crate with `-C target-feature=+simd128` (for
# simd-json; see its .cargo/config.toml), so its stock plugin, and every static module built from
# it, contains SIMD instructions. Endive runs those only on Java 25+ (and mis-runs simd-json there);
# a module built from this plugin has none, so both runtimes run it on any supported JDK.
#
# Each starlarky release also publishes the result as javy-plugin-nosimd-<javy version>.wasm.
#
# Usage: build-javy-plugin.sh OUTPUT [WORKDIR]
#   OUTPUT   where to write the initialized plugin (OUTPUT.sha256 gets its SHA-256)
#   WORKDIR  where to install Rust, clone Javy and download the CLI (default: a temporary directory)
# Set JAVY to a javy v9.1.0 CLI to use it instead of downloading one (the download is checked
# against its published SHA-256). Installs Rust into WORKDIR; it does not touch ~/.cargo or
# ~/.rustup.
set -euo pipefail

JAVY_VERSION=v9.1.0
output="$1"
work="${2:-$(mktemp -d)}"
mkdir -p "$work" "$(dirname "$output")"
work="$(cd "$work" && pwd)"
output="$(cd "$(dirname "$output")" && pwd)/$(basename "$output")"

sha256() {
  if command -v sha256sum >/dev/null; then sha256sum "$1"; else shasum -a 256 "$1"; fi | cut -d' ' -f1
}

javy="${JAVY:-}"
if [[ -z "$javy" ]]; then
  case "$(uname -s)-$(uname -m)" in
    Darwin-arm64) asset="javy-arm-macos-$JAVY_VERSION" ;;
    Darwin-x86_64) asset="javy-x86_64-macos-$JAVY_VERSION" ;;
    Linux-x86_64) asset="javy-x86_64-linux-$JAVY_VERSION" ;;
    Linux-aarch64 | Linux-arm64) asset="javy-arm-linux-$JAVY_VERSION" ;;
    *) echo "no Javy CLI for $(uname -s)-$(uname -m); set JAVY" >&2; exit 1 ;;
  esac
  if [[ ! -x "$work/$asset" ]]; then
    base="https://github.com/bytecodealliance/javy/releases/download/$JAVY_VERSION/$asset.gz"
    curl -sSfL -o "$work/$asset.gz" "$base"
    curl -sSfL -o "$work/$asset.gz.sha256" "$base.sha256"
    expected="$(cut -d' ' -f1 < "$work/$asset.gz.sha256")"
    actual="$(sha256 "$work/$asset.gz")"
    if [[ "$expected" != "$actual" ]]; then
      echo "SHA-256 mismatch for $asset.gz: expected $expected, got $actual" >&2
      exit 1
    fi
    gunzip -f "$work/$asset.gz"
    chmod +x "$work/$asset"
  fi
  javy="$work/$asset"
fi
"$javy" --version

export RUSTUP_HOME="$work/rustup" CARGO_HOME="$work/cargo"
export PATH="$CARGO_HOME/bin:$PATH"
if [[ ! -x "$CARGO_HOME/bin/cargo" ]]; then
  curl -sSf https://sh.rustup.rs -o "$work/rustup-init.sh"
  sh "$work/rustup-init.sh" -y --no-modify-path --profile minimal \
    --default-toolchain stable --target wasm32-wasip1 >/dev/null
fi

if [[ ! -d "$work/javy" ]]; then
  git clone -q --depth 1 --branch "$JAVY_VERSION" https://github.com/bytecodealliance/javy.git \
    "$work/javy"
fi
cd "$work/javy"
# Drop +simd128 (it is the only rustflag for the wasm32 targets).
sed -i.bak 's/rustflags = \["-C", "target-feature=+simd128"\]/rustflags = []/' .cargo/config.toml
if grep -q simd128 .cargo/config.toml; then
  echo "could not remove +simd128 from $work/javy/.cargo/config.toml" >&2
  exit 1
fi

cargo build -p javy-plugin --target wasm32-wasip1 --release
"$javy" init-plugin target/wasm32-wasip1/release/plugin.wasm -o "$output"
echo "$(sha256 "$output")  $(basename "$output")" > "$output.sha256"
ls -l "$output"
cat "$output.sha256"
