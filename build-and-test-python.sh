set -ex

VERSION=${VERSION:-0.0.1}

# You must run ./build-and-test-java.sh to build this
cp ./runlarky/target/larky-runner ./pylarky

# Run tests
poetry version ${VERSION}
poetry install > poetry_install.log
poetry run pytest pylarky/tests
poetry build