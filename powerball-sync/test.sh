#!/usr/bin/env bash
set -euo pipefail
module_dir="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$module_dir/target/test-classes"
javac -source 8 -target 8 -Xlint:-options -d "$module_dir/target/test-classes" "$module_dir/src/main/java/com/vipin/lottery/powerball/PowerballSync.java" "$module_dir/src/test/java/com/vipin/lottery/powerball/PowerballSyncTest.java"
java -cp "$module_dir/target/test-classes" com.vipin.lottery.powerball.PowerballSyncTest
