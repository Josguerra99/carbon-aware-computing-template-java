#!/usr/bin/env bash
set -eu

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! grep -qiE 'microsoft|wsl' /proc/version 2>/dev/null; then
  echo "ERROR: Run inside WSL."
  exit 1
fi

MAVEN_VERSION="3.9.10"
PORTABLE_MVN="$ROOT/.tools/apache-maven-${MAVEN_VERSION}/bin/mvn"
MVN=mvn
command -v mvn >/dev/null || MVN="$PORTABLE_MVN"

echo "==> Maven profile tf-gpu..."
"$MVN" -q -Ptf-gpu -DskipTests dependency:resolve compile

if grep -q '^TF_BACKEND=' .env 2>/dev/null; then
  sed -i 's/^TF_BACKEND=.*/TF_BACKEND=gpu/' .env
else
  echo "TF_BACKEND=gpu" >> .env
fi

bash scripts/smoke-tf-gpu.sh
