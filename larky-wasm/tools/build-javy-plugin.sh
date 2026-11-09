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
#   WORKDIR  where to install Rust, clone Javy and download the CLI (default: a temporary directory,
#            removed afterwards)
# Set JAVY to a javy v9.1.0 CLI to use it instead of downloading one. Installs Rust into WORKDIR;
# it does not touch ~/.cargo or ~/.rustup.
#
# What it downloads is pinned here: the Javy CLI and rustup-init by SHA-256, Javy's source by the
# commit its release tag named when this was written, and the Rust toolchain by version (Javy's own
# rust-toolchain.toml says only "stable"). A mismatch stops the build.
set -euo pipefail

JAVY_VERSION=v9.1.0
JAVY_COMMIT=d9b6139ddaefe2be95cfd758c2a8e62fdd8de861
RUSTUP_VERSION=1.28.2
RUST_VERSION=1.99.0
output="$1"
if [[ -n "${2:-}" ]]; then
  work="$2"
else
  work="$(mktemp -d)"
  trap 'rm -rf "$work"' EXIT
fi
mkdir -p "$work" "$(dirname "$output")"
work="$(cd "$work" && pwd)"
output="$(cd "$(dirname "$output")" && pwd)/$(basename "$output")"

sha256() {
  if command -v sha256sum >/dev/null; then sha256sum "$1"; else shasum -a 256 "$1"; fi | cut -d' ' -f1
}

javy="${JAVY:-}"
if [[ "$javy" == */* ]]; then
  # A path, not a command on PATH: make it absolute, since the build runs from Javy's checkout.
  javy="$(cd "$(dirname "$javy")" && pwd)/$(basename "$javy")"
fi
if [[ -z "$javy" ]]; then
  case "$(uname -s)-$(uname -m)" in
    Darwin-arm64)
      asset="javy-arm-macos-$JAVY_VERSION"
      expected=99e9ec6a8e8c98e119d137c08a921d2443d3b873c675a5571e1800f4451e6294 ;;
    Darwin-x86_64)
      asset="javy-x86_64-macos-$JAVY_VERSION"
      expected=6eed2927575dc2b3fb5a1563eee1ce0874e6da91c9cec39a728c9377a8cc7b5a ;;
    Linux-x86_64)
      asset="javy-x86_64-linux-$JAVY_VERSION"
      expected=a68b122d48eb3dfc1b801d4e14c39271fde3638243d3272d206e376ac9189e39 ;;
    Linux-aarch64 | Linux-arm64)
      asset="javy-arm-linux-$JAVY_VERSION"
      expected=826962f0e82354cf97d6e928ea36777872bbae76c626e326948e0367c4b55cb5 ;;
    *) echo "no Javy CLI for $(uname -s)-$(uname -m); set JAVY" >&2; exit 1 ;;
  esac
  if [[ ! -x "$work/$asset" ]]; then
    curl -sSfL -o "$work/$asset.gz" \
      "https://github.com/bytecodealliance/javy/releases/download/$JAVY_VERSION/$asset.gz"
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
  case "$(uname -s)-$(uname -m)" in
    Darwin-arm64)
      target=aarch64-apple-darwin
      expected=20ef5516c31b1ac2290084199ba77dbbcaa1406c45c1d978ca68558ef5964ef5 ;;
    Darwin-x86_64)
      target=x86_64-apple-darwin
      expected=9c331076f62b4d0edeae63d9d1c9442d5fe39b37b05025ec8d41c5ed35486496 ;;
    Linux-x86_64)
      target=x86_64-unknown-linux-gnu
      expected=20a06e644b0d9bd2fbdbfd52d42540bdde820ea7df86e92e533c073da0cdd43c ;;
    Linux-aarch64 | Linux-arm64)
      target=aarch64-unknown-linux-gnu
      expected=e3853c5a252fca15252d07cb23a1bdd9377a8c6f3efa01531109281ae47f841c ;;
    *) echo "no rustup-init for $(uname -s)-$(uname -m)" >&2; exit 1 ;;
  esac
  curl -sSfL -o "$work/rustup-init" \
    "https://static.rust-lang.org/rustup/archive/$RUSTUP_VERSION/$target/rustup-init"
  actual="$(sha256 "$work/rustup-init")"
  if [[ "$expected" != "$actual" ]]; then
    echo "SHA-256 mismatch for rustup-init: expected $expected, got $actual" >&2
    exit 1
  fi
  chmod +x "$work/rustup-init"
  "$work/rustup-init" -y --no-modify-path --profile minimal \
    --default-toolchain "$RUST_VERSION" --target wasm32-wasip1 >/dev/null
fi
# Overrides Javy's rust-toolchain.toml ("stable"), so the plugin does not change with Rust releases.
export RUSTUP_TOOLCHAIN="$RUST_VERSION"
rustup toolchain install "$RUST_VERSION" --profile minimal --target wasm32-wasip1 >/dev/null
rustc --version

if [[ ! -d "$work/javy" ]]; then
  git clone -q --depth 1 --branch "$JAVY_VERSION" https://github.com/bytecodealliance/javy.git \
    "$work/javy"
fi
cd "$work/javy"
if [[ "$(git rev-parse HEAD)" != "$JAVY_COMMIT" ]]; then
  echo "$JAVY_VERSION is $(git rev-parse HEAD), not the pinned $JAVY_COMMIT" >&2
  exit 1
fi
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
