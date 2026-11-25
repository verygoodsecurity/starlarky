#!/usr/bin/env bash
# Atlas Horizon's script execution with both Larky caches warm, build A against build B.
#
#   ATLAS_CP=<Atlas service classes and dependencies, ':'-separated> \
#   ATLAS_OVERLAY=<optional dir of Atlas classes to put first, e.g. stable helper names> \
#   bench/examples/atlas-two-caches.sh A-CLASSPATH B-CLASSPATH [extra larky-bench options]
#
# A-CLASSPATH and B-CLASSPATH are each a build's Larky and libstarlark jars. If the Atlas build
# generates helper names per call, use an overlay with stable names so the program cache can hit.
set -euo pipefail
here=$(cd "$(dirname "$0")/.." && pwd)
a=${1:?A classpath}; b=${2:?B classpath}; shift 2
extra=${ATLAS_CP:?set ATLAS_CP}
if [[ -n "${ATLAS_OVERLAY:-}" ]]; then extra="$ATLAS_OVERLAY:$extra"; fi
for scenario in ${SCENARIOS:-response short-response ordinary terminal}; do
  java -jar "$here/target/larky-bench.jar" -a "$a" -b "$b" \
    -w AtlasRequestProfile --workload-src "$here/workloads/atlas/AtlasRequestProfile.java" \
    -s "$scenario" --extra-cp "$extra" \
    --jvm-arg -Xms512m --jvm-arg -Xmx1024m --jvm-arg -Dlarky.moduleCache.enable=true \
    --jvm-arg -Dfile.encoding=UTF-8 \
    --jvm-arg -Dknox.host=127.0.0.1 --jvm-arg -Dknox.port=1 --jvm-arg -Dknox.tlsEnabled=false \
    --jvm-arg -Dknox.keycloakServerUrl=http://127.0.0.1:1/auth \
    --jvm-arg -Dknox.keycloak.client_id=fixture --jvm-arg -Dknox.keycloak.client_secret=fixture \
    --jvm-arg -Dknox.keycloak.realm=vault \
    --jvm-arg -Dcalm.host=127.0.0.1 --jvm-arg -Dcalm.port=1 --jvm-arg -Dcalm.tlsEnabled=false \
    --jvm-arg -Dcalm.keycloak.auth.enabled=false --jvm-arg -Dauth.provider=none \
    ${ATLAS_LOGBACK:+--jvm-arg -Dlogback.configurationFile=$ATLAS_LOGBACK} \
    -o "${OUT:-bench-results}/atlas-$scenario" "$@"
done
