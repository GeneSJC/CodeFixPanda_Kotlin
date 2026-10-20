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
import com.example.githubissuefinder.data.TopReposStateHolder
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
    single { RepoIssueGroupRepository(JsonFileRepoIssueGroupStorage(androidContext(), moshi = get())) }

    // Repositories over the GitHub API.
    single {
        val tokens = get<ApiTokenRepository>()
        IssueSearchRepository(GitHubIssueSearchApi(get()), hasToken = { tokens.effectiveToken.value.isNotBlank() })
    }
    single {
        val tokens = get<ApiTokenRepository>()
        ReadmeRepository(GitHubReadmeApi(get()), hasToken = { tokens.effectiveToken.value.isNotBlank() })
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
