# Bringing ads back: the restore guide

Ads are **parked**: no build of this app contains the ads SDK, and none of the code that talks to it, not even the
small SDK-free hooks. Start at [`README.md`](README.md) in this folder for what the layer is worth and the setup it
needs. This guide is the way back. It is written so that a developer who has never seen the ads code can restore it, and so
that nothing has to be re-implemented or reverse-engineered. Read `_meta/docs/ads/architecture.md` first if you want to
understand how the ads layer works; this document is the procedure.

**A note on paths.** This guide was written when `docs/` and `scripts/` sat at the repository root. They now live under
`_meta/` (`_meta/docs/`, `_meta/scripts/`), and the guide uses the new paths for files that are in the repository today.
Paths inside the archives (the commit and the tags below) are the old ones, so a file checked out from an archive lands at
its old path and needs a `git mv` into `_meta/`, as the steps say.

## 0. Where the working implementation is

| What | Where | Notes |
|---|---|---|
| **The mainline archive** | commit `9d61e0f1fd0814f68d8061cd24698bb069d6e70d` on `develop` (the merge of PR #37) | The last commit with the complete working ads layer: real controller, consent, banner, rewarded video and ad-free hour. It is part of `develop`'s history, so it stays reachable for as long as `develop` exists. **This is the primary source for the steps below.** |
| **The tag, a superset** | tag `ads-parked-2026-10-27`, commit `da3ac8725721cc88af0b447ac0fbc6753baa4150` | The same implementation plus two things built afterwards and never merged: the **ads kit** (`scripts/export-ads-kit.sh`, which packages the ads code for another project) and the **ads switch** (`adsEnabled`, which binds a do-nothing controller while keeping the SDK in the build). Use it only if you want those. Check it exists: `git tag -l 'ads-parked*'`. |

Two more places matter now that the SDK-free hooks have been removed from the code too:

| What | Where | Notes |
|---|---|---|
| **The hooks, as they were** | tag `ads-seam-2026-11-03`, commit `4d8c7f6c49a20d24e55f1103cb04bc6bf61a3ce5` | `develop` just before the clean-out: the `AdsController` interface, `NoAdsController`, `AdBannerSlot`, `AdFreeOfferLine`, `AdsViewModel`, the ViewModel flows, the menu entries, both flavors' stub modules and banners, and the tests. These docs were at `docs/ads-*.md` then. |
| **The clean-out commit** | `0428e36eb21242a44985f2e81e83f05e424c8125` (Build 54.2) | The single commit that removed the hooks and their uses. `git show` it to see the exact edit; `git revert --no-commit` it to re-apply the inverse (step 1a). |

Look inside before you start: `git show --stat 9d61e0f1fd0814f68d8061cd24698bb069d6e70d` and `git ls-tree -r --name-only 9d61e0f1fd0814f68d8061cd24698bb069d6e70d | grep -iE 'ads|AdBanner|AdFree'`.

**Copy files out; do not merge.** By the time ads come back, `develop` will have moved on, and a merge would
conflict across many files. Restoring is a copy of self-contained files, a few build blocks, and a re-check of the
integration points (section 4).

## 1. What was parked

**Removed from every build (restore these).** The paths below are as they are in the archive, which predates the package rename (the code package was `io.github.genesjc.githubissuefinder` then, `io.github.genesjc.opensourcepanda` until Build 61, and is `io.github.genesjc.codefixpanda` now). Step 3.2 moves the files into the current package.

| Piece | Path in the archive | Role |
|---|---|---|
| Real controller | `app/src/commercial/java/io/github/genesjc/githubissuefinder/ads/AdMobAdsController.kt` | Consent (UMP), SDK start, test devices, rewarded video, the flags |
| Ad-free period | `app/src/commercial/java/io/github/genesjc/githubissuefinder/ads/AdFreePeriod.kt` | The earned hour: start, expiry as a Flow, saved end time, clock clamp |
| Real module | `app/src/commercial/java/io/github/genesjc/githubissuefinder/ads/AdsModule.kt` | Koin module binding the real controller and the period (**overwrites the parked stub**) |
| Real banner | `app/src/commercial/java/io/github/genesjc/githubissuefinder/ui/ads/AdBanner.kt` | Adaptive banner, reserved height, offline message (**overwrites the parked stub**) |
| Manifest entry | `app/src/commercial/AndroidManifest.xml` | The AdMob app id the SDK needs at start-up |
| Period test | `app/src/testCommercial/java/io/github/genesjc/githubissuefinder/ads/AdFreePeriodTest.kt` | Tests of the period under virtual time |
| Build blocks | `app/build.gradle.kts` | Sample ids, the production flag and guard, flavor build-config fields, manifest placeholders, the dependency (section 3 and appendix A) |
| Version catalog | `gradle/libs.versions.toml` | The ads library version and entry (appendix A) |
| Strings | `app/src/main/res/values/strings.xml` | `ad_banner_offline` (appendix A) |
| Backup rules | `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml` | Exclude `ad_free_prefs.xml` (appendix A) |
| Clean-build check | `scripts/check-foss-clean.sh` and a CI step | Proves the open build has no ads software, with a positive control on the commercial build. Parking replaced it with `_meta/scripts/check-no-ads-sdk.sh` (section 3, step 8) |

**Also removed, in the clean-out (restore these first; step 1a):** the SDK-free hooks that had stayed in the code while ads
were parked: the `AdsController` interface and `NoAdsController` (`ads/`), `AdsViewModel`, `AdBannerSlot` and
`AdFreeOfferLine` (`ui/ads/`), the ViewModel flows (`canShowAds`, `adFreeUntilMs`, `canOfferAdFree`,
`privacyOptionsRequired`) in the Top Repos and repo-issues ViewModels, the banner slots and the offer line in both
screens, the ad-free and privacy entries in the Top Repos menu, the `adsAreLive` marker ("LIVE ADS") in the version
label, the `adsModule` loaded by the application's Koin start-up, the `adsViewModel.start(this)` call in `MainActivity`,
both flavors' own `AdsModule` and `AdBanner` stubs, `FakeAdsController` and the tests of the hooks, and the strings
the hooks used (`action_hide_ads`, `ad_free_remaining`, `menu_privacy_options`). Their paths in the seam tag use the
current package, so no renaming is needed for them.

## 2. Prerequisites: finish these before going live

Restoring the code and building with **test ads** needs none of them. Going live does.

1. **The ad account.** Check the AdMob/AdSense account standing first. A previously disabled publisher may not be
   able to open a new account, so find out before relying on ads. One account only, with 2-step verification on.
2. **Privacy policy** covering ads, and a consent setup that matches (the consent form is part of the restored code).
3. **Store declarations** drafted: whether the app contains ads, and whether it uses the advertising ID (the SDK can
   add the advertising-ID permission to the manifest by itself). Answer from what the shipped file contains. Check the
   store's current policy text; this guide does not quote it.
4. **Three real ad ids** from the ad account (app id, banner unit, rewarded unit), to keep as private properties, never
   in a committed file.
5. **Test devices** registered in the ad console for everyone who will run a build.
6. Read `_meta/docs/ads/testing-rules.md`: never tap your own live ads, never ask anyone to.

## 3. The restore, step by step

Work on a branch from the latest `develop`, one step per commit, and build after each of steps 2 to 7.

1. **Branch.** `git checkout -b feature-set/adsRestore origin/develop`
   1a. **Re-apply the clean-out in reverse.** It was one commit, so the hooks, their uses in the screens, ViewModels, Koin and
   `MainActivity`, the strings and the tests all come back together:
```
git revert --no-commit 0428e36eb21242a44985f2e81e83f05e424c8125
```
   `develop` will have moved, so expect conflicts in the files the clean-out touched (the two ViewModels, the Top Repos and
   repo-issues screens, the drawer, `AppModule`). Resolve each by keeping today's code and re-adding the ad lines shown by
   `git show 0428e36eb21242a44985f2e81e83f05e424c8125`. Keep today's build-label values in `app/build.gradle.kts` (do not
   take the old label). The two stub files this brings back in `src/commercial` are overwritten by step 2.
