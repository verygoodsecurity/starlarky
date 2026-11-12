#!/usr/bin/env bash

set -e

cd "$(dirname "$0")"
GRAALVM_DIR=${GRAALVM_DIR:=./.graalvm}
# native-maven-plugin 1.x needs a GraalVM that ships lib/svm/schemas/reachability-metadata-schema.json;
# GraalVM Community for Java 21 stops at 21.0.2, which does not.
GRAALVM_RELEASE=25.4.4.1.1
GRAALVM_JAVA_VERSION=25.0.4.1.1
GRAALVM_PACKAGE="graalvm-community-${GRAALVM_RELEASE}+1.1"
GRAALVM_DOWNLOADS="https://github.com/graalvm/graalvm-ce-builds/releases/download/graal-${GRAALVM_RELEASE}"

OS_TYPE=${OS_TYPE:-$(uname)}
case "$OS_TYPE" in
  MINGW*|MSYS*|CYGWIN*) OS_TYPE=Windows ;;  # Git Bash on Windows
esac
if [ "$OS_TYPE" == "Darwin" ]; then
    # For local development.
    echo "macOS detected."
    GRAALVM_URL="${GRAALVM_DOWNLOADS}/graalvm-community-jdk-25i4-${GRAALVM_JAVA_VERSION}_macos-aarch64_bin.tar.gz"
    GRAALVM_SHA256=4ec9932aa4aab1afca0ea31982d32d6f956615c61fe21a227b49756b25adda4f
    GRAALVM_HOME=${GRAALVM_DIR}/Contents/Home
    GRAALVM_BIN=${GRAALVM_HOME}/bin
elif [ "$OS_TYPE" == "Linux" ]; then
    # For CI/CD builds and MakeFile usage
    echo "Linux detected."
    GRAALVM_URL="${GRAALVM_DOWNLOADS}/graalvm-community-jdk-25i4-${GRAALVM_JAVA_VERSION}_linux-x64_bin.tar.gz"
    GRAALVM_SHA256=05ccbbe783210b6886ff7b08fcd0b061c5dce4852b05db87284fc0e24abb08e2
    GRAALVM_HOME=${GRAALVM_DIR}
    GRAALVM_BIN=${GRAALVM_HOME}/bin
elif [ "$OS_TYPE" == "Windows" ]; then
    # For the Windows release binary (native-binaries workflow).
    echo "Windows detected."
    GRAALVM_URL="${GRAALVM_DOWNLOADS}/graalvm-community-jdk-25i4-${GRAALVM_JAVA_VERSION}_windows-x64_bin.zip"
    GRAALVM_SHA256=2733ea1331f1a98b05dd55a768b07347051dad48f030dbbfa191b11a2ccf4144
    GRAALVM_HOME=${GRAALVM_DIR}
    GRAALVM_BIN=${GRAALVM_HOME}/bin
else
    echo "Unsupported OS: $OS_TYPE"
    exit 1
fi

# A GraalVM for another Java version (e.g. restored from an older cache) is replaced.
if [ -d "$GRAALVM_DIR" ] && ! grep -qs "^JAVA_VERSION=\"$GRAALVM_JAVA_VERSION\"" "$GRAALVM_HOME/release"; then
  echo "GraalVM in '$GRAALVM_DIR' is not for Java $GRAALVM_JAVA_VERSION. Reinstalling..."
  rm -rf "$GRAALVM_DIR"
fi

if [ ! -d "$GRAALVM_DIR" ]; then
  echo "Installing GraalVM $GRAALVM_RELEASE (Java $GRAALVM_JAVA_VERSION) in '$(pwd)'..."
  GRAALVM_ARCHIVE="graalvm.${GRAALVM_URL##*_bin.}"  # graalvm.tar.gz, or graalvm.zip on Windows
  curl -f -o "$GRAALVM_ARCHIVE" -L "$GRAALVM_URL"
  if command -v sha256sum >/dev/null; then
    echo "$GRAALVM_SHA256  $GRAALVM_ARCHIVE" | sha256sum -c -
  else
    echo "$GRAALVM_SHA256  $GRAALVM_ARCHIVE" | shasum -a 256 -c -
  fi
  case "$GRAALVM_ARCHIVE" in
    *.zip) unzip -q "$GRAALVM_ARCHIVE" ;;
    *) tar xfz "$GRAALVM_ARCHIVE" ;;
  esac
  mv "$GRAALVM_PACKAGE" .graalvm
  rm "$GRAALVM_ARCHIVE"
fi
