#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

MAVEN_VERSION="3.9.10"
TOOLS_DIR="$ROOT/.tools"
PORTABLE_MVN="$TOOLS_DIR/apache-maven-${MAVEN_VERSION}/bin/mvn"

echo "=== Carbon-aware template (Java) — Ubuntu setup ==="
echo "Installs: OpenJDK 21, Maven (apt or .tools), mvn dependency:resolve."
echo "Before run: set MAIN_SERVER_URL in .env to the URL from your facilitator."
echo ""

bash "$(dirname "$0")/../ensure-env.sh"

ensure_java() {
  if command -v java >/dev/null 2>&1; then
    echo "Java: $(java -version 2>&1 | head -n1)"
    return
  fi
  echo "Installing openjdk-21-jdk..."
  sudo apt-get update -qq
  sudo apt-get install -y openjdk-21-jdk
  echo "Java: $(java -version 2>&1 | head -n1)"
}

ensure_mvn() {
  if command -v mvn >/dev/null 2>&1; then
    echo "Maven: $(mvn -version | head -n1)" >&2
    printf '%s\n' mvn
    return
  fi
  if [[ -x "$PORTABLE_MVN" ]]; then
    echo "Maven: $($PORTABLE_MVN -version | head -n1)" >&2
    printf '%s\n' "$PORTABLE_MVN"
    return
  fi
  echo "Downloading Apache Maven ${MAVEN_VERSION} to .tools ..." >&2
  mkdir -p "$TOOLS_DIR"
  local zip="/tmp/apache-maven-${MAVEN_VERSION}-bin.zip"
  if ! command -v curl >/dev/null 2>&1; then
    sudo apt-get update -qq
    sudo apt-get install -y curl unzip
  fi
  curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip" -o "$zip"
  unzip -qo "$zip" -d "$TOOLS_DIR"
  rm -f "$zip"
  chmod +x "$PORTABLE_MVN"
  echo "Maven: $($PORTABLE_MVN -version | head -n1)" >&2
  printf '%s\n' "$PORTABLE_MVN"
}

ensure_java
MVN="$(ensure_mvn)"

echo "Resolving dependencies..."
"$MVN" -q -DskipTests dependency:resolve compile

echo ""
echo "Setup complete. Run: bash scripts/ubuntu/run.sh"
