package io.github.genesjc.codefixpanda.testutil

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module

/**
 * A ViewModel takes what it needs with `by inject()`, so a test registers
 * the objects the ViewModel under test should get (fakes, or real classes
 * over fakes), then constructs the ViewModel:
 *
 *     restartKoin { single { repository }; single { testAppConfig() } }
 *     val viewModel = IssueModeViewModel()
 *
 * Replaces any Koin left running, so a test can register again after
 * building a first ViewModel. Pair it with [StopKoinRule].
 */
fun restartKoin(registrations: ModuleDeclaration) {
    stopKoin()
    startKoin { modules(module(moduleDeclaration = registrations)) }
}

/** Stops Koin after each test, so one test's registrations never reach the next. */
class StopKoinRule : TestWatcher() {
    override fun finished(description: Description) {
        stopKoin()
    }
}
