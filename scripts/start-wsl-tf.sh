#!/usr/bin/env bash
set -eu

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MAVEN_VERSION="3.9.10"
PORTABLE_MVN="$ROOT/.tools/apache-maven-${MAVEN_VERSION}/bin/mvn"
WORKSHOP_PORT="${WORKSHOP_PORT:-3915}"

if ! grep -qiE 'microsoft|wsl' /proc/version 2>/dev/null; then
  echo "ERROR: Run inside WSL, or use npm run start:wsl from Windows."
  exit 1
fi

resolve_mvn() {
  if command -v mvn >/dev/null 2>&1; then echo mvn; return; fi
  if [[ -x "$PORTABLE_MVN" ]]; then echo "$PORTABLE_MVN"; return; fi
  echo ""
}

MVN="$(resolve_mvn)"
if [[ -z "$MVN" ]]; then
  echo "ERROR: Maven missing. Run: npm run setup:wsl"
  exit 1
fi

set -a
if [[ -f .env ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line//$'\r'/}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    export "$line"
  done < .env
fi
set +a

export TF_BACKEND="${TF_BACKEND:-gpu}"
TF_PROFILE=tf-cpu
if [[ "$TF_BACKEND" == "gpu" || "$TF_BACKEND" == "cuda" ]]; then
  TF_PROFILE=tf-gpu
fi

check_health() {
  local base="$1"
  base="${base%/}"
  curl -4 -sf --connect-timeout 2 --max-time 4 "${base}/api/health" >/dev/null
}

MAIN_RESOLVED=""
env_base="${MAIN_SERVER_URL:-}"
env_base="${env_base%/}"
if [[ -n "$env_base" ]] && check_health "$env_base"; then
  MAIN_RESOLVED="$env_base"
fi

if [[ -z "$MAIN_RESOLVED" ]]; then
  for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if candidate="$(bash scripts/wsl-resolve-main-url.sh)" && check_health "$candidate"; then
      MAIN_RESOLVED="$candidate"
      break
    fi
    [[ "$attempt" -lt 10 ]] && sleep 2
  done
fi

if [[ -z "$MAIN_RESOLVED" ]]; then
  echo "ERROR: Workshop site not reachable from WSL."
  exit 1
fi

export MAIN_SERVER_URL="$MAIN_RESOLVED"

if [[ "${WIN_PROXY:-}" == "1" ]]; then
  PORT=3917
  echo "WSL team server on port ${PORT} (Windows proxy publishes :3916)"
else
  PORT="${PORT:-3916}"
fi
export PORT

WATCH="${JAVA_WATCH:-${NODEMON_RELOAD:-1}}"
SRC="$ROOT/src/main/java"

echo "TF_BACKEND=${TF_BACKEND} (Maven -P${TF_PROFILE}) | MAIN_SERVER_URL=${MAIN_SERVER_URL}"
echo "Forecast server on http://0.0.0.0:${PORT}"

run_server() {
  "$MVN" -q -P"$TF_PROFILE" exec:java
}

if [[ "$WATCH" != "1" && "$WATCH" != "true" ]]; then
  exec "$MVN" -q -P"$TF_PROFILE" exec:java
fi

if ! command -v inotifywait >/dev/null 2>&1; then
  echo "inotify-tools missing — running without reload (sudo apt install inotify-tools)."
  exec "$MVN" -q -P"$TF_PROFILE" exec:java
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
  echo "Compiling (-P${TF_PROFILE})..."
  if ! "$MVN" -q -P"$TF_PROFILE" compile; then
    echo "Compile failed — fix and save again."
    return 1
  fi
  "$MVN" -q -P"$TF_PROFILE" exec:java &
  server_pid=$!
}

trap 'stop_server; exit 0' INT TERM
start_server_bg || true
while true; do
  inotifywait -r -e modify,create,delete,move "$SRC" >/dev/null 2>&1 || continue
  sleep 0.8
  echo "Changes detected — restarting..."
  start_server_bg || true
done
