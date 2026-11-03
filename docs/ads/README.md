# Ads: parked on purpose, worth bringing back

**Status: parked.** No build of Open Source Panda contains the ads SDK, and, since the clean-out, none of the code
that talked to it either. This is a deliberate choice, not an abandoned feature. A working, tested ads layer exists,
kept in the repository's history, and everything needed to restore it is written down here.

If you are deciding whether ads are worth reviving, read this page first. It is the front door; the three documents
next to it hold the detail.

## What you would be getting back

A complete, working ads layer for the `commercial` (Google Play) build, built on Google AdMob:

- **Consent first.** Google's User Messaging Platform consent form, shown where privacy rules require it, with a
  "Privacy options" menu entry to reopen the choice. Nothing is requested from the ad network until consent allows it.
- **A bottom banner** on Top Repos and on the repo-issues screen: an adaptive banner with its height reserved so the
  list does not jump, and an "You're offline" message in the strip when there is no network.
- **An opt-in reward.** One rewarded video that earns an ad-free hour. It plays only after the user taps the offer. A
  countdown shows in the title bar, the period survives the app being closed, and a clock change cannot stretch it.
- **Test ads by default.** Every build used Google's sample ids unless it was a deliberate production build, so
  testers could never serve or tap live ads.
- **Tests.** The ad-free period is unit-tested under virtual time; the ViewModel flows and the do-nothing controller
  have their own tests.
- **A proof that the open build is clean.** A CI script checks that no ads or consent library and no AdMob manifest
  entry is in either build. That check stays in the repository today (`scripts/check-no-ads-sdk.sh`).

Restoring it is a copy of files out of the history plus a re-check, about half a day if nothing has drifted. The real
lead time is not code: it is the ad account, the privacy policy and the store declarations (see "Before going live").

## Where it lives

The code is not in the mainline; it is in these places in the history. Do not delete them.

