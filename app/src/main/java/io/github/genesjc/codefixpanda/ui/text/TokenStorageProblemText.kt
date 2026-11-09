package io.github.genesjc.codefixpanda.ui.text

import androidx.annotation.StringRes
import io.github.genesjc.codefixpanda.R
import io.github.genesjc.codefixpanda.model.TokenStorageProblem

/** The wording for a token storage problem, as a string resource id. The `when` is exhaustive, so a new problem needs its text. */
@StringRes
fun TokenStorageProblem.messageRes(): Int = when (this) {
    TokenStorageProblem.COULD_NOT_READ -> R.string.token_problem_could_not_read
    TokenStorageProblem.COULD_NOT_SAVE -> R.string.token_problem_could_not_save
}
