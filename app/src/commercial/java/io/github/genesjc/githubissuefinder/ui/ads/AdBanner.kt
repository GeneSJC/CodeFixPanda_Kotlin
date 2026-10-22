package io.github.genesjc.githubissuefinder.ui.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import io.github.genesjc.githubissuefinder.BuildConfig
import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.ui.findActivity

/** Google's published sample unit for an adaptive banner: shows test ads and can never earn or be penalized. */
private const val SAMPLE_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

/**
 * The commercial build's banner: an adaptive banner as wide as its slot. Its height is reserved from
 * the ad size as soon as the width is known, so the layout does not jump when the ad loads (and a
 * failed load leaves a quiet bar and not a gap that opens later). Debug builds always use Google's
 * sample unit; release builds use the unit from the build config. The ad pauses and resumes with the
 * screen and is destroyed when it leaves. When a load fails because there is no network, the reserved bar
 * says so in plain, muted text until an ad loads (the ads software keeps retrying). Callers show it only when the consent state allows
 * (canShowAds). The open flavor has an empty composable with this name and signature.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        var offline by remember(adSize) { mutableStateOf(false) }
        val adView = remember(adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        offline = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        offline = error.code == AdRequest.ERROR_CODE_NETWORK_ERROR
                    }
                }
                adUnitId = if (BuildConfig.DEBUG) SAMPLE_BANNER_UNIT_ID else BuildConfig.ADMOB_BANNER_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
        val owner = context.findActivity() as? LifecycleOwner
        DisposableEffect(adView, owner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    else -> Unit
                }
            }
            owner?.lifecycle?.addObserver(observer)
            onDispose {
                owner?.lifecycle?.removeObserver(observer)
                adView.destroy()
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(adSize.height.dp), contentAlignment = Alignment.Center) {
            AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(adSize.height.dp))
            if (offline) {
                Text(
                    stringResource(R.string.ad_banner_offline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