2. **Copy the files** out of the mainline archive (overwriting the two parked stubs):
```
git checkout 9d61e0f1fd0814f68d8061cd24698bb069d6e70d -- \
  app/src/commercial/AndroidManifest.xml \
  app/src/commercial/java/io/github/genesjc/githubissuefinder/ads/AdMobAdsController.kt \
  app/src/commercial/java/io/github/genesjc/githubissuefinder/ads/AdFreePeriod.kt \
  app/src/commercial/java/io/github/genesjc/githubissuefinder/ads/AdsModule.kt \
  app/src/commercial/java/io/github/genesjc/githubissuefinder/ui/ads/AdBanner.kt \
  app/src/testCommercial/java/io/github/genesjc/githubissuefinder/ads/AdFreePeriodTest.kt
```
   Then move the copied files into the current package (the archive uses the old package name, so the paths and the
   `package` and `import` lines all change). The current stubs sit in the new folders, and this overwrites them:
```
NEW=io/github/genesjc/codefixpanda
for OLD in io/github/genesjc/githubissuefinder io/github/genesjc/opensourcepanda; do
  for set in commercial testCommercial; do
    if [ -d app/src/$set/java/$OLD ]; then
      mkdir -p app/src/$set/java/$NEW
      cp -R app/src/$set/java/$OLD/. app/src/$set/java/$NEW/
      git rm -r -q --cached app/src/$set/java/$OLD && rm -rf app/src/$set/java/$OLD
    fi
  done
done
grep -rlE "io.github.genesjc.(githubissuefinder|opensourcepanda)" app/src/commercial app/src/testCommercial \
  | xargs sed -i -E 's/io\.github\.genesjc\.(githubissuefinder|opensourcepanda)/io.github.genesjc.codefixpanda/g'
git add -A app/src/commercial app/src/testCommercial
```
3. **Gradle: the advertising block.** Paste appendix A1 into `app/build.gradle.kts` below the build-label values and above
   `splashAnimation`. It defines Google's sample ids, the `admobProduction` flag, its guard and the release ids. (The
   `privateProperty` function it uses is already in the file.)
