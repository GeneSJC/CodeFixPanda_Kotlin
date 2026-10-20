package io.github.genesjc.githubissuefinder.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.genesjc.githubissuefinder.BuildConfig
import io.github.genesjc.githubissuefinder.data.ApiTokenRepository
import io.github.genesjc.githubissuefinder.data.AppConfig
import io.github.genesjc.githubissuefinder.data.BookmarksRepository
import io.github.genesjc.githubissuefinder.data.GitHubIssueSearchApi
import io.github.genesjc.githubissuefinder.data.GitHubReadmeApi
import io.github.genesjc.githubissuefinder.data.GitHubReadmeSource
import io.github.genesjc.githubissuefinder.data.HiddenIssuesRepository
import io.github.genesjc.githubissuefinder.data.IssueModeRepository
import io.github.genesjc.githubissuefinder.data.IssueSearchRepository
import io.github.genesjc.githubissuefinder.data.JsonFileBookmarksStorage
import io.github.genesjc.githubissuefinder.data.JsonFileHiddenIssuesStorage
import io.github.genesjc.githubissuefinder.data.JsonFileRepoIssueGroupStorage
import io.github.genesjc.githubissuefinder.data.ReadmeRepository
import io.github.genesjc.githubissuefinder.data.ReadmeSnippetRepository
import io.github.genesjc.githubissuefinder.data.RepoIssueGroupRepository
import io.github.genesjc.githubissuefinder.data.RepoIssuesStateHolder
import io.github.genesjc.githubissuefinder.data.SharedPreferencesApiTokenStorage
import io.github.genesjc.githubissuefinder.data.SharedPreferencesIssueModeStorage
import io.github.genesjc.githubissuefinder.data.SharedPreferencesStartupSplashStorage
import io.github.genesjc.githubissuefinder.data.StartupSplashRepository
import io.github.genesjc.githubissuefinder.data.SwitchedIssueSearchApi
import io.github.genesjc.githubissuefinder.data.SwitchedReadmeApi
import io.github.genesjc.githubissuefinder.data.SwitchedRepoIssueGroupStorage
import io.github.genesjc.githubissuefinder.data.TopReposStateHolder
import io.github.genesjc.githubissuefinder.data.demo.DemoIssueSearchApi
import io.github.genesjc.githubissuefinder.data.demo.DemoReadmeApi
import io.github.genesjc.githubissuefinder.data.demo.InMemoryRepoIssueGroupStorage
import io.github.genesjc.githubissuefinder.model.DEMO_TAG
import io.github.genesjc.githubissuefinder.network.GitHubApiClient
import io.github.genesjc.githubissuefinder.ui.ContextStringResolver
import io.github.genesjc.githubissuefinder.ui.StringResolver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * The one place that says how the app's shared objects are built, bottom
 * layer first: shared plumbing, the repositories (data sources inside
 * them), then the state holders. Each `get()` is resolved by type, so a
 * class that is built here asks for what it needs in its constructor and
 * this file only has to say how to build it.
 *
 * Every definition is a `single`: one app-wide instance of each. The
 * ViewModels are not listed here. A ViewModel is created per screen by the
 * default factory, and takes what it needs with `by inject()`, resolved
 * against this module.
 */
val appModule = module {

    // Shared plumbing.
    single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    single { GitHubApiClient(tokenRepository = get(), moshi = get()) }
    single { get<GitHubApiClient>().api }

    // Resolves string resources for the wording ViewModels build.
    single<StringResolver> { ContextStringResolver(androidContext()) }

    // Build-time constants: the one place BuildConfig is read.
    // The label names the flavor too, so the drawer shows which build is running (foss or commercial).
    single {
        AppConfig(
            "${BuildConfig.BUILD_LABEL} (${BuildConfig.FLAVOR})",
            BuildConfig.SPLASH_ANIMATION,
            BuildConfig.SHOW_DETAILED_DRAWER_MENU,
        )
    }

    // Repositories over local storage.
    single { ApiTokenRepository(SharedPreferencesApiTokenStorage(androidContext()), BuildConfig.GITHUB_API_TOKEN) }
    single { IssueModeRepository(SharedPreferencesIssueModeStorage(androidContext())) }
    single { StartupSplashRepository(SharedPreferencesStartupSplashStorage(androidContext())) }
    single { BookmarksRepository(JsonFileBookmarksStorage(androidContext(), moshi = get())) }
    single { HiddenIssuesRepository(JsonFileHiddenIssuesStorage(androidContext(), moshi = get())) }
    // The cache is the real file with a token, an in-memory copy without one (demo data).
    single {
        val tokens = get<ApiTokenRepository>()
        RepoIssueGroupRepository(
            SwitchedRepoIssueGroupStorage(
                live = JsonFileRepoIssueGroupStorage(androidContext(), moshi = get()),
                demo = InMemoryRepoIssueGroupStorage(),
                useDemo = { tokens.activeTag.value == DEMO_TAG },
            ),
        )
    }

    // Repositories over the GitHub API. With no token the demo data sources answer instead, chosen
    // on every call because a token can be added or removed while the app runs.
    single {
        val tokens = get<ApiTokenRepository>()
        IssueSearchRepository(
            api = SwitchedIssueSearchApi(
                live = GitHubIssueSearchApi(get()),
                demo = DemoIssueSearchApi(),
                useDemo = { tokens.activeTag.value == DEMO_TAG },
            ),
        )
    }
    single {
        val tokens = get<ApiTokenRepository>()
        ReadmeRepository(
            api = SwitchedReadmeApi(
                live = GitHubReadmeApi(get()),
                demo = DemoReadmeApi(),
                useDemo = { tokens.activeTag.value == DEMO_TAG },
            ),
        )
    }
    single { ReadmeSnippetRepository(GitHubReadmeSource(get())) }

    // State holders.
    single {
        TopReposStateHolder(
            modeRepository = get(),
            tokenRepository = get(),
            cacheRepository = get(),
            searchRepository = get(),
        )
    }
    single {
        RepoIssuesStateHolder(
            modeRepository = get(),
            cacheRepository = get(),
            searchRepository = get(),
            tokenRepository = get(),
        )
    }
}
