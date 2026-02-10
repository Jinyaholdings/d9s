#!/usr/bin/env bash
set -euo pipefail

IMAGE="eclipse-temurin:8-jre"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

LOGINURL="${SFDC_LOGINURL:-https://login.salesforce.com}"
API_VERSION="60.0"
CLS_NAMESPACE="ps"

if [[ "${LOGINURL}" == *"/services/Soap/u/"* ]]; then
  BASE_URL="${LOGINURL%%/services/Soap/u/*}"
else
  BASE_URL="$(echo "${LOGINURL}" | sed -E 's#(https?://[^/]+).*#\1#')"
fi
CLS_SERVICE_URL="${BASE_URL}/services/Soap/class"

run_case() {
  local label="$1"
  local java_opts="$2"
  echo ""
  echo "=== Case: ${label} ==="
  podman run --rm \
    -e SFDC_LOGINURL="${LOGINURL}" \
    -e SFDC_API_VERSION="${API_VERSION}" \
    -e SFDC_CLS_SERVICE_URL="${CLS_SERVICE_URL}" \
    -e SFDC_CLS_NAMESPACE="${CLS_NAMESPACE}" \
    -e SFDC_OUTPUT_FILE="salesforce_connectivity_results.log" \
    -e SFDC_CASE_LABEL="${label}" \
    -v "${SCRIPT_DIR}:/work:Z" \
    -w /work \
    "${IMAGE}" \
    bash -lc "java ${java_opts} SalesforceConnectivity"
}

run_case "default" ""
run_case "prefer_ipv4" "-Djava.net.preferIPv4Stack=true"
run_case "tls_debug_ipv4" "-Djavax.net.debug=ssl,handshake -Djava.net.preferIPv4Stack=true"
