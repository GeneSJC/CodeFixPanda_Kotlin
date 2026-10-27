# How the ads layer works (architecture)

This describes the ads layer as it was when it last ran, so that it can be understood, restored and extended. The
code is parked (see `docs/ads-restore.md` for where it is and how to bring it back); the SDK-free hooks described
here are still in the code. Behaviour rules for people (testing, never tapping live ads) are in
`docs/ads-testing-rules.md`.

## 1. Principles

- **Isolated.** All ad software lives in the `commercial` flavor's own source folder. Shared code knows one small
  interface, `AdsController`, and nothing about AdMob.
- **Absent from the open build.** The `foss` flavor has no ads library, no ads manifest entry and no advertising-id
  permission; a CI script proved it.
- **Consent first.** Nothing is requested from the ad network until the consent state allows it.
- **Test ads by default.** Every build used Google's sample ids unless it was a deliberate production build with all
  three real ids. Debug always used the sample ids.
- **The same shape as the rest of the app.** Koin supplies the implementation, ViewModels expose plain `StateFlow`s,
  composables only read state. Nothing but the controller touches the SDK.
- **Opt-in only.** The one rewarded video plays only after the user taps the offer, and the offer never asks anyone to
  tap or view an ad.

## 2. The pieces

| Piece | Flavor | Role |
|---|---|---|
| `ads/AdsController.kt` | main | The interface: `canShowAds`, `privacyOptionsRequired`, `adsAreLive`, `adFreeUntilMs`, `canOfferAdFree`, `start(activity)`, `showAdFreeOffer(activity)`, `showPrivacyOptions(activity)` |
| `ads/NoAdsController.kt` | main | The do-nothing implementation: every flag false or 0, every call empty |
| `ads/AdsModule.kt` | foss, commercial | Per-flavor Koin module with the same name and package; foss binds `NoAdsController`; commercial bound `AdMobAdsController` and `AdFreePeriod` |
| `ads/AdMobAdsController.kt` | commercial | The real controller: consent, SDK start, test devices, rewarded video, the flags |
| `ads/AdFreePeriod.kt` | commercial | The earned hour: start, no stacking, expiry as a Flow, saved end time, clock clamp. No SDK, so it is unit-tested |
| `ui/ads/AdBanner.kt` | foss, commercial | Same name and signature in both. Foss: empty. Commercial: an adaptive banner with its height reserved and an offline message |
| `ui/ads/AdsViewModel.kt` | main | Starts the controller once from `MainActivity`. The Activity is passed for the call only and never kept |
| `ui/ads/AdBannerSlot.kt` | main | The animated slot a screen puts in its bottom bar; reads `canShowAds` |
| `ui/ads/AdFreeOffer.kt` | main | `AdFreeOfferLine` (the title-bar offer and countdown) and `adFreeMinutesLeft` (a pure, tested function) |
| Screen ViewModels | main | Top Repos and Repo Issues expose `canShowAds`, `adFreeUntilMs`, `canOfferAdFree` and the offer call from the controller |
| Gradle and manifest | build | Flavors, sample ids, the production flag and guard, flavor build-config fields, the `${admobAppId}` placeholder, the commercial manifest entry |

## 3. How it ran

```
launch -> MainActivity -> AdsViewModel.start(activity) -> controller.start()
  consent: requestConsentInfoUpdate -> loadAndShowConsentFormIfRequired -> publish()
  publish(): allowed = consent.canRequestAds()
             if allowed: register test devices, init the SDK on IO, then sdkReady
             canShowAds     = allowed && sdkReady && !adFreeActive
             canOfferAdFree = canShowAds && a rewarded video is loaded
screens collect canShowAds -> AdBannerSlot(visible) -> AdBanner loads an ad
offer tap -> showAdFreeOffer(activity) -> video -> reward earned -> AdFreePeriod.start()
  period running -> canShowAds false -> the banner slides out
  period ends (a Flow emits false by itself) -> canShowAds true -> the banner returns
```

- **One recompute point.** Every change (consent result, SDK ready, period start or end, video loaded) calls
  `publish()`, which recomputes the flags from the current facts. No flag is flipped by hand elsewhere.
