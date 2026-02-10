#!/usr/bin/env bash
set -euo pipefail

IMAGE="eclipse-temurin:8-jdk"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

podman run --rm \
  -v "${ROOT_DIR}:/work:Z" \
  -w /work/sf_diag \
  "${IMAGE}" \
  bash -lc "javac SalesforceConnectivity.java"
