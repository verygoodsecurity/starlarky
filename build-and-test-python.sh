pyenv local 3.8.6
poetry version ${VERSION}
poetry install
poetry run pytest pylarky/tests
poetry build