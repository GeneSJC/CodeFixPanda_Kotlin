package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.genesjc.opensourcepanda.ui.theme.BrandOnOrange
import io.github.genesjc.opensourcepanda.ui.theme.LocalModeDecor

/**
 * The top bar of every screen but the start screen (which has the taller [BrandTitleBand]): a gradient in
 * the Issue Mode's color (the brand orange, or blue for beginner-friendly) with white text and icons. The
 * bar's own container is transparent and the gradient is drawn behind it, which also covers the status bar
 * area the bar reaches under.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrandTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val decor = LocalModeDecor.current
    TopAppBar(
        title = title,
        modifier = modifier.background(Brush.verticalGradient(listOf(decor.headerTop, decor.headerBottom))),
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            navigationIconContentColor = BrandOnOrange,
            titleContentColor = BrandOnOrange,
            actionIconContentColor = BrandOnOrange,
        ),
    )
}

/**
 * An icon button for a [BrandTopAppBar]: white, because the stock IconButton takes its color from the theme
 * and not from the bar, so it would stay dark on the gradient.
 */
@Composable
internal fun BrandBarIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(contentColor = BrandOnOrange),
        content = content,
    )
}

/**
 * A header with the same gradient and white content as [BrandTopAppBar] but a layout of its own, for a
 * screen with more to show than a title. The top row is [leading] at the start, [center] filling the
 * middle and [trailing], a column, at the end; [below] is a row of its own under them, so each widget
 * has a place and none is put wherever there is room. It reaches under the status bar like the bar does.
 */
@Composable
internal fun BrandHeader(
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    center: @Composable () -> Unit,
    trailing: @Composable ColumnScope.() -> Unit,
    below: @Composable () -> Unit = {},
) {
    val decor = LocalModeDecor.current
    CompositionLocalProvider(LocalContentColor provides BrandOnOrange) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(decor.headerTop, decor.headerBottom)))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leading()
                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) { center() }
                Column(horizontalAlignment = Alignment.End, content = trailing)
            }
            below()
        }
    }
}
