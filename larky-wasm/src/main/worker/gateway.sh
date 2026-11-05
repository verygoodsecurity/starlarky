#!/bin/sh
set -eu

: "${WORKER_KEYSTORE:?WORKER_KEYSTORE must point to an absolute PKCS12 file}"
: "${WORKER_KEYSTORE_PASSWORD_FILE:?WORKER_KEYSTORE_PASSWORD_FILE is required}"
: "${WORKER_TRUSTSTORE:?WORKER_TRUSTSTORE must point to an absolute PKCS12 file}"
: "${WORKER_TRUSTSTORE_PASSWORD_FILE:?WORKER_TRUSTSTORE_PASSWORD_FILE is required}"

exec /opt/java/openjdk/bin/java --add-modules jdk.incubator.vector \
  -Xmx96m -XX:MaxMetaspaceSize=64m \
  -Dlarky.wasm.worker.server.port="${WORKER_PORT:-8443}" \
  -Dlarky.wasm.worker.server.plainPort="${WORKER_PLAIN_PORT:-8081}" \
  -Dlarky.wasm.worker.tls.keyStore="$WORKER_KEYSTORE" \
  -Dlarky.wasm.worker.tls.keyStorePasswordFile="$WORKER_KEYSTORE_PASSWORD_FILE" \
  -Dlarky.wasm.worker.tls.trustStore="$WORKER_TRUSTSTORE" \
  -Dlarky.wasm.worker.tls.trustStorePasswordFile="$WORKER_TRUSTSTORE_PASSWORD_FILE" \
  -cp '/app/worker.jar:/app/lib/*' \
  com.verygood.security.larky.wasm.WorkerTlsGateway
