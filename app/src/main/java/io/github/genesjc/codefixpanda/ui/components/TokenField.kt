package io.github.genesjc.codefixpanda.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.github.genesjc.codefixpanda.R

/**
 * The box a GitHub token is pasted or typed into, shared by the welcome screen and Settings. The token shows
 * as dots until the user taps Show, so it is not readable over a shoulder or in a screen recording. It uses
 * the password keyboard type with auto-correct off, so the keyboard does not learn or suggest it. Whether it
 * is shown is remembered across rotation but is not saved: a new visit starts masked. [colors] and
 * [toggleColor] let the welcome screen restyle it for its gradient.
 */
@Composable
fun TokenField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    toggleColor: Color = Color.Unspecified,
) {
    var shown by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.settings_token_label)) },
        singleLine = true,
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
        ),
        trailingIcon = {
            TextButton(onClick = { shown = !shown }) {
                Text(stringResource(if (shown) R.string.token_hide else R.string.token_show), color = toggleColor)
            }
        },
        colors = colors,
        modifier = modifier,
    )
}
