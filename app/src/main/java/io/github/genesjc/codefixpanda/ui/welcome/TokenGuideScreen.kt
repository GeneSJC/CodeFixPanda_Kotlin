package io.github.genesjc.codefixpanda.ui.welcome

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.ui.components.BrandSoftBackground
import io.github.genesjc.codefixpanda.ui.components.IndependentNotice
import io.github.genesjc.codefixpanda.ui.theme.CodeFixPandaTheme
import io.github.genesjc.codefixpanda.ui.theme.BrandBlue
import io.github.genesjc.codefixpanda.ui.theme.BrandSun
import kotlinx.coroutines.launch

/** The gap between the panel and the screen edges. */
private val PANEL_MARGIN = 15.dp

/** How round the panel's corners are. */
private val PANEL_CORNER_RADIUS = 32.dp

/**
 * The step-by-step guide to getting a GitHub token: four numbered steps, one per screen, each showing a
 * screenshot: GitHub's own pages (the first also shows the page's address), then this app's welcome screen.
 * It sits in a rounded, bordered panel inset from the screen edges, on the soft glyph backdrop the issue-type
 * screen uses, over the same orange background as the welcome screen. Reached from the welcome screen's link
 * and its menu entry. [onBack] runs from the Close button on any step, and from Back on the first one; Back
 * on a later step goes to the previous step. Drawn in the neutral theme, since no Issue Mode is chosen yet.
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
                color = Color.Transparent,
                border = BorderStroke(3.dp, BrandSun),
            ) {
                BrandSoftBackground {
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
}

/** One step: the glyph, the title, the link if it has one, a reminder if it has one, the picture, and the explanation. */
@Composable
private fun TokenGuideStepPage(step: TokenGuideStep) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(step.glyph, style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(step.title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f, fill = false),
            )
        }

        if (step.showsLink) {
            TokenPageLink(NEW_TOKEN_DISPLAY)
        }

        step.emphasis?.let { EmphasisNote(stringResource(it)) }
        GuideScreenshot(step.screenshot, stringResource(step.screenshotDescription), step.screenshotWidthFraction)
        Text(
            stringResource(step.body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
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

/**
 * The address of GitHub's token page, as a blue link. Tapping it only explains where the real button is: the guide
 * is for reading, and the welcome screen's Get a token button is what opens the page.
 */
@Composable
private fun TokenPageLink(address: String) {
    val context = LocalContext.current
    val explanation = stringResource(R.string.guide_link_toast)

    Text(
        address,
        style = MaterialTheme.typography.titleMedium,
        color = BrandBlue,
        textDecoration = TextDecoration.Underline,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(bottom = 16.dp)
            .clickable { Toast.makeText(context, explanation, Toast.LENGTH_SHORT).show() },
    )
}

/** A screenshot at its natural height, [widthFraction] of the width, so the page scrolls instead of squashing it. */
@Composable
private fun GuideScreenshot(@DrawableRes image: Int, description: String, widthFraction: Float) {
    Image(
        painter = painterResource(image),
        contentDescription = description,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.fillMaxWidth(widthFraction).clip(RoundedCornerShape(12.dp)),
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
