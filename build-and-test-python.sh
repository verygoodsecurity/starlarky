set -ex

pyenv local 3.10.5
PATH=${PATH}:~/.local/bin

# You must run ./build-and-test-java.sh to build this
cp ./runlarky/target/larky-runner ./pylarky

# Run tests
poetry version ${VERSION}
poetry install
poetry run pytest pylarky/tests
poetry build