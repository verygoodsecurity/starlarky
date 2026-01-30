test_and_package:
	@echo "Running tests and packaging"
	docker compose run --rm maven /bin/sh -c "./install_graalvm.sh && mvn package -Pnative"

unit_and_package:
	@echo "Running unit tests and packaging"
	docker compose run --rm maven /bin/sh -c "mvn package -DskipTests -DSTDOUT_TO_JSON=false"

integration:
	@echo "Running integration tests"
	docker compose up -d --wait postgres
	docker compose run --rm \
		-e STDOUT_TO_JSON=false \
		-e SPRING_DATASOURCE_URL="jdbc:postgresql://postgres:5432/vault_test?user=vault&password=vault" \
		-e AWS_DEFAULT_REGION \
		-e AWS_REGION \
		-e AWS_ACCESS_KEY_ID \
		-e AWS_SECRET_ACCESS_KEY \
		-e AWS_SESSION_TOKEN \
		maven /bin/sh -c "mvn package"
	docker compose down postgres

publish:
	@echo "Deploying jar"
	docker compose run --rm maven /bin/sh -c "mvn -B -e versions:set -DnewVersion=$(VERSION) && mvn deploy -DskipTests"

checkstyle:
	@echo "Running checkstyle"
	docker compose run --rm maven /bin/sh -c "mvn checkstyle:check"
