package io.github.genesjc.codefixpanda.ui.navigation

import io.github.genesjc.codefixpanda.model.LanguageFilter
import io.github.genesjc.codefixpanda.testutil.repoGroupOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** RepoSelectionViewModel: the repo the repo-issues screen shows, and the language Top Repos was on. */
class RepoSelectionViewModelTest {

    @Test
    fun nothingIsSelectedUntilARepoIsOpened() {
        assertNull(RepoSelectionViewModel().selection.value)
    }

    @Test
    fun openingARepoKeepsItAndTheLanguage() {
        val vm = RepoSelectionViewModel()
        val group = repoGroupOf("octo", "repo", 1, 2)

        vm.select(group, LanguageFilter.JAVA)

        assertEquals(RepoSelection(group, LanguageFilter.JAVA), vm.selection.value)
    }

    @Test
    fun openingAnotherRepoReplacesTheChoice() {
        val vm = RepoSelectionViewModel()
        vm.select(repoGroupOf("octo", "one", 1), LanguageFilter.KOTLIN)

        val second = repoGroupOf("octo", "two", 2)
        vm.select(second, LanguageFilter.ANY)

        assertEquals(RepoSelection(second, LanguageFilter.ANY), vm.selection.value)
    }
}
