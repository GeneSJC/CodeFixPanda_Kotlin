#!/usr/bin/env bash
# Proves neither flavor contains advertising software: no ads or consent library on the release classpaths,
# and no AdMob entry or advertising-id permission in the merged release manifests. Run from the repository
# root; CI runs it too. A single transitive dependency can bring an ads library back, so run it whenever
# dependencies change.
#
# Ads are parked (docs/ads-restore.md), so there is no build that contains ads to use as a positive control.
# The restore guide says to bring a control back with the code (a check that the commercial build does
# contain the libraries, so this script cannot pass by looking in the wrong place).
set -euo pipefail

fail() { echo "FAIL: $1" >&2; exit 1; }

ADS_LIBS="play-services-ads|user-messaging-platform"

echo "== Dependencies"
for flavor in foss commercial; do
  out="/tmp/$flavor-deps.txt"
  ./gradlew --quiet :app:dependencies --configuration "${flavor}ReleaseRuntimeClasspath" > "$out"
  [ -s "$out" ] || fail "no dependency report for $flavor, so this check cannot be trusted"
  if grep -Ei "$ADS_LIBS" "$out"; then
    fail "the $flavor build lists an ads library"
  fi
done

echo "== Merged manifests"
./gradlew --quiet :app:processFossReleaseMainManifest :app:processCommercialReleaseMainManifest
manifest_of() {
  find app/build/intermediates -path "*$1*" -name AndroidManifest.xml | head -1
}
for flavor in foss commercial; do
  manifest=$(manifest_of "${flavor}Release")
  [ -n "$manifest" ] || fail "no merged manifest found for $flavor"
  if grep -Ei "gms.ads|android.permission.AD_ID|ACCESS_ADSERVICES" "$manifest"; then
    fail "the $flavor manifest has ads entries"
  fi
done

echo "OK: neither build contains advertising software."
