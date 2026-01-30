#!/usr/bin/env bash

set -e

cd "$(dirname "$0")"
GRAALVM_HOME=${GRAALVM_HOME:=./.graalvm}

if [ ! -d "$GRAALVM_HOME" ]; then
  echo "GraalVM exists in '$(pwd)'. Updating..."
  curl https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-20.0.2/graalvm-community-jdk-20.0.2_linux-x64_bin.tar.gz -O -J -L
  tar xfz graalvm-community-jdk-20.0.2_linux-x64_bin.tar.gz
  mv graalvm-community-openjdk-20.0.2+9.1 .graalvm
  rm graalvm-community-jdk-20.0.2_linux-x64_bin.tar.gz
  $GRAALVM_HOME/bin/gu install native-image
fi