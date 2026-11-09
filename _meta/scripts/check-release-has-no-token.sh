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

# Newer Android Gradle plugins write a flavored variant to buildConfig/<flavor>/<buildType>, older ones to
# buildConfig/<flavor><BuildType>; look in both. Arguments: flavor, build type, variant name.
config_of() {
  find app/build/generated/source/buildConfig -name BuildConfig.java \
    | grep -E "/buildConfig/($1/$2|$3)/" | head -1 || true
}

echo "== Debug builds carry the token (so this check can be trusted)"
for entry in "foss debug fossDebug" "commercial debug commercialDebug"; do
  set -- $entry
  file=$(config_of "$1" "$2" "$3")
  [ -n "$file" ] || fail "no BuildConfig found for $3"
  grep -q "$SENTINEL" "$file" || fail "$3 does not carry the token, so this check cannot be trusted"
done

echo "== Release builds do not"
for entry in "foss release fossRelease" "commercial release commercialRelease"; do
  set -- $entry
  file=$(config_of "$1" "$2" "$3")
  [ -n "$file" ] || fail "no BuildConfig found for $3"
  if grep -q "$SENTINEL" "$file"; then
    fail "$3 contains the developer token"
  fi
done

echo "OK: release builds contain no developer token."
