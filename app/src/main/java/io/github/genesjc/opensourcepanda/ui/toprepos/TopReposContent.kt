package io.github.genesjc.opensourcepanda.ui.toprepos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.IssueLabelFilter
import io.github.genesjc.opensourcepanda.model.LanguageFilter
import io.github.genesjc.opensourcepanda.model.RateLimitInfo
import io.github.genesjc.opensourcepanda.model.RepoIssueGroup
import io.github.genesjc.opensourcepanda.ui.components.BrandBarIconButton
import io.github.genesjc.opensourcepanda.ui.components.BrandHeader
import io.github.genesjc.opensourcepanda.ui.components.BrandSoftBackground
import io.github.genesjc.opensourcepanda.ui.components.DemoWidget
import io.github.genesjc.opensourcepanda.ui.components.LanguageFilterRow
import io.github.genesjc.opensourcepanda.ui.components.ModePill
import io.github.genesjc.opensourcepanda.ui.components.RateLimitWidget

/**
 * What Top Repos draws, without owning any state: the soft backdrop, the header, and the language picker
 * with the card list under it (portrait) or beside it (landscape). TopReposScreen holds the state and the
 * dialogs and passes in what to show and what to do. The header's right side and the card list are passed
 * as content, since the screen builds them from its own state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopReposContent(
    uiState: TopReposUiState,
    visibleGroups: List<RepoIssueGroup>,
    currentMode: IssueLabelFilter,
    isLandscape: Boolean,
    snackbarHostState: SnackbarHostState,
    onMenuClick: () -> Unit,
    onRepoCountClick: () -> Unit,
    onLanguageSelected: (LanguageFilter) -> Unit,
    headerTrailing: @Composable () -> Unit,
    repoList: @Composable (Modifier) -> Unit,
) {
    BrandSoftBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                BrandHeader(
                    leading = {
                        BrandBarIconButton(onClick = onMenuClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu),
                                contentDescription = stringResource(R.string.menu_open_description),
                            )
                        }
                    },
                    // In landscape the mode and the count live in the left rail instead.
                    center = {
                        if (!isLandscape) {
                            ModeTitle(
                                mode = currentMode,
                                repoCount = visibleGroups.size,
                                lastUpdatedAtMs = uiState.lastUpdatedAtMs,
                                onRepoCountClick = onRepoCountClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    trailing = { headerTrailing() },
                )
            },
        ) { padding ->
            if (isLandscape) {
                // Landscape: a left rail (mode, count, language picker) beside the card list, so the list
                // gets the full height.
                Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                    TopReposLandscapeRail(
                        mode = currentMode,
                        repoCount = visibleGroups.size,
                        lastUpdatedAtMs = uiState.lastUpdatedAtMs,
                        selectedLanguage = uiState.selectedLanguage,
                        onCountClick = onRepoCountClick,
                        onLanguageSelected = onLanguageSelected,
                    )
                    repoList(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(modifier = Modifier.padding(padding)) {
                    LanguageFilterRow(
                        selected = uiState.selectedLanguage,
                        onSelect = onLanguageSelected,
                    )
                    repoList(Modifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * The right side of the header: the rate limit as a rounded square, always shown (a placeholder with no
 * data yet) so the header does not shift. With no token there are no rate-limit numbers, so the demo marker
 * takes its place; the screen decides what each tap does.
 */
@Composable
internal fun TopReposHeaderTrailing(
    isDemo: Boolean,
    rateLimit: RateLimitInfo?,
    onDemoClick: () -> Unit,
    onRateLimitClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(end = 8.dp),
    ) {
        if (isDemo) {
            DemoWidget(onClick = onDemoClick)
        } else {
            RateLimitWidget(info = rateLimit, onClick = onRateLimitClick)
        }
    }
}

/** Landscape's left rail: the mode, the repo count with its updated time, and the language picker. */
@Composable
private fun TopReposLandscapeRail(
    mode: IssueLabelFilter,
    repoCount: Int,
    lastUpdatedAtMs: Long?,
    selectedLanguage: LanguageFilter,
    onCountClick: () -> Unit,
    onLanguageSelected: (LanguageFilter) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ModePill(mode = mode)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            RepoCountAndUpdated(
                repoCount = repoCount,
                lastUpdatedAtMs = lastUpdatedAtMs,
                onCountClick = onCountClick,
            )
        }
        LanguageFilterRow(
            selected = selectedLanguage,
            onSelect = onLanguageSelected,
            wrap = true,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
