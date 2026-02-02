test_and_package:
	@echo "Running tests and packaging"
	OS_TYPE=${OS_TYPE:-$(uname)} docker compose run --rm maven /bin/sh -c "./install_graalvm.sh && mvn package -Pnative"

publish:
	@echo "Deploying jar"
	docker compose run --rm maven /bin/sh -c "mvn -B -e versions:set -DnewVersion=$(VERSION) && mvn deploy -DskipTests"

py_test_and_package:
	@echo "Running tests and packaging: pylarky"
	docker compose run --rm python /bin/sh -c "./build-and-test-python.sh"