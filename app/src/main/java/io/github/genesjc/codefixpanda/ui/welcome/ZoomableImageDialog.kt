package io.github.genesjc.codefixpanda.ui.welcome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.genesjc.codefixpanda.R

/** The most the picture can be zoomed, as a multiple of its fitted size. */
private const val MAX_ZOOM = 5f

/** What a double tap zooms to; a second double tap zooms back out. */
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * A full-screen view of a screenshot that can be pinched to zoom, dragged to move, and double-tapped to zoom in
 * and out, because the guide's screenshots are tall captures that shrink to fit the guide's panel. [onDismiss]
 * runs from the Close button and from Back.
 */
@Composable
internal fun ZoomableImageDialog(@DrawableRes image: Int, description: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var area by remember { mutableStateOf(IntSize.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f)).onSizeChanged { area = it }) {
            Image(
                painter = painterResource(image),
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            val applied = newScale / scale
                            val middle = Offset(area.width / 2f, area.height / 2f)

                            // Zoom around the fingers, then follow them, and never slide the picture off the screen.
                            offset = (offset * applied + (centroid - middle) * (1f - applied) + pan).limitedTo(area, newScale)
                            scale = newScale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = if (scale > 1.01f) 1f else DOUBLE_TAP_ZOOM
                                offset = Offset.Zero
                            },
                        )
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(8.dp),
            ) {
                Text(stringResource(R.string.action_close), color = Color.White)
            }

            Text(
                stringResource(R.string.guide_zoom_hint),
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(16.dp),
            )
        }
    }
}

/** This offset, held to how far a picture zoomed by [scale] can be moved before its edge would leave the [area]. */
private fun Offset.limitedTo(area: IntSize, scale: Float): Offset {
    val maxX = area.width * (scale - 1f) / 2f
    val maxY = area.height * (scale - 1f) / 2f

    return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
}
