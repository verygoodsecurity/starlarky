#!/usr/bin/env bash
# Smoke-tests a native runlarky binary: its classes reach Starlark only through reflect-config.json (#397).
# Usage: smoke-test-native.sh <path to the binary>
set -euo pipefail
BINARY=${1:?usage: smoke-test-native.sh <binary>}

SMOKE_DIR=$(mktemp -d)
trap 'rm -rf "${SMOKE_DIR:?}"' EXIT
cat > "$SMOKE_DIR/smoke.star" <<'STAR'
load("@stdlib//json", "json")

def f(self):
    return "A"

def cm(cls):
    return cls.__name__

A = type("A", (object,), {"f": f, "cm": classmethod(cm), "sm": staticmethod(lambda x: x * 2)})

def g(self):
    return super(B, self).f() + "B"

B = type("B", (A,), {"f": g})

[type(1), str(type(object())), B().f(), B.cm(), B.sm(21), json.dumps({"a": [1]})]
STAR
"$BINARY" -s "$SMOKE_DIR/smoke.star" -o "$SMOKE_DIR/out" -l "$SMOKE_DIR/log" || exit 1
EXPECTED='["int", "type", "AB", "B", 42, "{\"a\":[1]}"]'
ACTUAL=$(tr -d '\r' < "$SMOKE_DIR/out")
if [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "native smoke test: expected $EXPECTED, got $ACTUAL"
  exit 1
fi
echo "native smoke test passed: $BINARY"
