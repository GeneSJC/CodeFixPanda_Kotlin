package com.example.githubissuefinder.ui.discover

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.noties.markwon.Markwon

/**
 * Renders a repo's README natively in Compose — explicitly NOT a
 * WebView, per direct instruction: a small Markdown-rendering library
 * (Markwon, which targets a plain TextView) rather than loading
 * GitHub's own rendered HTML in an embedded browser. That keeps this
 * screen's visual style consistent with the rest of the app (plain
 * MaterialTheme colors/typography, no WebView lifecycle or its own
 * independent theming to reconcile) at the cost of not pixel-matching
 * GitHub's own README styling exactly.
 *
 * AndroidView + TextView is Compose's documented escape hatch for
 * wrapping a classic Android View — Markwon has no first-party Compose
 * API of its own, only a TextView target, so this is the smallest
 * amount of View-system interop that actually gets the job done,
 * rather than reaching for a full WebView just to avoid one
 * AndroidView call.
 *
 * Deliberately no rate-limit chip here (per direct instruction) — this
 * screen's one network call (RepoReadmeViewModel.loadIfNeeded) hits
 * GitHub's general REST API quota, not the Search API quota
 * RateLimitState tracks; see GitHubApiService.getReadme's own doc
 * comment for why mixing the two into one displayed number would be
 * actively misleading rather than merely incomplete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoReadmeScreen(
    repoOwner: String,
    repoName: String,
    repoFullName: String,
    repoDisplayTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RepoReadmeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(repoFullName) {
        viewModel.loadIfNeeded(repoOwner, repoName, repoFullName)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("$repoDisplayTitle README") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            uiState.notFound -> CenteredReadmeMessage(
                text = "$repoFullName doesn't have a README at the root of its default branch.",
                modifier = Modifier.padding(padding),
            )
            uiState.errorMessage != null -> CenteredReadmeMessage(
                text = uiState.errorMessage.orEmpty(),
                modifier = Modifier.padding(padding),
            )
            uiState.markdown != null -> ReadmeContent(
                markdown = uiState.markdown.orEmpty(),
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun CenteredReadmeMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
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
private fun ReadmeContent(markdown: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val markwon = remember(context) { Markwon.create(context) }
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    Box(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
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
