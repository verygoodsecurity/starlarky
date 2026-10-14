#!/bin/sh
set -eu

exec /opt/java/openjdk/bin/java --add-modules jdk.incubator.vector \
  -Xmx96m -XX:MaxMetaspaceSize=64m \
  -Dlarky.wasm.worker.launcher=/app/child.sh \
  -Dlarky.wasm.worker.server.plainPort="${WORKER_PLAIN_PORT:-8081}" \
  -cp '/app/worker.jar:/app/lib/*' \
  com.verygood.security.larky.wasm.WorkerPlainServer
