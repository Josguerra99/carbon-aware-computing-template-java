#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

NO_WATCH=0
for arg in "$@"; do
  if [[ "$arg" == "--no-watch" ]]; then
    NO_WATCH=1
  fi
done

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
SRC="$ROOT/src/main/java"

if [[ "$NO_WATCH" -eq 1 ]]; then
  echo "Starting forecast server (Maven exec:java) on port ${PORT}..."
  exec "$MVN" -q exec:java
fi

echo "Starting forecast server (watch src/main/java) on port ${PORT}..."

if ! command -v inotifywait >/dev/null 2>&1; then
  echo "inotify-tools not installed — running once without reload."
  echo "Install: sudo apt install inotify-tools"
  exec "$MVN" -q exec:java
fi

server_pid=""
stop_server() {
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
    kill "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
  fi
  server_pid=""
}

start_server_bg() {
  stop_server
  echo "Compiling..."
  "$MVN" -q compile
  echo "Server running (Ctrl+C to stop). Edit Java under src/main/java to reload."
  "$MVN" -q exec:java &
  server_pid=$!
}

trap 'stop_server; exit 0' INT TERM

start_server_bg
while true; do
  inotifywait -r -e modify,create,delete,move "$SRC" >/dev/null 2>&1 || continue
  sleep 0.8
  echo "Changes detected — restarting..."
  start_server_bg
done
