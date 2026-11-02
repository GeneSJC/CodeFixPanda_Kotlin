package io.github.genesjc.opensourcepanda.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The one coroutine scope for work that has to outlive any single screen: the state holders run
 * their work here, since a ViewModel's scope ends with its screen. A SupervisorJob keeps one failed
 * task from cancelling the others, and the immediate main dispatcher keeps state updates where the
 * screen reads them. Heavy work is not done here: repositories use their own IO dispatchers. Tests
 * do not use it; a state holder takes its scope as an argument.
 */
object AppScope {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
