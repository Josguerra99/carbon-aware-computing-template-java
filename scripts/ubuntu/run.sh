#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

bash "$(dirname "$0")/../ensure-env.sh"

MAVEN_VERSION="3.9.10"
PORTABLE_MVN="$ROOT/.tools/apache-maven-${MAVEN_VERSION}/bin/mvn"

resolve_mvn() {
  if command -v mvn >/dev/null 2>&1; then
    echo "mvn"
    return
  fi
  if [[ -x "$PORTABLE_MVN" ]]; then
    echo "$PORTABLE_MVN"
    return
  fi
  echo ""
}

MVN="$(resolve_mvn)"
if [[ -z "$MVN" ]]; then
  echo "Maven not ready — running setup..."
  bash "$(dirname "$0")/setup.sh"
  MVN="$(resolve_mvn)"
fi

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

PORT="${PORT:-3916}"
echo "Starting forecast server (Maven exec:java) on port ${PORT}..."
exec "$MVN" -q exec:java
