#!/usr/bin/env bash
cd "$(dirname "$0")"

OS_TYPE=${OS_TYPE:-$(uname)}
OS_TYPE_LOWER=$(echo "$OS_TYPE" | tr '[:upper:]' '[:lower:]')

# test and package
mvn package -Pnative

# tag distribution
mv ./runlarky/target/larky-runner ./runlarky/target/larky-${OS_TYPE_LOWER}

# smoke-test the native runner: its classes reach Starlark only through reflect-config.json (#397)
SMOKE_DIR=$(mktemp -d)
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
./runlarky/target/larky-${OS_TYPE_LOWER} -s "$SMOKE_DIR/smoke.star" -o "$SMOKE_DIR/out" -l "$SMOKE_DIR/log" || exit 1
EXPECTED='["int", "type", "AB", "B", 42, "{\"a\":[1]}"]'
if [ "$(cat "$SMOKE_DIR/out")" != "$EXPECTED" ]; then
  echo "native smoke test: expected $EXPECTED, got $(cat "$SMOKE_DIR/out")"
  exit 1
fi
