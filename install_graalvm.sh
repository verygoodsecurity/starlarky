#!/usr/bin/env bash

set -e

cd "$(dirname "$0")"
GRAALVM_DIR=${GRAALVM_DIR:=./.graalvm}
GRAALVM_JAVA_VERSION=21.0.2

OS_TYPE=${OS_TYPE:-$(uname)}
if [ "$OS_TYPE" == "Darwin" ]; then
    # For local development.
    echo "macOS detected."
    GRAALVM_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_macos-aarch64_bin.tar.gz"
    GRAALVM_PACKAGE="graalvm-community-openjdk-21.0.2+13.1"
    GRAALVM_HOME=${GRAALVM_DIR}/Contents/Home
    GRAALVM_BIN=${GRAALVM_HOME}/bin
elif [ "$OS_TYPE" == "Linux" ]; then
    # For CI/CD builds and MakeFile usage
    echo "Linux detected."
    GRAALVM_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz"
    GRAALVM_PACKAGE="graalvm-community-openjdk-21.0.2+13.1"
    GRAALVM_HOME=${GRAALVM_DIR}
    GRAALVM_BIN=${GRAALVM_HOME}/bin
else
    echo "Unsupported OS: $OS_TYPE"
    exit 1
fi

# A GraalVM for another Java version (e.g. restored from an older cache) is replaced: native-image
# cannot load classes compiled for a newer Java.
if [ -d "$GRAALVM_DIR" ] && ! grep -qs "^JAVA_VERSION=\"$GRAALVM_JAVA_VERSION\"" "$GRAALVM_HOME/release"; then
  echo "GraalVM in '$GRAALVM_DIR' is not for Java $GRAALVM_JAVA_VERSION. Reinstalling..."
  rm -rf "$GRAALVM_DIR"
fi

if [ ! -d "$GRAALVM_DIR" ]; then
  echo "Installing GraalVM for Java $GRAALVM_JAVA_VERSION in '$(pwd)'..."
  curl -o graalvm.tar.gz -J -L "$GRAALVM_URL"
  tar xfz graalvm.tar.gz
  mv $GRAALVM_PACKAGE .graalvm
  rm graalvm.tar.gz
  # GraalVM for JDK 21+ bundles native-image and no longer ships gu.
  if [ -x "$GRAALVM_BIN/gu" ]; then
    $GRAALVM_BIN/gu install native-image
  fi
fi