4. **Gradle: the commercial flavor fields.** Replace the `commercial` flavor block with appendix A2.
5. **Gradle: the manifest placeholders.** Add appendix A3's two lines inside the `debug` and `release` build types.
6. **Dependency and catalog.** Add appendix A4 to `dependencies { }` and appendix A5 to `gradle/libs.versions.toml`.
7. **Strings and backup rules.** Add appendix A6 to `strings.xml` and appendix A7 to the two XML files.
8. **The clean-build check.** Restore the old script and CI step, which add the positive control back:
   `git checkout 9d61e0f1fd0814f68d8061cd24698bb069d6e70d -- scripts/check-foss-clean.sh`, move it with `git mv scripts/check-foss-clean.sh _meta/scripts/`, then `git rm _meta/scripts/check-no-ads-sdk.sh`, and replace the
   CI step "Builds contain no ads software" with appendix A8. (The restored script requires the foss build to be
   clean and the commercial build to contain the libraries.)
9. **Update the docs.** Replace the "Ads are parked" notes in `README.md` and `_meta/docs/design-notes.md` with a description of
   the live ads layer (the old text is in `git show 9d61e0f1fd0814f68d8061cd24698bb069d6e70d:docs/design-notes.md`, sections "Advertising" and
   "Ad-free offer"). Update the status line at the top of `_meta/docs/ads/README.md`, keep `_meta/docs/ads/architecture.md`, and update the
   comments that say ads are parked in `AdsController.kt`, `NoAdsController.kt` and the foss `AdsModule.kt`.
10. **Bump the build label** in the same commit as the work.

**Optional, from the tag (only if you want them):** the **ads switch** (`adsEnabled`, off by default) and the **kit export**.
`git checkout ads-parked-2026-10-27 -- scripts/export-ads-kit.sh scripts/ads-kit`, move them with `git mv scripts/export-ads-kit.sh scripts/ads-kit _meta/scripts/`, and take the `ads-kit:begin/end`
marker comments and the `adsEnabled` block from that tag's `app/build.gradle.kts` and its commercial `AdsModule.kt`.
The switch is not needed for ads to work; skip it unless you want a way to keep ads off in some builds.

## 4. Re-check for drift

`develop` has moved since the archive. Before the first build, check:

- **The interface.** Compare the restored `ads/AdsController.kt` (step 1a) with the restored controller: every member the controller overrides must
  still exist, with the same types. If a member was added or removed, update the controller, `NoAdsController` and
  `FakeAdsController` together.
- **The hooks in each screen.** Each screen that carried a banner or the offer line still calls `AdBannerSlot(visible = canShowAds, ...)`
  and `AdFreeOfferLine(...)`, and its ViewModel still exposes the flows. New screens that should show a banner need the
  same two additions.
- **Koin.** The commercial `adsModule` is loaded by the application's Koin start-up next to the app module, and `AdsViewModel.start(activity)`
  is still called once from `MainActivity`.
- **Build files.** The flavor names, `localProperties` and `privateProperty` still exist as the blocks assume. The dependency must still use the
  string form `add("commercialImplementation", ...)`.
