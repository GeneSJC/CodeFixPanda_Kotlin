package io.github.genesjc.opensourcepanda.domain

import io.github.genesjc.opensourcepanda.model.IssueLabelFilter

/**
 * The small decisions the Issue Mode flow makes, pulled out of the
 * ViewModels and DiscoverNavHost as plain functions so each rule lives
 * in one place and can be unit-tested without Android or Compose. Each
 * one used to be an inline expression repeated at its call sites.
 */

/** True while no mode has been chosen, meaning the app must start on the mode gate instead of Top Repos. */
fun needsModeGate(mode: IssueLabelFilter?): Boolean = mode == null

/**
 * True only when [requested] differs from the [current] mode. Switching
 * to the mode that's already active would write storage and reload the
 * whole list for nothing, so callers skip it.
 */
fun shouldSwitchMode(current: IssueLabelFilter?, requested: IssueLabelFilter): Boolean =
    requested != current
