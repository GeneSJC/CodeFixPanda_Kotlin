#!/usr/bin/env bash
# Exports the app's advertising code as a kit another project can drop in or use as a reference:
#   ./scripts/export-ads-kit.sh [output folder]      (default: build/ads-kit, plus build/ads-kit.zip)
# The kit is generated from the current files, so it cannot drift from them. The package name becomes a
# placeholder that the kit's install.sh swaps for the other project's. Run from anywhere.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT="${1:-build/ads-kit}"
PKG="io.github.genesjc.githubissuefinder"
PKG_PATH="io/github/genesjc/githubissuefinder"

fail() { echo "FAIL: $1" >&2; exit 1; }

rm -rf "$OUT"
mkdir -p "$OUT/files" "$OUT/snippets" "$OUT/scripts" "$OUT/docs"

placeholder() { perl -pe "s/\Q$PKG\E/__PACKAGE__/g"; }

# Copies one source file into files/, keeping its place under src/ and turning the package into a placeholder.
copy() {
  [ -f "$1" ] || fail "missing source file $1"
  rel="${1#app/src/}"
  dest="$OUT/files/$(echo "$rel" | sed "s#$PKG_PATH#PACKAGE_PATH#")"
  mkdir -p "$(dirname "$dest")"
  placeholder < "$1" > "$dest"
}

P="app/src"
for f in \
  "$P/main/java/$PKG_PATH/ads/AdsController.kt" \
  "$P/main/java/$PKG_PATH/ui/ads/AdsViewModel.kt" \
  "$P/main/java/$PKG_PATH/ui/ads/AdBannerSlot.kt" \
  "$P/main/java/$PKG_PATH/ui/ads/AdFreeOffer.kt" \
  "$P/main/java/$PKG_PATH/ui/ContextExtensions.kt" \
  "$P/foss/java/$PKG_PATH/ads/AdsModule.kt" \
  "$P/foss/java/$PKG_PATH/ui/ads/AdBanner.kt" \
  "$P/commercial/java/$PKG_PATH/ads/AdsModule.kt" \
  "$P/commercial/java/$PKG_PATH/ads/AdMobAdsController.kt" \
  "$P/commercial/java/$PKG_PATH/ads/AdFreePeriod.kt" \
  "$P/commercial/java/$PKG_PATH/ui/ads/AdBanner.kt" \
  "$P/commercial/AndroidManifest.xml" \
  "$P/test/java/$PKG_PATH/testutil/FakeAdsController.kt" \
  "$P/test/java/$PKG_PATH/ui/ads/AdFreeMinutesLeftTest.kt" \
  "$P/testCommercial/java/$PKG_PATH/ads/AdFreePeriodTest.kt" \
  "$P/testFoss/java/$PKG_PATH/ads/NoAdsControllerTest.kt"; do
  copy "$f"
done

# The kit's AdsViewModel must not depend on this app's own base ViewModel.
vm="$OUT/files/main/java/PACKAGE_PATH/ui/ads/AdsViewModel.kt"
perl -0pi -e 's/import __PACKAGE__\.ui\.BaseViewModel\n/import androidx.lifecycle.ViewModel\nimport org.koin.core.component.KoinComponent\n/; s/: BaseViewModel\(\)/: ViewModel(), KoinComponent/' "$vm"
grep -q "BaseViewModel" "$vm" && fail "AdsViewModel still mentions BaseViewModel; adjust the export script"

# Snippets, cut out of the real files between ads-kit markers, or picked by name.
extract() { sed -n "/ads-kit:begin $1/,/ads-kit:end $1/p" app/build.gradle.kts | sed '1d;$d' | placeholder; }
for block in local-properties advertising flavors; do
  extract "$block" > "$OUT/snippets/build.gradle.kts.$block.txt"
  [ -s "$OUT/snippets/build.gradle.kts.$block.txt" ] || fail "no ads-kit:$block block found in app/build.gradle.kts"
done
{
  echo "// In dependencies { }:"
  grep 'commercialImplementation' app/build.gradle.kts | sed 's/^ *//'
  echo
  echo "// In each build type (debug and release), next to your other settings:"
  grep 'manifestPlaceholders\["admobAppId"\]' app/build.gradle.kts | sed 's/^ *//'
  echo
  echo "# In gradle/libs.versions.toml, under [versions] and [libraries]:"
  grep -E 'playServicesAds|play-services-ads' gradle/libs.versions.toml
} > "$OUT/snippets/dependencies.txt"
grep -E 'name="(action_hide_ads|ad_free_remaining|ad_banner_offline|menu_privacy_options)"' \
  app/src/main/res/values/strings.xml > "$OUT/snippets/strings.xml.txt"
grep -H 'ad_free_prefs' app/src/main/res/xml/*.xml > "$OUT/snippets/backup-rules.txt"
for f in dependencies.txt strings.xml.txt backup-rules.txt; do
  [ -s "$OUT/snippets/$f" ] || fail "empty snippet $f"
done

# The check that the open build has no ads software, and the docs.
cp scripts/check-foss-clean.sh "$OUT/scripts/"
cp docs/ads-testing-rules.md "$OUT/docs/"
awk '/^## /{keep = ($0 == "## Advertising" || $0 == "## Ad-free offer")} keep' docs/design-notes.md \
  | placeholder > "$OUT/docs/ADS_DESIGN_NOTES.md"
[ -s "$OUT/docs/ADS_DESIGN_NOTES.md" ] || fail "no Advertising section found in docs/design-notes.md"

# The integration guide, with the file list filled in, and the installer.
cp scripts/ads-kit/install.sh "$OUT/install.sh"
chmod +x "$OUT/install.sh"
filelist=$( (cd "$OUT" && find files snippets scripts docs -type f | sort | sed 's/^/- `/; s/$/`/') )
commit=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
FILELIST="$filelist" perl -pe '
  s/\@\@DATE\@\@/'"$(date +%Y-%m-%d)"'/;
  s/\@\@COMMIT\@\@/'"$commit"'/;
  s/\@\@FILES\@\@/$ENV{FILELIST}/;
' scripts/ads-kit/README.template.md > "$OUT/README.md"

# Nothing that names this app's package may be left behind.
if grep -rIl "genesjc" "$OUT" >/dev/null 2>&1; then
  grep -rIn "genesjc" "$OUT" >&2 || true
  fail "the kit still refers to this app's package"
fi

if command -v zip >/dev/null 2>&1; then
  (cd "$(dirname "$OUT")" && rm -f "$(basename "$OUT").zip" && zip -qr "$(basename "$OUT").zip" "$(basename "$OUT")")
  echo "Kit: $OUT and $OUT.zip"
else
  echo "Kit: $OUT (zip not installed, so no archive)"
fi
