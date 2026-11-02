package io.github.genesjc.opensourcepanda.ui

import io.github.genesjc.opensourcepanda.config.AppConfig
import io.github.genesjc.opensourcepanda.testutil.FakeStringResolver
import io.github.genesjc.opensourcepanda.testutil.StopKoinRule
import io.github.genesjc.opensourcepanda.testutil.restartKoin
import io.github.genesjc.opensourcepanda.ui.text.StringResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** BaseViewModel over a config: each getter forwards the matching AppConfig value. */
class BaseViewModelTest {

    @get:Rule
    val stopKoin = StopKoinRule()

    private class TestViewModel : BaseViewModel() {
        fun text(id: Int, vararg args: Any) = string(id, *args)
    }

    private fun viewModel(config: AppConfig): TestViewModel {
        restartKoin {
            single { config }
            single<StringResolver> { FakeStringResolver }
        }
        return TestViewModel()
    }

    @Test
    fun gettersForwardTheConfigValues() {
        val vm = viewModel(AppConfig(buildLabel = "24.1", splashAnimation = "origami", showDetailedDrawerMenu = true))

        assertEquals("24.1", vm.buildLabel)
        assertEquals("origami", vm.splashAnimation)
        assertTrue(vm.showDetailedDrawerMenu)
    }

    @Test
    fun theDrawerFlagCanBeOff() {
        val vm = viewModel(AppConfig(buildLabel = "1.0", splashAnimation = "coin", showDetailedDrawerMenu = false))

        assertFalse(vm.showDetailedDrawerMenu)
    }

    @Test
    fun stringResolvesThroughTheRegisteredResolver() {
        val vm = viewModel(AppConfig(buildLabel = "1.0", splashAnimation = "coin", showDetailedDrawerMenu = false))

        assertEquals(FakeStringResolver(7, "a", 2), vm.text(7, "a", 2))
    }
}
