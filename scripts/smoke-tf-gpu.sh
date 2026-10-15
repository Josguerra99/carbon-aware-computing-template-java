#!/usr/bin/env bash
set -eu
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MAVEN_VERSION="3.9.10"
PORTABLE_MVN="$ROOT/.tools/apache-maven-${MAVEN_VERSION}/bin/mvn"
MVN=mvn
command -v mvn >/dev/null || MVN="$PORTABLE_MVN"

PROFILE=tf-cpu
if [[ "${TF_BACKEND:-}" == "gpu" || "${TF_BACKEND:-}" == "cuda" ]]; then
  PROFILE=tf-gpu
fi

"$MVN" -q -P"$PROFILE" -DskipTests compile exec:java \
  -Dexec.mainClass=team.carbonaware.template.core.tf.TfBackend \
  -Dexec.cleanupDaemonThreads=false
