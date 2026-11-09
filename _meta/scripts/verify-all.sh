#!/usr/bin/env bash
# Runs every check CI runs, in one command, so a change can be verified before it is pushed: the unit
# tests and the debug builds for both flavors, then the two safety checks. Run it from anywhere inside
# the repository. It stops at the first failure and says which step failed.
set -euo pipefail

cd "$(dirname "$0")/../.."

echo "== Unit tests and debug builds, both flavors"
./gradlew testFossDebugUnitTest testCommercialDebugUnitTest assembleFossDebug assembleCommercialDebug --stacktrace

echo "== No advertising software in either flavor"
./_meta/scripts/check-no-ads-sdk.sh

echo "== No developer token in any release build"
./_meta/scripts/check-release-has-no-token.sh

echo "All checks passed."
