package io.github.genesjc.opensourcepanda.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The open (foss) build's banner: nothing. The commercial flavor has a file with the same name,
 * package and signature that draws the ad, so the screens call it the same way in both builds, and
 * no advertising software is compiled into this one.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun AdBanner(modifier: Modifier = Modifier) = Unit
