test_and_package:
	@echo "Running tests and packaging"
	docker compose run --rm maven /bin/sh -c "./install_graalvm.sh && ./build-and-test-java.sh"

publish:
	@echo "Deploying jar"
	docker compose run --rm maven /bin/sh -c "mvn -B -e versions:set -DnewVersion=$(VERSION) && mvn deploy -DskipTests"

py_test_and_package:
	@echo "Running tests and packaging: pylarky"
	docker compose run --rm \
	    python /bin/sh -c "./build-and-test-python.sh"