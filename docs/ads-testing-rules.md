# Ads testing rules

Ads are an income source, and Google can suspend an account for invalid traffic, sometimes without appeal.
These rules keep that from happening.

## For everyone who tests

- Install only a test build (any debug build, or a release build made without `-PadmobProduction=true`). A test ad is labelled "Test Ad".
- Check the drawer's version label. If it says `LIVE ADS`, do not tap any ad.
- The rewarded video behind "Hide ads for 1 hour" is an ad too: on a live build, do not play it to test. Test it on a test build, where it is Google's sample video.
- Never tap an ad on a live build, even once, even "to see where it goes".
- Never ask anyone to tap, view or "support the app" through ads: not in the app, the README, the store listing, or messages. The ad-free offer is worded only as time without ads; keep it that way.
- Do not run the live build on an emulator or a farm of devices, or generate automated traffic.

## For the developer

- Debug builds always use Google's sample ids. Keep it that way.
- Put your own device's test id in `local.properties` as `ADMOB_TEST_DEVICE_ID`.
- Real ids (`admobAppId`, `admobBannerUnitId`, `admobRewardedUnitId`) go only in `local.properties` or the user-level `~/.gradle/gradle.properties` on your machine, or in CI secrets. Never in the repository, and never in its `gradle.properties`.
- Build live only with `-PadmobProduction=true`, only for release.

## Publishing checklist (before any live ad)

1. Check the AdMob/AdSense account standing first. A previously disabled publisher may not be able to open a new account; find out before relying on ads.
2. One account only, with 2-step verification on.
3. Register every tester's phone as a test device in the AdMob console.
4. Confirm the privacy policy covers ads and the consent form shows where required.
5. With the three real ids in `local.properties`, build with `-PadmobProduction=true`, confirm the label shows `LIVE ADS`, and install it on no one's phone but the store's.
6. After launch, watch the Invalid traffic report and act on any warning at once.
