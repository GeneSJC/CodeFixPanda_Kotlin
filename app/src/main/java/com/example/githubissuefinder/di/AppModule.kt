package com.example.githubissuefinder.di

import com.example.githubissuefinder.BuildConfig
import com.example.githubissuefinder.data.ApiTokenRepository
import com.example.githubissuefinder.data.AppConfig
import com.example.githubissuefinder.data.AppConfigRepository
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
import com.example.githubissuefinder.ui.discover.BookmarksViewModel
import com.example.githubissuefinder.ui.discover.HiddenIssuesViewModel
import com.example.githubissuefinder.ui.discover.IssueModeViewModel
import com.example.githubissuefinder.ui.discover.RepoIssuesViewModel
import com.example.githubissuefinder.ui.discover.RepoReadmeViewModel
import com.example.githubissuefinder.ui.discover.TopReposViewModel
import com.example.githubissuefinder.ui.settings.ApiTokenViewModel
import com.example.githubissuefinder.ui.splash.StartupSplashViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The one place that says how the app's objects are built, bottom layer
 * first: shared plumbing, the repositories (data sources inside them),
 * the state holders, then the ViewModels. Each `get()` is resolved by
 * type, so a class asks for what it needs in its constructor and this
 * file only has to say how to build it.
 *
 * Every definition is a `single` except the ViewModels: one app-wide
 * instance of each repository and state holder, while a ViewModel is
 * created per screen and lives in the ViewModel store of its owner (the
 * Activity or a navigation destination), exactly as it did before.
 * `viewModelOf(::X)` builds a ViewModel from the arguments of its
 * constructor, each resolved by type.
 */
val appModule = module {

    // Shared plumbing.
    single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    single { GitHubApiClient(tokenRepository = get(), moshi = get()) }
    single { get<GitHubApiClient>().api }

    // Repositories over local storage.
    single { AppConfigRepository(AppConfig(BuildConfig.BUILD_LABEL, BuildConfig.SPLASH_ANIMATION, BuildConfig.SHOW_DETAILED_DRAWER_MENU)) }
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

    // ViewModels.
    viewModelOf(::IssueModeViewModel)
    viewModelOf(::StartupSplashViewModel)
    viewModelOf(::ApiTokenViewModel)
    viewModelOf(::RepoReadmeViewModel)
    viewModelOf(::HiddenIssuesViewModel)
    viewModelOf(::BookmarksViewModel)
    viewModelOf(::RepoIssuesViewModel)
    viewModelOf(::TopReposViewModel)
}