- **Threads.** The SDK starts on `Dispatchers.IO`; the rewarded video loads and shows on the main thread; state is
  thread-safe `StateFlow`. The banner pauses and resumes with the screen's lifecycle and is destroyed when it leaves.
- **Consent.** The consent form is shown only where the consent service says one is required. Where required, the Top
  Repos menu offers a way to reopen the privacy choices (`privacyOptionsRequired`, `showPrivacyOptions`). A debug build
  with a test device id set simulates the EU so the form can be seen anywhere.

## 4. The ad-free hour in detail

**What the user sees.** A small line, "Hide ads for 1 hour", appears once a rewarded video is ready: right-aligned under
the chip row on Top Repos, under the title on Repo Issues, plus a backup menu entry on Top Repos. Tapping plays a full-screen
rewarded video (Google's sample video in every non-production build). Closing it early earns nothing; finishing it starts the
hour. During the hour the banner slides out, no new video is requested, and the line reads "Ad-free: N min" (rounded up, redrawn
every 15 seconds). At the end the banner slides back and the offer returns once the next video has loaded.

**States.**

| State | When | What the user gets |
|---|---|---|
| Unavailable | No consent yet, SDK not ready, no video loaded, or offline | No line; the banner follows `canShowAds` |
| Offerable | Ads are open and a video is loaded | The offer line |
| Running | A period is active (now is before its saved end) | Banner hidden, countdown shown, no video requested |
| Ended | The end time has passed | Banner returns; the next video loads; back to Offerable |

**`AdFreePeriod`.** Members: `untilMs` (StateFlow), `isActive()`, `active` (a Flow that emits true until the end, then false by
itself, using a delay), and `start()`, which returns false and changes nothing while a period is running. The clock and the duration
are constructor parameters so tests control them. The duration is one constant, `AD_FREE_DURATION_MS` (one hour); keep the words in
the `action_hide_ads` string ("1 hour") in step with it.

**Storage.** One saved number, the end time in epoch milliseconds, in a private preferences file (`ad_free_prefs`) that is excluded
from backups and device transfer. It survives closing the app. A saved end more than one period ahead of now is cut back to one
period, which caps clock tampering; setting the clock forward only ends a period early.

**Controller interplay.** The controller starts with the saved state (`adFreeActive` from the period), so no banner flashes up
mid-period. `start()` collects the period's `active` flow, and each emission calls `publish()`. `adFreeUntilMs` is the period's `untilMs`,
exposed for the countdown.

**Playing the video.** `showAdFreeOffer(activity)` takes the loaded video (single-use), installs its dismiss and failure callbacks,
shows it, and starts the period only in the reward callback. Dismissal or failure calls `publish()`, which loads the next video if ads
are open and no period is running.

**Loading.** On the main thread, only when ads are open, none is loaded or loading, and the retry wait is over. A failure waits 60
seconds, then re-publishes. A loaded video expires after about an hour; showing an expired one fails, which reloads.

**The UI.** `AdFreeOfferLine` takes the end time, whether an offer can be made, and the tap callback, and draws the offer, the
countdown or nothing. `AdBannerSlot` wraps the banner in animated visibility on `canShowAds`, so the list's bottom edge eases
instead of jumping.

**Edge cases.**

| Case | What happens |
|---|---|
| Offline | No video loads, so no offer. The banner's reserved strip shows plain muted "You're offline" |
| Video closed early | No reward, no period; the next video loads |
| App closed mid-period | The saved end time restores the period on the next launch; the banner stays hidden until it ends |
| Clock set back | A saved end more than one period ahead is cut back to one period |
| Consent withdrawn | `canShowAds` and `canOfferAdFree` go false whatever the period says |
| Expired or failed video | Showing fails, `publish()` runs, the next video loads |

## 5. Configuration

| Build | Ids used |
|---|---|
| Debug | Google's sample app id and sample banner and rewarded units; ignores every property |
| Release, no flag | The same sample ids: test ads only, safe to give to testers |
| Release with `-PadmobProduction=true` | The three real ids (`admobAppId`, `admobBannerUnitId`, `admobRewardedUnitId`) from a Gradle property or `local.properties`, never from the committed `gradle.properties`. The build fails at configuration if any is missing |

- `adsAreLive` is true only for a release build whose banner unit is not the sample unit; it puts LIVE ADS in the drawer's version label so a
  live build is never mistaken for a test one.
- Every non-production build registers the emulator and (if set) the developer's own device as test devices (`ADMOB_TEST_DEVICE_ID` in
  `local.properties`), so even a live unit shows them test ads.
