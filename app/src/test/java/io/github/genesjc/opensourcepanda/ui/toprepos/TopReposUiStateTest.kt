package io.github.genesjc.opensourcepanda.ui.toprepos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopReposUiStateTest {

    @Test
    fun needsATokenFixWhenGitHubRejectedTheToken() {
        assertTrue(TopReposUiState(tokenRejected = true).needsTokenFix)
    }

    @Test
    fun doesNotNeedATokenFixOtherwise() {
        assertFalse(TopReposUiState().needsTokenFix)
        assertFalse(TopReposUiState(errorMessage = "network down").needsTokenFix)
    }
}
