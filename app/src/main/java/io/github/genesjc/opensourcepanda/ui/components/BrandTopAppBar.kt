package io.github.genesjc.opensourcepanda.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.github.genesjc.opensourcepanda.ui.theme.BrandOnOrange
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeMid
import io.github.genesjc.opensourcepanda.ui.theme.BrandOrangeTop

/**
 * The top bar of every screen but the start screen (which has the taller [BrandTitleBand]): the brand
 * orange gradient with white text and icons, so the app's identity runs through the whole app. The bar's
 * own container is transparent and the gradient is drawn behind it, which also covers the status bar
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
    TopAppBar(
        title = title,
        modifier = modifier.background(Brush.verticalGradient(listOf(BrandOrangeTop, BrandOrangeMid))),
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
