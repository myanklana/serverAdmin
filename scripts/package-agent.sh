#!/usr/bin/env sh
# Builds dist/server-manager-agent.jar (and its SHA-256) using the Maven wrapper.
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
(cd "$root/api" && ./mvnw -B -q -f ../agent/pom.xml package)
mkdir -p "$root/dist"
cp "$root"/agent/target/agent-*-SNAPSHOT.jar "$root/dist/server-manager-agent.jar" 2>/dev/null \
  || cp "$root"/agent/target/agent-*.jar "$root/dist/server-manager-agent.jar"
(cd "$root/dist" && sha256sum server-manager-agent.jar > server-manager-agent.jar.sha256)
echo "Pronto: $root/dist/server-manager-agent.jar"
