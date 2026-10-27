# Ads kit (AdMob, with an ad-free offer)

Generated @@DATE@@ from commit @@COMMIT@@ of GitHubIssueFinder by `scripts/export-ads-kit.sh`.
It is the advertising code of one Android app, packaged so another app can drop it in or read it as a
reference. Not compiled in this form: the first build in your project is the real test. SDK class names
were written from memory and verified only by that app's own builds, so a name may need adjusting for the
SDK version you use.

## What it gives you

- A **commercial** flavor with an AdMob adaptive banner, Google's consent flow (UMP) before anything is
  requested, and a rewarded video that buys an hour without the banner ("ad-free offer").
- A **foss** flavor with none of it: no ads library, no manifest entry, no advertising-id permission.
- Test ads in every build unless you deliberately build for production with three real ids.
- Shared code that knows only a small interface, `AdsController`, and nothing about AdMob.
- An **ads switch** (`adsEnabled`, off by default) that keeps all the ads code in the build but binds a
  do-nothing controller: no consent form, no SDK start, no banner, no offer. It is a plain build setting, not a
  flavor, so it adds nothing to the variant matrix. Turn ads on with `adsEnabled=true` in `local.properties`,
  in `~/.gradle/gradle.properties`, or as `-PadsEnabled=true`.

Read `docs/ADS_DESIGN_NOTES.md` for the design and `docs/ads-testing-rules.md` for the rules before any
live ad. The architecture and safety guides, if you were sent them, hold the same in more depth.

## What your project needs

- Two product flavors in one dimension, named `foss` and `commercial` (rename in the files if yours differ).
- Koin for dependency injection (the controller is supplied by a per-flavor Koin module).
- Jetpack Compose with Material 3, and `lifecycle-viewmodel` for the ViewModel.
- A version catalog is assumed in the snippets; adapt if you declare dependencies directly.
- `BuildConfig` generation enabled (`buildFeatures { buildConfig = true }`).
- `AdsViewModel` is written against plain `ViewModel` plus `KoinComponent`. If your project has its own base
  ViewModel, you may extend that instead.

## Steps

1. **Copy the files.** `./install.sh your.package.name path/to/your/app` copies everything in `files/` into
   `src/<set>/java/<your package path>/...`, swapping the placeholder package. Existing files are skipped:
   merge your own `src/commercial/AndroidManifest.xml` by hand (it needs the AdMob app-id meta-data).
2. **Gradle.** Paste from `snippets/`: the local-properties block (if you do not already read
   `local.properties`), the advertising block (sample ids, the production switch and its guard), and the
   flavors block (set your own application ids where `__PACKAGE__` appears). Add the dependency line, the
   version-catalog lines, and the manifest-placeholder line in each build type (`manifestPlaceholders`).
3. **Strings and backup rules.** Add the strings from `snippets/strings.xml.txt`, and the backup exclusion
   from `snippets/backup-rules.txt` so a restored backup cannot hand out an ad-free hour.
4. **Koin.** Start Koin with `adsModule` (each flavor defines its own, same name and package) next to your
   other modules. The commercial one reads the ads switch (`BuildConfig.ADS_ENABLED`, defined in the flavors
   snippet) and binds either the real controller or the shared `NoAdsController`.
5. **Start it once.** In your launcher Activity: `private val adsViewModel: AdsViewModel by viewModels()` and
   `adsViewModel.start(this)` in `onCreate`. It runs the consent flow, then starts the SDK.
6. **Show the banner.** In each screen that should carry it, expose `canShowAds` from your ViewModel
   (`adsController.canShowAds`, injected) and put `AdBannerSlot(visible = canShowAds, modifier = Modifier.navigationBarsPadding())`
   in the Scaffold's `bottomBar`.
7. **Offer the ad-free hour (optional).** Expose `adFreeUntilMs`, `canOfferAdFree` and a function calling
   `adsController.showAdFreeOffer(activity)`, and draw `AdFreeOfferLine(...)` in the title bar. Pass the
   Activity for the call only; never keep it.
8. **Privacy options.** Where privacy rules require it (`privacyOptionsRequired`), add a menu entry that calls
   `showPrivacyOptions(activity)`.
9. **Live label (optional).** Put `LIVE ADS` in your version label when `adsAreLive` is true, so a live build is
   never mistaken for a test one.
10. **Prove the open build is clean.** Copy `scripts/check-foss-clean.sh` (it assumes the module is `:app` and
    the flavors above) and run it in CI. Run both flavors' unit tests. Name the flavors in every Gradle task
    (`testFossDebugUnitTest testCommercialDebugUnitTest`): the bare task names are ambiguous with two flavors.

## Going live

Turn the switch on first (`adsEnabled=true`). Never use real ids in a debug build. Put the three real ids (`admobAppId`, `admobBannerUnitId`,
`admobRewardedUnitId`) in `local.properties` or `~/.gradle/gradle.properties`, never in a committed file, and build
with `-PadmobProduction=true`. Check your AdMob account standing first, and follow `docs/ads-testing-rules.md`:
never tap your own live ads and never ask anyone to.

## Files

@@FILES@@

## Keeping this current

The kit is generated, not edited: change the source app, then run `scripts/export-ads-kit.sh` again.
