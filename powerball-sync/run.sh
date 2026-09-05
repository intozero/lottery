#!/usr/bin/env bash
set -euo pipefail
module_dir="$(cd "$(dirname "$0")" && pwd)"
cd "$module_dir/.."
mkdir -p "$module_dir/target/classes"
javac -source 8 -target 8 -Xlint:-options -d "$module_dir/target/classes" "$module_dir/src/main/java/com/vipin/lottery/powerball/PowerballSync.java"
java -cp "$module_dir/target/classes" com.vipin.lottery.powerball.PowerballSync "$@"
