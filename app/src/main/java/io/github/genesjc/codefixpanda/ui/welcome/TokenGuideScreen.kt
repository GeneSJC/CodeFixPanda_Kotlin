package io.github.genesjc.codefixpanda.ui.welcome

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.ui.components.IndependentNotice
import io.github.genesjc.codefixpanda.ui.theme.CodeFixPandaTheme
import io.github.genesjc.codefixpanda.ui.theme.BrandSun
import kotlinx.coroutines.launch

/** The gap between the panel and the screen edges. */
private val PANEL_MARGIN = 15.dp

/** How round the panel's corners are. */
private val PANEL_CORNER_RADIUS = 32.dp

/**
 * The step-by-step guide to getting a GitHub token: four short steps, one per screen: the page we send you to,
 * then screenshots of GitHub's own pages. It sits in a rounded, bordered white panel, inset from the screen edges,
 * on the same blue gradient as the welcome screen. Reached from the welcome screen's link and its menu entry.
 * [onBack] runs from the Close button on any step, and from Back on the first one; Back on a later step goes
 * to the previous step. Drawn in the neutral theme, since no Issue Mode is chosen yet.
 */
@Composable
fun TokenGuideScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { tokenGuideSteps.size })
    val coroutineScope = rememberCoroutineScope()
    val lastPage = tokenGuideSteps.lastIndex

    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    CodeFixPandaTheme(issueMode = null) {
        FrontDoorBackground(modifier = modifier) {
            Surface(
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(PANEL_MARGIN),
                shape = RoundedCornerShape(PANEL_CORNER_RADIUS),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(3.dp, BrandSun),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        stringResource(R.string.token_help_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                    )
                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                        TokenGuideStepPage(tokenGuideSteps[page])
                    }

                    PageDots(count = tokenGuideSteps.size, current = pagerState.currentPage)
                    IndependentNotice(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.action_close)) }
                        if (pagerState.currentPage == lastPage) {
                            Button(onClick = onBack) { Text(stringResource(R.string.guide_done)) }
                        } else {
                            Button(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                                Text(stringResource(R.string.welcome_next))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One step: the glyph, the title, a reminder if it has one, the picture, the explanation, and where needed a button to GitHub's token page. */
@Composable
private fun TokenGuideStepPage(step: TokenGuideStep) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(step.glyph, style = MaterialTheme.typography.displayMedium)
        Text(
            stringResource(step.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        step.emphasis?.let { EmphasisNote(stringResource(it)) }
        if (step.showsLink) {
            LinkCard(NEW_TOKEN_DISPLAY)
        }

        if (step.screenshot != null && step.screenshotDescription != null) {
            GuideScreenshot(step.screenshot, stringResource(step.screenshotDescription))
        }

        Text(
            stringResource(step.body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (step.showGetToken) {
            OutlinedButton(
                onClick = { uriHandler.openUri(NEW_TOKEN_URL) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.action_get_token))
            }
        }
    }
}

/** A highlighted reminder, in the brand's yellow, for the one thing the user must not get wrong. */
@Composable
private fun EmphasisNote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = BrandSun.copy(alpha = 0.28f),
        border = BorderStroke(2.dp, BrandSun),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** The address the user is about to be sent to, in a box so they can see where the button goes. */
@Composable
private fun LinkCard(address: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            address,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** A screenshot of GitHub's own page, full width with its natural height, so the page scrolls instead of squashing it. */
@Composable
private fun GuideScreenshot(@DrawableRes image: Int, description: String) {
    Image(
        painter = painterResource(image),
        contentDescription = description,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
    )
}

/** The row of dots under the pager. It is one element for screen readers: "Page 2 of 4". */
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
