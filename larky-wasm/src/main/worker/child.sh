#!/bin/sh
set -eu

exec /opt/java/openjdk/bin/java --add-modules jdk.incubator.vector \
  -Xmx256m -XX:MaxMetaspaceSize=64m \
  -cp '/app/worker.jar:/app/lib/*' \
  com.verygood.security.larky.wasm.WorkerMain