- The AdMob app id reaches the commercial manifest through the `${admobAppId}` placeholder. Ids are public identifiers, not secrets, but real
  ones stay out of the repository so no developer build can serve live ads.
- Dependencies were added only in the commercial-only form (`commercialImplementation`), with the string form because the typed accessor is
  not generated when flavors are declared in the same script.

## 6. Testing

| What | How |
|---|---|
| Shared code | `FakeAdsController` drives the ViewModel tests: the flags follow the controller |
| `AdFreePeriod` | `src/testCommercial`: start, no stacking, expiry, survives a restart, clamp, the automatic end under virtual time |
| The minutes-left rounding | `AdFreeMinutesLeftTest` (shared) |
| The do-nothing controller | `NoAdsControllerTest` (shared): never allows an ad, never offers one, never asks for privacy options |
| The open build | `scripts/check-foss-clean.sh` in CI: no ads library on the classpath, no AdMob or advertising-id entry in the merged manifest, with a positive control on the commercial build |
| The SDK | Not unit-tested. Checked by hand on a device: the banner is labelled Test Ad, the offer plays a sample video, the banner returns after the hour, the offline text shows with the network off |
| CI | Names both flavors (`testFossDebugUnitTest testCommercialDebugUnitTest`) |

## 7. Extending and replacing

**Add another ad format (for example an interstitial).**

1. Add what shared code needs to `AdsController` as state or a call, with a constant answer in `NoAdsController` and the fake.
2. Implement it in `AdMobAdsController` (or a helper in `src/commercial`), recomputing through `publish()`. Use a sample unit in
   non-production builds and a new `admob...UnitId` property for production, added to the production guard.
3. Expose it through the screen ViewModels and use it from the composables; no SDK type crosses into main.
4. Word it as time or benefit, never as a request to tap or view an ad. Add it to `docs/ads-testing-rules.md`.
5. Run the clean-build check and both flavors' tests.

**Replace the provider, or remove ads.** Re-implement `AdsController` and `AdBanner` in `src/commercial` for the new provider, change the
commercial `adsModule`, swap the dependency, and update the clean-build script's list of forbidden libraries. No shared screen or ViewModel
changes: that is the test that the isolation still holds. To remove ads, bind `NoAdsController` in both flavors, which is what parking did.

## 8. Lessons from building it

- Verify SDK class names against the build: they were written from memory and a build is the only check available outside the IDE.
- Keep the banner's height reserved. A banner that appears under a thumb causes accidental taps, which count as invalid traffic.
- Hide ads by state, not by deleting code paths: the banner slot reads `canShowAds`, so consent, the ad-free hour and the open build all
  use one mechanism.
- Name both flavors in every Gradle task, in CI and in docs: `testDebugUnitTest` and `assembleDebug` are ambiguous with two flavors.
- Never let a developer's token or real ad ids into a release build by default: the safe state is the default, and a production build has
  to be asked for.

## 9. Compliance checklist (for when ads are live)

- No AdMob or UMP import, id or SDK call outside `src/commercial`.
- Shared code reaches ads only through `AdsController`, `AdBannerSlot` and `AdFreeOfferLine`.
- The clean-build check passes with its positive control, and both flavors' tests pass.
- Every flag is computed in one place (`publish()`), from current facts.
- Debug uses sample ids; no real id is in the repository; a production build without all three ids fails.
- The SDK starts only after consent allows it, and off the main thread.
- The ad-free offer is opt-in, worded only as time without ads, and the period starts only when the reward is earned.
- The banner's height is reserved, and a network failure shows plain muted text.
- The publishing checklist in `docs/ads-testing-rules.md` is complete before any live release.
