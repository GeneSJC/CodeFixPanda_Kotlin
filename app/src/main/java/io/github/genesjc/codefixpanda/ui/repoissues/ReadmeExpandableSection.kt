package io.github.genesjc.codefixpanda.ui.repoissues

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.ui.theme.LocalModeDecor
import io.noties.markwon.Markwon

/**
 * The README as an inline expand/collapse widget. It uses RepoReadmeViewModel, which loads the first time the widget is
 * expanded and not on screen entry, so no API call is spent before the user asks. Rendered by
 * Markwon in a wrapped TextView (AndroidView), since Markwon has no first-party Compose API. The
 * 2-second expand/collapse and the matching chevron rotation are a deliberate slow "hero" moment,
 * not the usual 200 to 300 ms. The body fills the available height instead of being capped, because
 * it is drawn as an overlay (see the repo issues screen in _meta/docs/design-notes.md).
 */
private const val EXPAND_COLLAPSE_DURATION_MS = 2000

/**
 * Split into a toggle and a body because they live in different parents in RepoIssuesScreen: the
 * toggle sits inline in the filter row, and the body renders full-width below that row. `expanded`
 * is hoisted to the screen so both halves share it.
 */
@Composable
fun ReadmeToggleButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val decor = LocalModeDecor.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(EXPAND_COLLAPSE_DURATION_MS),
        label = "readmeChevronRotation",
    )
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = decor.chipFill,
        border = BorderStroke(1.dp, decor.chipOutline),
        onClick = onClick,
        modifier = modifier.wrapContentWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_document),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(R.string.readme_toggle_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 6.dp),
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                modifier = Modifier.padding(start = 4.dp).size(16.dp).rotate(rotation),
            )
        }
    }
}

@Composable
fun ReadmeExpandableBody(
    expanded: Boolean,
    repoOwner: String,
    repoName: String,
    repoFullName: String,
    modifier: Modifier = Modifier,
    viewModel: RepoReadmeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(expanded, repoFullName) {
        if (expanded) {
            viewModel.loadIfNeeded(repoOwner, repoName, repoFullName)
        }
    }

    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = expandVertically(animationSpec = tween(EXPAND_COLLAPSE_DURATION_MS)) +
            fadeIn(animationSpec = tween(EXPAND_COLLAPSE_DURATION_MS)),
        exit = shrinkVertically(animationSpec = tween(EXPAND_COLLAPSE_DURATION_MS)) +
            fadeOut(animationSpec = tween(EXPAND_COLLAPSE_DURATION_MS)),
    ) {
        ReadmeBody(uiState = uiState, repoFullName = repoFullName)
    }
}

@Composable
private fun ReadmeBody(uiState: RepoReadmeUiState, repoFullName: String) {
    // Opaque, not just fillMaxHeight: this is an overlay on the issue list (see RepoIssuesScreen), so
    // whatever part is visible mid-animation must fully hide the list underneath.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 10.dp),
    ) {
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.heightIn(max = 28.dp))
            }
            uiState.notFound -> CenteredReadmeMessage(
                stringResource(R.string.readme_not_found, repoFullName),
            )
            uiState.errorMessage != null -> CenteredReadmeMessage(uiState.errorMessage.orEmpty())
            uiState.markdown != null -> ReadmeMarkdownContent(uiState.markdown)
        }
    }
}

@Composable
private fun CenteredReadmeMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReadmeMarkdownContent(markdown: String) {
    val context = LocalContext.current
    val markwon = remember(context) { Markwon.create(context) }
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        AndroidView(
            factory = { factoryContext ->
                TextView(factoryContext).apply {
                    movementMethod = LinkMovementMethod.getInstance()
                }
            },
            update = { textView ->
                textView.setTextColor(textColor)
                textView.setLinkTextColor(linkColor)
                markwon.setMarkdown(textView, markdown)
            },
        )
    }
}
