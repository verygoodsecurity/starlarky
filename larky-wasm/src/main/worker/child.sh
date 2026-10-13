#!/bin/sh
set -eu

# The supervisor clears the child environment, including PATH. This path is part of the pinned
# eclipse-temurin:21-jre image contract checked by Dockerfile.wasm-worker.
exec /opt/java/openjdk/bin/java --add-modules jdk.incubator.vector \
  -Xmx256m -XX:MaxMetaspaceSize=64m \
  -cp '/app/worker.jar:/app/lib/*' \
  com.verygood.security.larky.wasm.WorkerMain
