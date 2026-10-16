package com.example.githubissuefinder.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The one coroutine scope for work that has to outlive any single
 * screen: the services that hold app-level data state run their work
 * here. A ViewModel's scope ends with its screen, so a service cannot
 * use it; sharing one scope means there is a single app-level lifetime
 * to reason about, however many services there are.
 *
 * A SupervisorJob so one failed task does not cancel the others, and the
 * main dispatcher so state updates happen where the screen reads them
 * (immediate, so a call already on the main thread runs at once). Heavy
 * work is not done here: file and network work runs on the repositories'
 * own IO dispatchers.
 *
 * This is a plain object, not a scope created in an Application class,
 * because this app has no Application subclass and initializes its
 * repositories the same way (see MainActivity.onCreate). Tests do not
 * use it: a service takes its scope as an argument, and tests pass their
 * own.
 */
object AppScope {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
