package io.github.genesjc.githubissuefinder.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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

/**
 * The commercial build's [AdsController]: Google's consent flow first, then the Mobile Ads SDK.
 * Nothing is requested from the ads network until the consent state allows it. The order on each
 * launch is: ask the consent service for the current state, show the consent form only where one is
 * required, and only when the service says ads may be requested start the ads SDK (off the main
 * thread) and report [canShowAds]. [privacyOptionsRequired] tells the app to offer a way to reopen the
 * user's choices. The SDK itself is not unit-tested; the shared code is tested against a fake.
 */
class AdMobAdsController(private val context: Context) : AdsController {

    private val _canShowAds = MutableStateFlow(false)
    override val canShowAds: StateFlow<Boolean> = _canShowAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** True only for a production build: a release build whose banner unit is not Google's sample unit. */
    override val adsAreLive: Boolean =
        !BuildConfig.DEBUG && BuildConfig.ADMOB_BANNER_UNIT_ID != SAMPLE_BANNER_UNIT_ID

    private val started = AtomicBoolean(false)
    private val sdkStarted = AtomicBoolean(false)

    @Volatile
    private var sdkReady = false

    override fun start(activity: Activity) {
        if (!started.compareAndSet(false, true)) return
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
        _canShowAds.value = allowed && sdkReady
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
