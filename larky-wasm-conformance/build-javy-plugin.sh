#!/usr/bin/env bash
# Builds Javy's QuickJS plugin without WebAssembly SIMD, for `javy build -C plugin=...`.
#
# Javy's workspace builds every wasm32-wasip1 crate with `-C target-feature=+simd128` (for
# simd-json; see its .cargo/config.toml), so its stock plugin, and every static module built from
# it, contains SIMD instructions. Endive runs those only on Java 25+ (and mis-runs simd-json there);
# a module built from this plugin has none, so both runtimes run it on any supported JDK.
#
# Usage: build-javy-plugin.sh JAVY OUTPUT [WORKDIR]
#   JAVY     the javy CLI (v9.1.0), used to initialize the plugin
#   OUTPUT   where to write the initialized plugin
#   WORKDIR  where to install Rust and clone Javy (default: a temporary directory)
# Installs a Rust toolchain into WORKDIR (it does not touch ~/.cargo or ~/.rustup).
set -euo pipefail

JAVY_VERSION=v9.1.0
javy="$1"
output="$2"
work="${3:-$(mktemp -d)}"
mkdir -p "$work"
work="$(cd "$work" && pwd)"
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
ls -l "$output"