| What | Where | Use it for |
|---|---|---|
| **The working implementation** | commit `9d61e0f1fd0814f68d8061cd24698bb069d6e70d` on `develop` (the merge of PR #37) | The real controller, consent, banner, rewarded video and ad-free hour. The primary source for the SDK-bearing files. |
| **The same, plus extras** | tag `ads-parked-2026-10-27` (commit `da3ac8725721cc88af0b447ac0fbc6753baa4150`) | Also holds the **ads kit** (`scripts/export-ads-kit.sh`, which packages the ads code for another project) and the **ads switch**. Only if you want those. |
| **The SDK-free hooks** | tag `ads-seam-2026-11-03` (commit `4d8c7f6c49a20d24e55f1103cb04bc6bf61a3ce5`) | `develop` as it was just before the clean-out: the `AdsController` interface, `NoAdsController`, the banner slot, the offer line, the ViewModel flows, the menu entries, the per-flavor stubs and their tests. |
| **The clean-out itself** | commit `0428e36eb21242a44985f2e81e83f05e424c8125` (Build 54.2) | `git show 0428e36` is exactly the edit to re-apply to the screens, ViewModels, DI, `MainActivity` and strings when restoring. |

Check they exist before you start: `git tag -l 'ads-*'`. Look inside without checking anything out:
`git show --stat ads-seam-2026-11-03` and `git ls-tree -r --name-only ads-seam-2026-11-03 | grep -iE 'ads|AdBanner|AdFree'`.

**Copy files out; do not merge.** By the time ads come back `develop` will have moved on, and a merge would conflict
across many files.

## What is in this folder

| Document | What it holds |
|---|---|
| [`restore.md`](restore.md) | The procedure: prerequisites, the step-by-step restore with commands, the re-check for drift, verification, going live, troubleshooting, and every removed build block verbatim (appendix A). |
| [`architecture.md`](architecture.md) | How the layer works: principles, the pieces, how it ran, the ad-free hour in detail, configuration, testing, extending or replacing the provider, lessons from building it, and the compliance checklist. |
| [`testing-rules.md`](testing-rules.md) | The rules for people once ads are live: never tap your own live ads, never ask anyone to, and the publishing checklist. |

## Setup and configuration (accounts, ids and build properties)

Restoring the code and building with **test ads** needs none of this. Going live does.

**Before going live (not code, and the real lead time):**

1. **The ad account.** Check the AdMob/AdSense account standing first. A previously disabled publisher may not be able
   to open a new account, so find out before relying on ads. One account only, with 2-step verification on.
2. **A privacy policy** covering ads, and a consent setup that matches (the consent form is part of the restored code).
3. **Store declarations** drafted: whether the app contains ads, and whether it uses the advertising ID (the SDK can add
   that permission to the manifest by itself). Answer from what the shipped file contains, and check the store's
   current policy text.
4. **Three real ad ids** from the ad account: the app id, the banner unit and the rewarded unit.
5. **Test devices** registered in the ad console for everyone who will run a build.

**Which ids a build uses:**

| Build | Ids used |
|---|---|
| Debug | Google's sample app id and sample banner and rewarded units; ignores every property. |
| Release, no flag | The same sample ids: test ads only, safe to give to testers. |
| Release with `-PadmobProduction=true` | The three real ids, from Gradle properties or `local.properties`, never from the committed `gradle.properties`. The build fails at configuration if any is missing. |

- **The property names** are `admobAppId`, `admobBannerUnitId` and `admobRewardedUnitId`. They go in `local.properties`
  or `~/.gradle/gradle.properties`. Ids are public identifiers, not secrets, but real ones stay out of the repository
  so that no developer build can serve live ads.
- **Test devices:** every non-production build registers the emulator, and the developer's own device if
  `ADMOB_TEST_DEVICE_ID` is set in `local.properties`, so even a live unit shows them test ads. A debug build with that
  set also simulates the EU, so the consent form appears.
- **The live marker:** a release build whose banner unit is not the sample unit says LIVE ADS in the drawer's version
  label, so a live build is never mistaken for a test one.
- **The dependency** is added in the commercial-only form (`commercialImplementation`), so the `foss` build never lists
  an ads library.

The step-by-step commands, the removed Gradle blocks and the full test plan are in [`restore.md`](restore.md).

## Dependency injection (Koin): how ads were wired in

Shared code never knew which build it was in. It asked Koin for an `AdsController` and used the answer. The wiring:

- **One module per flavor, same name and package.** Each flavor had its own file `ads/AdsModule.kt` declaring
  `val adsModule = module { ... }`, in `src/foss` and `src/commercial`. The shared code imports `ads.adsModule`, and
  whichever flavor is being built supplies it, so there is no `if` anywhere.
  - **foss:** `single<AdsController> { NoAdsController() }`, the do-nothing implementation.
  - **commercial, the real one:**
    `single { AdFreePeriod(SharedPreferencesAdFreeStorage(androidContext())) }` and
    `single<AdsController> { AdMobAdsController(androidContext(), get()) }`. The controller takes the application context
    and the ad-free period, which Koin resolves with `get()`.
- **Loaded at start-up.** `OpenSourcePandaApplication.onCreate` calls `startKoin { androidContext(...); modules(appModule, adsModule) }`,
  so the ads module sits next to the app module. An Application is created once per process, so the container starts once.
- **Used through `by inject()`.** `BaseViewModel` is a `KoinComponent`, so the Top Repos and repo-issues ViewModels each declared
  `private val adsController: AdsController by inject()` and exposed its flows (`canShowAds`, `adFreeUntilMs`,
  `canOfferAdFree`, plus `privacyOptionsRequired` on Top Repos) to their screens. A small `AdsViewModel` held the one start call.
  `AppModule` also read `get<AdsController>().adsAreLive` to add "LIVE ADS" to the build label.
- **Tests swap it with a fake.** `ViewModelFixtures` registered `single<AdsController> { adsController }` (a `FakeAdsController`
  whose flags the test sets) in `restartKoin`, so ViewModel tests never touched a real controller.
- **Start-up.** `adsViewModel.start(this)` was a bare line in `MainActivity.onCreate`. The consent form needs an Activity, so the call
  cannot move into `Application`, but when restoring, put it inside a named app-start routine that the Activity calls (as the
  start splash now is, in `OpenSourcePandaApp`), not back as a loose line.

Restoring this wiring is step 1a of [`restore.md`](restore.md): reverting the clean-out commit brings back the `AppModule` and
`OpenSourcePandaApplication` edits, the `by inject()` fields and the test fixtures together, and step 2 brings back the real
commercial module.

## Convention: parking a feature

When a feature is taken out of the code but may come back, it is not deleted and it is not hidden in a tag alone:

1. **Tag it** so the exact code can be found (here, two tags and the removal commit).
2. **Keep a front-page README in `docs/<feature>/`**, in the mainline, that says up front that the parked material has
   value and why, where it lives, what restoring costs, and the setup and configuration it needs. A tag nobody reads
   about is forgotten; a README in the mainline is how a reader learns the capability exists.
3. **Keep the detail next to it** (how it works, the restore procedure, the rules for using it).
