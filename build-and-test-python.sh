#!/usr/bin/env bash
set -ex

cd "$(dirname "$0")"

OS_TYPE=${OS_TYPE:-$(uname)}
OS_TYPE_LOWER=$(echo "$OS_TYPE" | tr '[:upper:]' '[:lower:]')
VERSION=${VERSION:-0.0.1}

# You must run ./build-and-test-java.sh to build this
cp ./runlarky/target/larky-${OS_TYPE_LOWER} ./pylarky/larky-runner
chmod +x ./pylarky/larky-${OS_TYPE_LOWER}

# Run tests
poetry version ${VERSION}
poetry install > poetry_install.log
poetry run pytest pylarky/tests
poetry build