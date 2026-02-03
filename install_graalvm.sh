#!/usr/bin/env bash

set -e

cd "$(dirname "$0")"
GRAALVM_DIR=${GRAALVM_DIR:=./.graalvm}

OS_TYPE=${OS_TYPE:-$(uname)}
if [ "$OS_TYPE" == "Darwin" ]; then
    # For local development.
    echo "macOS detected."
    GRAALVM_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-20.0.2/graalvm-community-jdk-20.0.2_macos-aarch64_bin.tar.gz"
    GRAALVM_PACKAGE="graalvm-community-openjdk-20.0.2+9.1"
    GRAALVM_BIN=${GRAALVM_DIR}/Contents/Home/bin
elif [ "$OS_TYPE" == "Linux" ]; then
    # For CI/CD builds and MakeFile usage
    echo "Linux detected."
    GRAALVM_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-20.0.2/graalvm-community-jdk-20.0.2_linux-x64_bin.tar.gz"
    GRAALVM_PACKAGE="graalvm-community-openjdk-20.0.2+9.1"
    GRAALVM_BIN=${GRAALVM_DIR}/bin
else
    echo "Unsupported OS: $OS_TYPE"
    exit 1
fi

if [ ! -d "$GRAALVM_DIR" ]; then
  echo "GraalVM exists in '$(pwd)'. Updating..."
  curl -o graalvm.tar.gz -J -L "$GRAALVM_URL"
  tar xfz graalvm.tar.gz
  mv $GRAALVM_PACKAGE .graalvm
  rm graalvm.tar.gz
  $GRAALVM_BIN/gu install native-image
fi