#!/usr/bin/env bash
cd "$(dirname "$0")"

OS_TYPE=${OS_TYPE:-$(uname)}
OS_TYPE_LOWER=$(echo "$OS_TYPE" | tr '[:upper:]' '[:lower:]')
TEST_RESULTS_PATH=${TEST_RESULTS_PATH:-/tmp/test-results}

mkdir -p $TEST_RESULTS_PATH/junit/
mkdir -p $TEST_RESULTS_PATH/coverage/
mvn clean install dependency:go-offline -T 2.0C -B
find . -type f -regex ".*/target/surefire-reports/.*xml" -exec cp {} $TEST_RESULTS_PATH/junit/ \;
find . -type f -regex ".*/target/surefire-reports/.*-output.txt" -exec cp {} $TEST_RESULTS_PATH/junit/ \;
find . -type f -regex ".*/target/site/.*" -exec cp --parents {} $TEST_RESULTS_PATH/coverage/ \;

# package it up to deliver
mvn package -Pnative -DskipTests

# tag distribution
mv ./runlarky/target/larky-runner ./runlarky/target/larky-${OS_TYPE_LOWER}