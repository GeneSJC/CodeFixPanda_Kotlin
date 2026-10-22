package io.github.genesjc.githubissuefinder.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import io.github.genesjc.githubissuefinder.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Google's published sample banner unit: shows test ads only. Anything else is treated as a live unit. */
private const val SAMPLE_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

/** Google's published sample rewarded-video unit: shows test videos only. Debug builds always use it. */
private const val SAMPLE_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

/** How long to wait before asking for a rewarded video again after a failed request. */
private const val RETRY_AFTER_FAILED_LOAD_MS = 60_000L

/**
 * The commercial build's [AdsController]: Google's consent flow first, then the Mobile Ads SDK.
 * Nothing is requested from the ads network until the consent state allows it. The order on each
 * launch is: ask the consent service for the current state, show the consent form only where one is
 * required, and only when the service says ads may be requested start the ads SDK (off the main
 * thread) and report [canShowAds]. [privacyOptionsRequired] tells the app to offer a way to reopen the
 * user's choices. It also holds one preloaded rewarded video: while one is ready the user can be offered
 * the ad-free period, and finishing it starts the period (see [AdFreePeriod]). The SDK itself is not
 * unit-tested; the shared code is tested against a fake.
 */
class AdMobAdsController(private val context: Context, private val adFree: AdFreePeriod) : AdsController {

    private val _canShowAds = MutableStateFlow(false)
    override val canShowAds: StateFlow<Boolean> = _canShowAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** True only for a production build: a release build whose banner unit is not Google's sample unit. */
    override val adsAreLive: Boolean =
        !BuildConfig.DEBUG && BuildConfig.ADMOB_BANNER_UNIT_ID != SAMPLE_BANNER_UNIT_ID

    override val adFreeUntilMs: StateFlow<Long> = adFree.untilMs

    private val _canOfferAdFree = MutableStateFlow(false)
    override val canOfferAdFree: StateFlow<Boolean> = _canOfferAdFree.asStateFlow()

    private val started = AtomicBoolean(false)
    private val sdkStarted = AtomicBoolean(false)

    @Volatile
    private var sdkReady = false

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false
    private var nextRewardedLoadAtMs = 0L

    private val rewardedUnitId: String =
        if (BuildConfig.DEBUG) SAMPLE_REWARDED_UNIT_ID else BuildConfig.ADMOB_REWARDED_UNIT_ID

    // Starts from the saved state so a banner never flashes up in the middle of a period.
    @Volatile
    private var adFreeActive = adFree.isActive()

    override fun start(activity: Activity) {
        if (!started.compareAndSet(false, true)) return
        // Hides the banner when a period starts and brings it back by itself when the period ends.
        CoroutineScope(Dispatchers.Default).launch {
            adFree.active.collect {
                adFreeActive = it
                publish()
            }
        }
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            consentRequestParameters(activity),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> publish() }
            },
            { _ -> publish() },
        )
        // A previous launch's consent may already allow ads, so the first banner need not wait for the update.
        publish()
    }

    /**
     * Plays the preloaded rewarded video. The video is single-use, so it is dropped first; the next one
     * loads once this one closes. The period starts only when the user has earned the reward by finishing.
     */
    override fun showAdFreeOffer(activity: Activity) {
        val ad = rewardedAd ?: return
        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = publish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = publish()
        }
        ad.show(activity) { adFree.start() }
        publish()
    }

    /** Asks for a rewarded video when ads are allowed, none is loaded or loading, and the retry wait is over. Main thread only. */
    private fun loadRewardedIfNeeded() {
        if (rewardedAd != null || rewardedLoading) return
        if (!_canShowAds.value) return
        if (SystemClock.elapsedRealtime() < nextRewardedLoadAtMs) return
        rewardedLoading = true
        RewardedAd.load(
            context,
            rewardedUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewardedAd = ad
                    publish()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    nextRewardedLoadAtMs = SystemClock.elapsedRealtime() + RETRY_AFTER_FAILED_LOAD_MS
                    mainHandler.postDelayed(::publish, RETRY_AFTER_FAILED_LOAD_MS)
                }
            },
        )
    }

    override fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> publish() }
    }

    /** Re-reads the consent state, starts the SDK once ads are allowed, and reports both facts. */
    private fun publish() {
        val consent = UserMessagingPlatform.getConsentInformation(context)
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        val allowed = consent.canRequestAds()
        if (allowed) startSdkOnce()
        val adsOpen = allowed && sdkReady && !adFreeActive
        _canShowAds.value = adsOpen
        _canOfferAdFree.value = adsOpen && rewardedAd != null
        if (adsOpen) mainHandler.post(::loadRewardedIfNeeded)
    }

    /** The SDK is started off the main thread, as Google advises, and only after consent allows ads. */
    private fun startSdkOnce() {
        if (!sdkStarted.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            registerTestDevices()
            MobileAds.initialize(context) {
                sdkReady = true
                publish()
            }
        }
    }

    /**
     * In every build that is not a production build, registers the emulator and (if set) your own device as
     * test devices, so even a live ad unit serves them test ads. This is the safeguard against ever clicking a
     * live ad of your own: a registered device shows a "Test Ad" label and its traffic never reaches your
     * account. A production build registers none, so register every tester's phone in the AdMob console instead.
     */
    private fun registerTestDevices() {
        if (adsAreLive) return
        val ids = buildList {
            add(AdRequest.DEVICE_ID_EMULATOR)
            if (BuildConfig.ADMOB_TEST_DEVICE_ID.isNotBlank()) add(BuildConfig.ADMOB_TEST_DEVICE_ID)
        }
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(ids).build())
    }

    /**
     * In a debug build with a test device id set (ADMOB_TEST_DEVICE_ID in local.properties), pretends to be
     * in the EU so the consent form can be seen anywhere. Release builds never use debug settings.
     */
    private fun consentRequestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG && BuildConfig.ADMOB_TEST_DEVICE_ID.isNotBlank()) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(BuildConfig.ADMOB_TEST_DEVICE_ID)
                    .build(),
            )
        }
        return builder.build()
    }
}
