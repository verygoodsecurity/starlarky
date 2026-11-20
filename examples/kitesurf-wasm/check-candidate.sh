#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 3 || ! "$2" =~ ^[0-9a-f]{64}$ || ! "$3" =~ ^[0-9]+$ ]]; then
  printf 'Usage: %s candidate.wasm trusted-sha256 fixture-port\n' "$0" >&2
  exit 2
fi
candidate="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
sha256="$2"
fixture_port="$3"
cd "$(dirname "$0")/../.."

python3 - "$candidate" "$sha256" <<'PY'
import hashlib
import sys
from pathlib import Path

with Path(sys.argv[1]).open("rb") as artifact:
    if hashlib.file_digest(artifact, "sha256").hexdigest() != sys.argv[2]:
        sys.exit("Kitesurf candidate digest does not match")
PY

# Remove old reports for these classes so they cannot satisfy this run's gate.
for test in KitesurfWasmModuleTest KitesurfWasmJavaScriptTest KitesurfWasmAdyenTest; do
  rm -f "larky/target/surefire-reports/TEST-com.verygood.security.larky.modules.$test.xml"
done
rm -f larky-wasm/target/surefire-reports/TEST-com.verygood.security.larky.wasm.BrowserWasmRuntimeTest.xml

mvn -B -ntp -pl larky -am \
  -Dtest=KitesurfWasmModuleTest,KitesurfWasmJavaScriptTest,KitesurfWasmAdyenTest,BrowserWasmRuntimeTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  "-Dlarky.kitesurf.wasmGuest=$candidate" "-Dlarky.kitesurf.wasmGuestSha256=$sha256" \
  "-Dlarky.kitesurf.adyenFixturePort=$fixture_port" test

python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree

classes = {
    "larky": [
        "com.verygood.security.larky.modules.KitesurfWasmModuleTest",
        "com.verygood.security.larky.modules.KitesurfWasmJavaScriptTest",
        "com.verygood.security.larky.modules.KitesurfWasmAdyenTest",
    ],
    "larky-wasm": ["com.verygood.security.larky.wasm.BrowserWasmRuntimeTest"],
}
for module, names in classes.items():
    for name in names:
        report = Path(module, "target", "surefire-reports", f"TEST-{name}.xml")
        if not report.is_file():
            raise SystemExit(f"Missing candidate test report for {name}")
        suite = ElementTree.parse(report).getroot().attrib
        if int(suite["tests"]) < 1 or any(int(suite[key]) for key in ("failures", "errors", "skipped")):
            raise SystemExit(f"Candidate tests must run and pass without skips for {name}")
PY
