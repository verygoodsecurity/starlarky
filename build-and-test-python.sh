pyenv local 3.10.5
poetry version ${VERSION}
poetry install
poetry run pytest pylarky/tests
poetry build