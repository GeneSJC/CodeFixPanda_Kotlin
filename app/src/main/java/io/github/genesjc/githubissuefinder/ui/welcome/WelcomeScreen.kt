package io.github.genesjc.githubissuefinder.ui.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.ui.NEW_TOKEN_URL
import io.github.genesjc.githubissuefinder.ui.theme.GitHubIssueFinderTheme
import kotlinx.coroutines.launch

/**
 * The front door: a short slideshow that explains what the app does and walks a new user through getting a
 * GitHub token, ending with a place to paste it. "Try the demo" is always there and leaves with sample
 * data. [onDone] runs once the user has left by either route (token saved, or demo). Back steps to the
 * previous slide. Drawn in the neutral theme, since no Issue Mode is chosen yet.
 */
@Composable
fun WelcomeScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = viewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { welcomeSlides.size })
    val coroutineScope = rememberCoroutineScope()
    val lastPage = welcomeSlides.lastIndex
    var tokenDraft by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    GitHubIssueFinderTheme(issueMode = null) {
        Scaffold(
            modifier = modifier.imePadding(),
            bottomBar = {
                WelcomeBottomBar(
                    onTryDemo = {
                        viewModel.finish()
                        onDone()
                    },
                    isLastPage = pagerState.currentPage == lastPage,
                    onNext = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    canSave = tokenDraft.isNotBlank(),
                    onSave = { if (viewModel.saveToken(tokenDraft)) onDone() },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    WelcomeSlidePage(
                        slide = welcomeSlides[page],
                        tokenEntry = if (page == lastPage) {
                            { TokenEntry(draft = tokenDraft, onDraftChange = { tokenDraft = it }) }
                        } else {
                            null
                        },
                    )
                }
                PageDots(count = welcomeSlides.size, current = pagerState.currentPage)
            }
        }
    }
}

/** One slide: the glyph, the title, the screenshot slot, the explanation, and on the last slide the token entry. */
@Composable
private fun WelcomeSlidePage(slide: WelcomeSlide, tokenEntry: (@Composable () -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(slide.glyph, style = MaterialTheme.typography.displayMedium)
        Text(
            stringResource(slide.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        ScreenshotSlot(stringResource(slide.screenshotCaption))
        Text(
            stringResource(slide.body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        tokenEntry?.invoke()
    }
}

/** Where a real screenshot goes; until one is supplied it shows its caption as text. */
@Composable
private fun ScreenshotSlot(caption: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** The last slide's extra: a button to GitHub's token page, and the box to paste the token into. */
@Composable
private fun TokenEntry(draft: String, onDraftChange: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        OutlinedButton(onClick = { uriHandler.openUri(NEW_TOKEN_URL) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_get_token))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            label = { Text(stringResource(R.string.settings_token_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The row of dots under the pager. It is one element for screen readers: "Page 2 of 5". */
@Composable
private fun PageDots(count: Int, current: Int) {
    val description = stringResource(R.string.welcome_page_description, current + 1, count)
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp).semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

/** "Try the demo" on the left, always; "Next" on the left slides and "Save" on the last one, on the right. */
@Composable
private fun WelcomeBottomBar(
    onTryDemo: () -> Unit,
    isLastPage: Boolean,
    onNext: () -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onTryDemo) { Text(stringResource(R.string.welcome_try_demo)) }
        if (isLastPage) {
            Button(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.action_save)) }
        } else {
            Button(onClick = onNext) { Text(stringResource(R.string.welcome_next)) }
        }
    }
}
