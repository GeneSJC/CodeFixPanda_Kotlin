package io.github.genesjc.opensourcepanda.ui.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.opensourcepanda.R
import io.github.genesjc.opensourcepanda.model.TokenSource
import io.github.genesjc.opensourcepanda.ui.components.IndependentNotice
import io.github.genesjc.opensourcepanda.ui.components.TokenField
import io.github.genesjc.opensourcepanda.ui.theme.OpenSourcePandaTheme
import io.github.genesjc.opensourcepanda.ui.theme.BrandOnOrange
import io.github.genesjc.opensourcepanda.ui.theme.BrandOnOrangeSoft
import io.github.genesjc.opensourcepanda.ui.theme.BrandNavy

/** How much larger the front door's button and link text is than the standard button text. */
private const val ACTION_TEXT_SCALE = 1.5f

/**
 * The front door: a blue gradient screen that says the app needs a GitHub token, with the ways to get one
 * and the place to paste it. "Get a token" opens GitHub's token page, "How to get a token" opens the
 * step-by-step guide, and "Try the demo" leaves with sample data. [onTokenSaved] runs after a token is saved
 * and [onTryDemo] when the demo is chosen, separately because the caller treats them differently: after a
 * saved token there is nothing to come back to, but after the demo Back returns to this screen. The demo
 * lasts for the session only: the next launch with no token starts here again. Drawn in the
 * neutral theme, since no Issue Mode is chosen yet.
 */
@Composable
fun WelcomeScreen(
    onTokenSaved: () -> Unit,
    onTryDemo: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = viewModel(),
) {
    val uriHandler = LocalUriHandler.current
    val labelStyle = MaterialTheme.typography.labelLarge
    // The buttons and links on this screen are drawn 50% larger than the standard button text.
    val actionTextStyle = labelStyle.copy(
        fontSize = labelStyle.fontSize * ACTION_TEXT_SCALE,
        lineHeight = labelStyle.lineHeight * ACTION_TEXT_SCALE,
    )
    // Empty on a first visit; when the welcome is reopened with a token already set, the field starts with it (masked).
    var startingToken by remember { mutableStateOf(viewModel.currentToken) }
    val savedToken by viewModel.userToken.collectAsStateWithLifecycle()
    val tokenSource by viewModel.tokenSource.collectAsStateWithLifecycle()
    var tokenDraft by rememberSaveable { mutableStateOf(startingToken) }

    OpenSourcePandaTheme(issueMode = null) {
        FrontDoorBackground(modifier = modifier) {
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
                    painter = painterResource(R.drawable.panda_mark),
                    contentDescription = null,
                    modifier = Modifier.height(140.dp),
                )
                // "Welcome to" is smaller and lighter so the app's name stands out on its own.
                Text(
                    stringResource(R.string.welcome_title_intro),
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandOnOrangeSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.welcome_title_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandOnOrange,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.welcome_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandOnOrangeSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri(NEW_TOKEN_URL) },
                        border = BorderStroke(1.dp, BrandOnOrange),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandOnOrange),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_get_token), style = actionTextStyle)
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(onClick = onOpenGuide, modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.welcome_how_to_link),
                            style = actionTextStyle,
                            color = BrandOnOrange,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TokenField(
                    value = tokenDraft,
                    onValueChange = { tokenDraft = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BrandOnOrange,
                        unfocusedTextColor = BrandOnOrange,
                        focusedBorderColor = BrandOnOrange,
                        unfocusedBorderColor = BrandOnOrangeSoft,
                        focusedLabelColor = BrandOnOrange,
                        unfocusedLabelColor = BrandOnOrangeSoft,
                        cursorColor = BrandOnOrange,
                    ),
                    toggleColor = BrandOnOrange,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { if (viewModel.saveToken(tokenDraft)) onTokenSaved() },
                    enabled = tokenDraft.isNotBlank() && tokenDraft.trim() != startingToken,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandOnOrange,
                        contentColor = BrandNavy,
                        disabledContainerColor = BrandOnOrangeSoft.copy(alpha = 0.25f),
                        disabledContentColor = BrandOnOrangeSoft,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_save), style = actionTextStyle)
                }

                // Always shown, greyed out while the user has no saved token to remove. A token built into a debug
                // build is not stored by the app and cannot be removed here, which the note below says.
                TextButton(
                    onClick = {
                        viewModel.clearToken()
                        startingToken = viewModel.currentToken
                        tokenDraft = startingToken
                    },
                    enabled = savedToken.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = BrandOnOrange,
                        disabledContentColor = BrandOnOrangeSoft.copy(alpha = 0.4f),
                    ),
                ) {
                    Text(stringResource(R.string.settings_remove_token), style = actionTextStyle)
                }

                if (tokenSource == TokenSource.BUILD) {
                    Text(
                        stringResource(R.string.token_remove_unavailable_build),
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandOnOrangeSoft,
                        textAlign = TextAlign.Center,
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.tryDemo()
                        onTryDemo()
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.welcome_try_demo), style = actionTextStyle, color = BrandOnOrangeSoft)
                }

                IndependentNotice(color = BrandOnOrangeSoft, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
