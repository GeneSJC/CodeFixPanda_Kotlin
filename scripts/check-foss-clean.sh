#!/usr/bin/env bash
# Proves the open (foss) build contains no advertising software, and that this check can tell:
# the commercial build must contain it. Run from the repository root; CI runs it too. A single
# transitive dependency can bring an ads library back, so run it whenever dependencies change.
set -euo pipefail

fail() { echo "FAIL: $1" >&2; exit 1; }

echo "== Dependencies"
./gradlew --quiet :app:dependencies --configuration fossReleaseRuntimeClasspath > /tmp/foss-deps.txt
./gradlew --quiet :app:dependencies --configuration commercialReleaseRuntimeClasspath > /tmp/commercial-deps.txt
grep -Eiq "play-services-ads|user-messaging-platform" /tmp/commercial-deps.txt \
  || fail "the commercial build does not list the ads libraries, so this check cannot be trusted"
if grep -Ei "play-services-ads|user-messaging-platform" /tmp/foss-deps.txt; then
  fail "the foss build lists an ads library"
fi

echo "== Merged manifests"
./gradlew --quiet :app:processFossReleaseMainManifest :app:processCommercialReleaseMainManifest
manifest_of() {
  find app/build/intermediates -path "*$1*" -name AndroidManifest.xml | head -1
}
foss_manifest=$(manifest_of fossRelease)
commercial_manifest=$(manifest_of commercialRelease)
[ -n "$foss_manifest" ] || fail "no merged manifest found for foss"
[ -n "$commercial_manifest" ] || fail "no merged manifest found for commercial"
grep -q "com.google.android.gms.ads.APPLICATION_ID" "$commercial_manifest" \
  || fail "the commercial manifest has no AdMob app id, so this check cannot be trusted"
if grep -Ei "gms.ads|android.permission.AD_ID|ACCESS_ADSERVICES" "$foss_manifest"; then
  fail "the foss manifest has ads entries"
fi

echo "OK: the foss build contains no ads software."