- **The SDK itself.** The ads SDK's API changes between versions. The class names in the restored files were written from memory and checked
  only by this app's builds; a newer SDK may need adjustments (section 7).
- **Style and conventions.** Imports, lint rules and the comment conventions of the day.

## 5. Verification and test plan

1. **Build both flavors, debug and release**, naming them in every Gradle task (the bare task names are ambiguous with two flavors):
   `./gradlew assembleFossDebug assembleCommercialDebug` and the unit tests `testFossDebugUnitTest testCommercialDebugUnitTest`.
2. **The clean-build check.** `./_meta/scripts/check-foss-clean.sh` must pass: the foss build has no ads library or manifest entry; the commercial build
   does (the positive control).
3. **Run `commercialDebug` on a device with test ads.** Check: the consent form appears where required (a debug build with
   `ADMOB_TEST_DEVICE_ID` set simulates the EU); a banner appears at the bottom of Top Repos and repo issues, labelled "Test Ad"; the offer line
   "Hide ads for 1 hour" appears after the video loads; playing the sample video and finishing it hides the banner and shows a countdown; the banner
   returns when the hour ends; closing the video early earns nothing; with the network off the banner strip says "You're offline".
4. **Run `fossDebug`:** no banner, no offer, no consent.
5. **The commercial release build** uses **sample ids** unless built for production.

## 6. Going live

1. Put the three real ids in `local.properties` or `~/.gradle/gradle.properties`, as `admobAppId`, `admobBannerUnitId`, `admobRewardedUnitId`.
   Never in the committed `gradle.properties`.
2. Build with `./gradlew bundleCommercialRelease -PadmobProduction=true`. The build refuses to configure if any id is missing.
3. Confirm the drawer's version label says **LIVE ADS**. Install that build only from the store, on no tester's phone.
4. Update the store declarations (contains ads; advertising ID) to match the shipped file.
5. After launch, watch the invalid-traffic report and act on any warning at once.

## 7. Troubleshooting

- **A class or method from the SDK is unresolved.** The SDK version in the catalog differs from the one the code was written for. Check the SDK's current
  release notes for the renamed class (rewarded ads, `FullScreenContentCallback`, `RequestConfiguration`, the consent classes) and adjust the restored file.
- **The app crashes at start-up with a message about the ads app id.** The manifest entry is missing or the placeholder `${admobAppId}` is not set in the
  current build type. Restore appendix A3 and A9.
- **The consent form never appears.** Consent is only requested in regions where it is required, unless a debug build has `ADMOB_TEST_DEVICE_ID` set,
  which simulates the EU.
- **The foss build lists the ads library.** A dependency was added to the wrong configuration; it must be `commercialImplementation` only.
- **"Cannot access class ComposableFunction1" in the editor.** An editor index problem, not a build error: run the build, then sync and invalidate caches.
- **CI fails on a bare task name.** Name the flavors: `testFossDebugUnitTest testCommercialDebugUnitTest`, `assembleFossDebug assembleCommercialDebug`.

## 8. Effort estimate

About half a day if nothing drifted: copying files and blocks is minutes; the re-check and the first build take the rest. Add time for the
prerequisites (account, privacy policy, store declarations), which are not code and are the real lead time.

## Appendix A: the removed build blocks, verbatim

Taken from commit `9d61e0f1fd0814f68d8061cd24698bb069d6e70d`.

### A1. `app/build.gradle.kts`: the advertising block (top level, above `splashAnimation`)

