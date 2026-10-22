package io.github.genesjc.githubissuefinder.ui.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clearAndSetSemantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.githubissuefinder.R
import io.github.genesjc.githubissuefinder.ui.NEW_TOKEN_URL
import io.github.genesjc.githubissuefinder.ui.theme.GitHubIssueFinderTheme

// The launcher icon's blues, so the front door looks like the app's icon. The light end of the icon's
// gradient is left out because white text must stay readable over every part of the screen.
private val GradientDark = Color(0xFF0B1736)
private val GradientMid = Color(0xFF1D4ED8)
private val GradientLight = Color(0xFF2563EB)
private val SoftWhite = Color(0xCCFFFFFF)

/**
 * The front door: a blue gradient screen that says the app needs a GitHub token, with the ways to get one
 * and the place to paste it. "Get a token" opens GitHub's token page, "How to get a token" opens the
 * step-by-step guide, and "Try the demo" leaves with sample data. [onDone] runs once the user has left by
 * either route (token saved, or demo). Drawn in the neutral theme, since no Issue Mode is chosen yet.
 */
@Composable
fun WelcomeScreen(
    onDone: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = viewModel(),
) {
    val uriHandler = LocalUriHandler.current
    var tokenDraft by rememberSaveable { mutableStateOf("") }

    GitHubIssueFinderTheme(issueMode = null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(GradientDark, GradientMid, GradientLight),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Decorative: the title below already says what this is.
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(140.dp).clearAndSetSemantics { },
                )
                Text(
                    stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.welcome_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                )
                OutlinedButton(
                    onClick = { uriHandler.openUri(NEW_TOKEN_URL) },
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_get_token))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tokenDraft,
                    onValueChange = { tokenDraft = it },
                    label = { Text(stringResource(R.string.settings_token_label)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = SoftWhite,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = SoftWhite,
                        cursorColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { if (viewModel.saveToken(tokenDraft)) onDone() },
                    enabled = tokenDraft.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = GradientDark,
                        disabledContainerColor = SoftWhite.copy(alpha = 0.25f),
                        disabledContentColor = SoftWhite,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_save))
                }
                TextButton(onClick = onOpenGuide, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.welcome_how_to_link), color = Color.White)
                }
                TextButton(
                    onClick = {
                        viewModel.finish()
                        onDone()
                    },
                ) {
                    Text(stringResource(R.string.welcome_try_demo), color = SoftWhite)
                }
            }
        }
    }
}
