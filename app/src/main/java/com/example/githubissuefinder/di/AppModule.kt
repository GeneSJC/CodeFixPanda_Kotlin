package com.example.githubissuefinder.di

import com.example.githubissuefinder.BuildConfig
import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.AppConfig
import com.example.githubissuefinder.data.BookmarksRepository
import com.example.githubissuefinder.data.GitHubIssueSearchApi
import com.example.githubissuefinder.data.GitHubReadmeApi
import com.example.githubissuefinder.data.GitHubReadmeSource
import com.example.githubissuefinder.data.HiddenIssuesRepository
import com.example.githubissuefinder.data.IssueModeRepository
import com.example.githubissuefinder.data.IssueSearchRepository
import com.example.githubissuefinder.data.JsonFileBookmarksStorage
import com.example.githubissuefinder.data.JsonFileHiddenIssuesStorage
import com.example.githubissuefinder.data.JsonFileRepoIssueGroupStorage
import com.example.githubissuefinder.data.ReadmeRepository
import com.example.githubissuefinder.data.ReadmeSnippetRepository
import com.example.githubissuefinder.data.RepoIssueGroupRepository
import com.example.githubissuefinder.data.RepoIssuesStateHolder
import com.example.githubissuefinder.data.SharedPreferencesApiTokenStorage
import com.example.githubissuefinder.data.SharedPreferencesIssueModeStorage
import com.example.githubissuefinder.data.SharedPreferencesStartupSplashStorage
import com.example.githubissuefinder.data.StartupSplashRepository
import com.example.githubissuefinder.data.SwitchedIssueSearchApi
import com.example.githubissuefinder.data.SwitchedReadmeApi
import com.example.githubissuefinder.data.SwitchedRepoIssueGroupStorage
import com.example.githubissuefinder.data.TopReposStateHolder
import com.example.githubissuefinder.data.demo.DemoIssueSearchApi
import com.example.githubissuefinder.data.demo.DemoReadmeApi
import com.example.githubissuefinder.data.demo.InMemoryRepoIssueGroupStorage
import com.example.githubissuefinder.model.DEMO_TAG
import com.example.githubissuefinder.network.GitHubApiClient
import com.example.githubissuefinder.ui.ContextStringResolver
import com.example.githubissuefinder.ui.StringResolver
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
    single { AppConfig(BuildConfig.BUILD_LABEL, BuildConfig.SPLASH_ANIMATION, BuildConfig.SHOW_DETAILED_DRAWER_MENU) }

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
