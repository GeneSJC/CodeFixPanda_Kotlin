#!/usr/bin/env bash
# Proves a release build never contains a developer's GitHub token, and that this check can tell: the
# debug builds must contain it. A made-up token is passed for the check (the real one is never needed).
# Run from the repository root; CI runs it too. A published app can be unpacked by anyone, so a token
# inside it would be shared by every user.
set -euo pipefail

fail() { echo "FAIL: $1" >&2; exit 1; }

SENTINEL="sentinel-token-for-the-release-check"

echo "== Generating BuildConfig for all four variants"
./gradlew --quiet -PGITHUB_API_TOKEN="$SENTINEL" \
  :app:generateFossDebugBuildConfig :app:generateCommercialDebugBuildConfig \
  :app:generateFossReleaseBuildConfig :app:generateCommercialReleaseBuildConfig

config_of() {
  find app/build/generated/source/buildConfig -path "*$1/*" -name BuildConfig.java | head -1
}

echo "== Debug builds carry the token (so this check can be trusted)"
for variant in fossDebug commercialDebug; do
  file=$(config_of "$variant")
  [ -n "$file" ] || fail "no BuildConfig found for $variant"
  grep -q "$SENTINEL" "$file" || fail "$variant does not carry the token, so this check cannot be trusted"
done

echo "== Release builds do not"
for variant in fossRelease commercialRelease; do
  file=$(config_of "$variant")
  [ -n "$file" ] || fail "no BuildConfig found for $variant"
  if grep -q "$SENTINEL" "$file"; then
    fail "$variant contains the developer token"
  fi
done

echo "OK: release builds contain no developer token."
