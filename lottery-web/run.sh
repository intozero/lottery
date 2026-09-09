#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_root"
if [[ ! -f lottery-web/target/lottery-web-1.0-SNAPSHOT.jar ]]; then
  echo "Build first: mvn -pl lottery-web -am clean install" >&2
  exit 1
fi
exec java -jar lottery-web/target/lottery-web-1.0-SNAPSHOT.jar "$@"