```kotlin
// ---- Advertising ids, and the production switch ----
// Safe by default: every build, release included, uses Google's published sample ids, so it serves only
// test ads and can never earn or be penalized. Live ads need a deliberate production build:
//     ./gradlew bundleCommercialRelease -PadmobProduction=true
// with all three real ids supplied as private properties (admobAppId, admobBannerUnitId, admobRewardedUnitId):
// in local.properties, in ~/.gradle/gradle.properties, or as -P options (CI secrets). Never in the repository's
// own gradle.properties, which is committed. A production build refuses to configure without all three, so
// test ads cannot be shipped as live ones by mistake. Debug builds ignore all of this and always use the sample ids. The foss build has no ads.
val admobSampleAppId = "ca-app-pub-3940256099942544~3347511713"
val admobSampleBannerUnitId = "ca-app-pub-3940256099942544/9214589741"
val admobSampleRewardedUnitId = "ca-app-pub-3940256099942544/5224354917"
val admobProduction = providers.gradleProperty("admobProduction").orNull == "true"

if (admobProduction) {
    val missing = listOf("admobAppId", "admobBannerUnitId", "admobRewardedUnitId").filter { privateProperty(it) == null }
    if (missing.isNotEmpty()) {
        throw GradleException(
            "-PadmobProduction=true needs all three real AdMob ids as private properties (local.properties or Gradle); missing: $missing. " +
                "Leave the flag off to build with Google's test ids.",
        )
    }
}
val releaseAdmobAppId = if (admobProduction) privateProperty("admobAppId")!! else admobSampleAppId
val releaseAdmobBannerUnitId =
    if (admobProduction) privateProperty("admobBannerUnitId")!! else admobSampleBannerUnitId
val releaseAdmobRewardedUnitId =
    if (admobProduction) privateProperty("admobRewardedUnitId")!! else admobSampleRewardedUnitId
```

### A2. `app/build.gradle.kts`: the commercial flavor with its ads fields (inside `productFlavors`)

```kotlin
        create("commercial") {
            dimension = "distribution"
            applicationId = "io.github.genesjc.codefixpanda.commercial"
            versionNameSuffix = "-commercial"
            // The banner ad unit for release builds: Google's sample unit unless this is a production build
            // (see the advertising block at the top). Debug builds ignore it and always use the sample unit.
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$releaseAdmobBannerUnitId\"")
            // The same for the rewarded video behind the ad-free offer.
            buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"$releaseAdmobRewardedUnitId\"")
            // Your device's test id, which the ads SDK prints in the log, set as ADMOB_TEST_DEVICE_ID in
            // local.properties. In every build that is not a production build it is registered as a test device,
            // and in a debug build it also lets the consent form be seen outside the EU. Empty by default.
            buildConfigField(
                "String",
                "ADMOB_TEST_DEVICE_ID",
                "\"${localProperties.getProperty("ADMOB_TEST_DEVICE_ID", "")}\"",
            )
        }
```

### A3. `app/build.gradle.kts`: in `buildTypes`, one line in each type

```kotlin
manifestPlaceholders["admobAppId"] = admobSampleAppId
manifestPlaceholders["admobAppId"] = releaseAdmobAppId
```
The first line goes in `debug`, the second in `release`.

### A4. `app/build.gradle.kts`: inside `dependencies`

```kotlin
    // Only the commercial build contains the ads software; the foss build never does (see
    // _meta/scripts/check-foss-clean.sh). The string form is used because the typed accessor for a flavor's
    // configuration is not generated when the flavors are declared in this same script.
    add("commercialImplementation", libs.play.services.ads)
```

### A5. `gradle/libs.versions.toml`: one line under `[versions]`, one under `[libraries]`

```toml
playServicesAds = "25.4.0"
play-services-ads = { group = "com.google.android.gms", name = "play-services-ads", version.ref = "playServicesAds" }
```

### A6. `app/src/main/res/values/strings.xml`

```xml
    <string name="ad_banner_offline">You\'re offline</string>
```

### A7. Backup rules: `res/xml/backup_rules.xml`, and `res/xml/data_extraction_rules.xml` (twice: cloud backup and device transfer)

```xml
    <exclude domain="sharedpref" path="ad_free_prefs.xml"/>
        <exclude domain="sharedpref" path="ad_free_prefs.xml"/>
        <exclude domain="sharedpref" path="ad_free_prefs.xml"/>
```
Each file also had a comment line above the first exclusion: "The ad-free period stays on this device too. Keep in sync with PREFS_NAME in ads/AdFreePeriod.kt."

### A8. `.github/workflows/ci.yml`: the clean-build step

```yaml
      # Proves the open (foss) build has no ads libraries or manifest entries, and that the check can
      # tell (the commercial build must have them). See _meta/scripts/check-foss-clean.sh.
      - name: Open build contains no ads software
        run: ./_meta/scripts/check-foss-clean.sh
```

### A9. `app/src/commercial/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
    Only the commercial build's manifest mentions the ads software. The AdMob SDK refuses to start
    without its app id here, and the foss build must contain no ads entries at all
    (_meta/scripts/check-foss-clean.sh checks that). The value comes from a placeholder set per build type
    in app/build.gradle.kts: Google's sample id in debug, the real id from a Gradle property in release.
-->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application>
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="${admobAppId}" />
    </application>

</manifest>
```
