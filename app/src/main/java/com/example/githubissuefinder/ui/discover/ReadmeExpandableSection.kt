package com.example.githubissuefinder.ui.discover

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.noties.markwon.Markwon

/**
 * The README, as an inline expand/collapse widget — replaces the
 * earlier v0.1 of this feature, a separate full-screen destination
 * (RepoReadmeScreen, ROUTE_REPO_README) reached via a toolbar icon.
 * That screen was always meant as a placeholder shape ("option 2, no
 * WebView") while this inline version — the originally-discussed
 * "expansion widget at the top of the view" — was deliberately
 * deferred until after the IssueCard density pass landed. With this
 * widget built, the separate screen is genuinely redundant (two access
 * points for the same content) rather than a reasonable fallback, so
 * it and its nav route were removed rather than left as dead weight
 * alongside this one.
 *
 * Still uses RepoReadmeViewModel exactly as before — that ViewModel's
 * own doc comment (loadIfNeeded's dedup guard, base64 decoding, no
 * rate-limit tracking since this hits GitHub's general REST quota, not
 * Search) is unchanged and still accurate. Only WHERE and WHEN it gets
 * called changed: loadIfNeeded now fires the first time the widget is
 * expanded, not on screen entry — no reason to spend that API call
 * before the user has asked to see the README at all.
 *
 * The 2-second expand/collapse duration (both directions) and the
 * chevron's matching 180° rotation were a specific, direct request —
 * deliberately slower than a typical Material expand/collapse (usually
 * 200-300ms), treated here as a genuine "hero" moment rather than a
 * snappy disclosure animation.
 *
 * Rendering is Markwon + a wrapped TextView (AndroidView interop),
 * same approach the removed full-screen version used — still no
 * first-party Compose API for Markwon itself.
 *
 * FULL-HEIGHT EXPAND (added October 5, 2026, per direct instruction):
 * originally capped at a fixed max height (400dp) with its own
 * internal scroll, so an uncapped README couldn't push the issue list
 * an arbitrary distance down the page below it. That cap is gone —
 * ReadmeBody/ReadmeMarkdownContent now fillMaxHeight() instead, and
 * the issue list is no longer rendered behind/below the README at all
 * while expanded (see RepoIssuesScreen's own doc comment for the
 * weight(1f)-swap that makes that true). The 400dp cap was solving a
 * problem — the README and the issue list visually blending together,
 * since the list was still right there below whatever the README
 * didn't fill — that no longer exists once the list simply isn't
 * drawn underneath it; capping the height was no longer buying
 * anything once that fix landed, just leaving dead space below a
 * short README instead.
 */
private const val EXPAND_COLLAPSE_DURATION_MS = 2000

/**
 * Split into a toggle (this file's ReadmeToggleButton) and a body
 * (ReadmeExpandableBody) deliberately, rather than one self-contained
 * composable, because the two live in DIFFERENT parents in
 * RepoIssuesScreen: the toggle sits inline in the top filter row, next
 * to the Issue Mode and Kotlin pills, while the body renders full-width
 * BELOW that entire row once expanded — "expands down and outward,"
 * not just downward from the toggle itself. `expanded` state is
 * hoisted to RepoIssuesScreen so both halves share it.
 */
@Composable
fun ReadmeToggleButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(EXPAND_COLLAPSE_DURATION_MS),
        label = "readmeChevronRotation",
    )
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onClick,
        modifier = modifier.wrapContentWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "README.md",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "⌄",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(start = 4.dp).rotate(rotation),
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
    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(top = 10.dp)) {
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.heightIn(max = 28.dp))
            }
            uiState.notFound -> CenteredReadmeMessage(
                "$repoFullName doesn't have a README at the root of its default branch.",
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
