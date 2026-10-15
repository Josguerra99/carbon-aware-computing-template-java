#!/usr/bin/env bash
set -eu

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MAVEN_VERSION="3.9.10"
TOOLS_DIR="$ROOT/.tools"
PORTABLE_MVN="$TOOLS_DIR/apache-maven-${MAVEN_VERSION}/bin/mvn"

echo "==> Carbon-aware Java template — WSL + TensorFlow Java"
echo "    $ROOT"

if ! grep -qiE 'microsoft|wsl' /proc/version 2>/dev/null; then
  echo "ERROR: Run inside WSL. From Windows: npm run setup:wsl"
  exit 1
fi

ensure_java() {
  if command -v java >/dev/null 2>&1; then
    echo "==> $(java -version 2>&1 | head -n1)"
    return
  fi
  echo "==> Installing OpenJDK 21..."
  sudo apt-get update -qq
  sudo apt-get install -y openjdk-21-jdk
}

ensure_mvn() {
  if command -v mvn >/dev/null 2>&1; then
    printf '%s\n' mvn
    return
  fi
  if [[ -x "$PORTABLE_MVN" ]]; then
    printf '%s\n' "$PORTABLE_MVN"
    return
  fi
  echo "==> Downloading Maven ${MAVEN_VERSION} to .tools ..."
  mkdir -p "$TOOLS_DIR"
  local zip="/tmp/apache-maven-${MAVEN_VERSION}-bin.zip"
  command -v curl >/dev/null || sudo apt-get install -y curl unzip
  curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip" -o "$zip"
  unzip -qo "$zip" -d "$TOOLS_DIR"
  rm -f "$zip"
  chmod +x "$PORTABLE_MVN"
  printf '%s\n' "$PORTABLE_MVN"
}

ensure_java
MVN="$(ensure_mvn)"
echo "==> Maven: $($MVN -version | head -n1)"

if command -v nvidia-smi >/dev/null 2>&1; then
  echo "==> GPU:"
  nvidia-smi -L || true
  TF_PROFILE=tf-gpu
  TF_MODE=gpu
else
  echo "WARN: nvidia-smi not found — using Maven profile tf-cpu."
  TF_PROFILE=tf-cpu
  TF_MODE=cpu
fi

echo "==> mvn -P${TF_PROFILE} compile..."
"$MVN" -q -P"$TF_PROFILE" -DskipTests dependency:resolve compile

if [[ ! -f .env ]]; then
  cp .env.example .env 2>/dev/null || bash scripts/ensure-env.sh
fi

MAIN_URL="$(bash scripts/wsl-resolve-main-url.sh 2>/dev/null || echo "http://127.0.0.1:3915")"
export MAIN_URL TF_MODE
set_env_line() {
  local key="$1" val="$2"
  if grep -q "^${key}=" .env 2>/dev/null; then
    sed -i "s|^${key}=.*|${key}=${val}|" .env
  else
    echo "${key}=${val}" >> .env
  fi
}
set_env_line PORT 3916
set_env_line MAIN_SERVER_URL "$MAIN_URL"
set_env_line TF_BACKEND "$TF_MODE"
echo "==> .env updated (MAIN_SERVER_URL=${MAIN_URL}, TF_BACKEND=${TF_MODE})"

echo "==> Smoke (compile + TfBackend)..."
bash scripts/smoke-tf-gpu.sh || echo "WARN: smoke failed."

cat <<EOF

==> Setup complete.

From WSL (watch src/main/java):
  cd "$ROOT"
  bash scripts/start-wsl-tf.sh

From Windows:
  npm run start:wsl

EOF
