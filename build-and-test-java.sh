#!/usr/bin/env bash
cd "$(dirname "$0")"

OS_TYPE=${OS_TYPE:-$(uname)}
OS_TYPE_LOWER=$(echo "$OS_TYPE" | tr '[:upper:]' '[:lower:]')

# test and package
mvn package -Pnative

# tag distribution
mv ./runlarky/target/larky-runner ./runlarky/target/larky-${OS_TYPE_LOWER}