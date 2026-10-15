package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.IssueLabelFilter

/**
 * The small decisions the Issue Mode flow makes, pulled out of the
 * ViewModels and DiscoverNavHost as plain functions so each rule lives
 * in one place and can be unit-tested without Android or Compose. Each
 * one used to be an inline expression repeated at its call sites.
 */

/**
 * The mode to fall back on when none has been chosen. Defensive only:
 * DiscoverNavHost never shows a screen that needs a mode while the mode
 * is null (see [needsModeGate]), so this exists to keep an unexpected
 * null from crashing rather than to be a real default.
 */
val DEFAULT_ISSUE_MODE = IssueLabelFilter.GOOD_FIRST_ISSUE

/** The mode screens and ViewModels should actually query with: the chosen one, or the defensive fallback. */
fun resolveActiveMode(mode: IssueLabelFilter?): IssueLabelFilter = mode ?: DEFAULT_ISSUE_MODE

/** True while no mode has been chosen, meaning the app must start on the mode gate instead of Top Repos. */
fun needsModeGate(mode: IssueLabelFilter?): Boolean = mode == null

/**
 * True only when [requested] differs from the [current] mode. Switching
 * to the mode that's already active would write storage and reload the
 * whole list for nothing, so callers skip it.
 */
fun shouldSwitchMode(current: IssueLabelFilter?, requested: IssueLabelFilter): Boolean =
    requested != current
