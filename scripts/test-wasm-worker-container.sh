#!/bin/sh
set -eu

# Disposable local acceptance run. Never reuse names, certificates, or volumes in production.
job_id="wasm-test-$(date +%s)-$$"
network="$job_id-net"
secrets="$job_id-secrets"
pause="$job_id-netns"
worker="$job_id-worker"
gateway="$job_id-gateway"
client="$job_id-client"

cleanup() {
  docker rm -f "$client" "$gateway" "$worker" "$pause" >/dev/null 2>&1 || true
  docker network rm "$network" >/dev/null 2>&1 || true
  docker volume rm "$secrets" >/dev/null 2>&1 || true
}
trap cleanup EXIT HUP INT TERM

docker network create --internal "$network" >/dev/null
docker volume create "$secrets" >/dev/null
docker run --rm -v "$secrets:/secrets" -e WORKER_DNS_NAME="$pause" \
  --entrypoint sh maven:3.9.9-eclipse-temurin-21 -ec '
    keytool -genkeypair -alias worker -keyalg RSA -keysize 2048 \
      -dname "CN=$WORKER_DNS_NAME" -ext "SAN=dns:$WORKER_DNS_NAME" -validity 1 \
      -storetype PKCS12 -keystore /secrets/identity.p12 \
      -storepass local-test-password -keypass local-test-password -noprompt >/dev/null 2>&1
    keytool -exportcert -alias worker -keystore /secrets/identity.p12 \
      -storepass local-test-password -file /secrets/cert.der >/dev/null 2>&1
    keytool -importcert -alias worker -file /secrets/cert.der \
      -keystore /secrets/trust.p12 -storetype PKCS12 \
      -storepass local-test-password -noprompt >/dev/null 2>&1
    printf "%s\n" local-test-password > /secrets/key-password
    printf "%s\n" local-test-password > /secrets/trust-password
    chown -R 65532:65532 /secrets
    chmod 0700 /secrets
    chmod 0600 /secrets/*
  '

# The pause container owns a Pod-like network namespace; restarting the worker cannot kill it.
docker run -d --name "$pause" --network "$network" --entrypoint sleep \
  eclipse-temurin:21-jre 3600 >/dev/null
docker run -d --name "$worker" --network "container:$pause" \
  --memory 768m --memory-swap 768m --cpus 1 --pids-limit 64 --shm-size 1g \
  --read-only --cap-drop ALL --security-opt no-new-privileges \
  --tmpfs /tmp:rw,nosuid,nodev,size=32m,mode=1777 \
  --entrypoint /app/plain-server.sh starlarky-wasm-worker:split >/dev/null
docker run -d --name "$gateway" --network "container:$pause" \
  --memory 256m --memory-swap 256m --cpus 0.5 --pids-limit 32 \
  --read-only --cap-drop ALL --security-opt no-new-privileges \
  --tmpfs /tmp:rw,nosuid,nodev,size=16m,mode=1777 \
  -v "$secrets:/var/run/wasm-tls:ro" \
  -e WORKER_KEYSTORE=/var/run/wasm-tls/identity.p12 \
  -e WORKER_KEYSTORE_PASSWORD_FILE=/var/run/wasm-tls/key-password \
  -e WORKER_TRUSTSTORE=/var/run/wasm-tls/trust.p12 \
  -e WORKER_TRUSTSTORE_PASSWORD_FILE=/var/run/wasm-tls/trust-password \
  --entrypoint /app/gateway.sh starlarky-wasm-worker:split >/dev/null

if docker inspect --format '{{range .Mounts}}{{println .Destination}}{{end}}' "$worker" \
    | rg -q '/var/run/wasm-tls'; then
  echo 'Worker container unexpectedly has the TLS Secret' >&2
  exit 1
fi
status="$(docker exec "$worker" cat /proc/1/status)"
printf '%s\n' "$status" | rg -q '^NoNewPrivs:[[:space:]]+1$'
printf '%s\n' "$status" | rg -q '^CapEff:[[:space:]]+0000000000000000$'
printf '%s\n' "$status" | rg -q '^Seccomp:[[:space:]]+2$'
echo 'OS policy: separate memory/CPU/PID budgets, no_new_privs, no capabilities, seccomp'

probe() {
  if [ -f _gitless/wasm-adversarial/worker-spike/parser.wasm ] \
      && [ -f _gitless/wasm-adversarial/worker-spike/table.wasm ]; then
    docker run --rm --network "$network" --user 65532:65532 \
      -v "$secrets:/secrets:ro" \
      -v "$PWD/_gitless/wasm-adversarial/worker-spike:/private-wasm:ro" \
      -e WASM_WORKER_HOST="$pause" -e WASM_PRIVATE_FIXTURES=/private-wasm \
      starlarky-wasm-worker-probe:local "$1"
  else
    docker run --rm --network "$network" --user 65532:65532 \
      -v "$secrets:/secrets:ro" -e WASM_WORKER_HOST="$pause" \
      starlarky-wasm-worker-probe:local "$1"
  fi
}

wait_for_worker() {
  attempt=0
  until probe healthy >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 10 ]; then
      docker logs "$gateway" >&2
      docker logs "$worker" >&2
      echo 'worker gateway did not become ready' >&2
      exit 1
    fi
    sleep 1
  done
}
wait_for_worker
probe smoke

docker run -d --name "$client" --network "$network" --user 65532:65532 \
  --tmpfs /control:rw,nosuid,nodev,size=1m,mode=1777 \
  -v "$secrets:/secrets:ro" -e WASM_WORKER_HOST="$pause" \
  starlarky-wasm-worker-probe:local supervised >/dev/null

wait_for_client() {
  marker="$1"
  attempt=0
  until docker exec "$client" test -f "/control/$marker"; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 20 ]; then
      docker logs "$client" >&2
      echo "client did not reach $marker" >&2
      exit 1
    fi
    sleep 1
  done
}
wait_for_client ready

# Native-memory pressure must stay in the worker container's cgroup.
if docker exec "$worker" sh -c \
    'dd if=/dev/zero of=/dev/shm/fill bs=1M count=850 status=none'; then
  echo 'Expected worker cgroup OOM did not occur' >&2
  exit 1
fi
sleep 1
if [ "$(docker inspect --format '{{.State.Running}}' "$worker")" = true ] \
    && events="$(docker exec "$worker" cat /sys/fs/cgroup/memory.events 2>/dev/null)"; then
  printf '%s\n' "$events" | rg -q '^oom_kill [1-9][0-9]*$'
  docker exec "$worker" rm -f /dev/shm/fill || true
else
  test "$(docker inspect --format '{{.State.OOMKilled}}' "$worker")" = true
  docker start "$worker" >/dev/null
fi
test "$(docker inspect --format '{{.State.Running}}' "$gateway")" = true
test "$(docker inspect --format '{{.State.Running}}' "$client")" = true
echo 'worker cgroup OOM: gateway survived'

wait_for_worker
docker exec "$client" touch /control/resume
wait_for_client recovered

docker kill "$worker" >/dev/null
test "$(docker inspect --format '{{.State.Running}}' "$gateway")" = true
test "$(docker inspect --format '{{.State.Running}}' "$client")" = true
docker start "$worker" >/dev/null
wait_for_worker
docker exec "$client" touch /control/resume2
test "$(docker wait "$client")" = 0
docker logs "$client"
echo 'worker crash/restart: gateway and original client JVM survived'
