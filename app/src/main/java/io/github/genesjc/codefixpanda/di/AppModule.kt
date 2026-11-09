package io.github.genesjc.codefixpanda.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.genesjc.codefixpanda.BuildConfig
import io.github.genesjc.codefixpanda.config.AppConfig
import io.github.genesjc.codefixpanda.config.SplashAnimation
import io.github.genesjc.codefixpanda.data.ApiTokenRepository
import io.github.genesjc.codefixpanda.data.BookmarksRepository
import io.github.genesjc.codefixpanda.data.GitHubIssueSearchApi
import io.github.genesjc.codefixpanda.data.GitHubReadmeApi
import io.github.genesjc.codefixpanda.data.GitHubReadmeSource
import io.github.genesjc.codefixpanda.data.HiddenIssuesRepository
import io.github.genesjc.codefixpanda.data.EncryptingApiTokenStorage
import io.github.genesjc.codefixpanda.data.IssueModeRepository
import io.github.genesjc.codefixpanda.data.KeystoreTokenCipher
import io.github.genesjc.codefixpanda.data.IssueSearchRepository
import io.github.genesjc.codefixpanda.data.JsonFileBookmarksStorage
import io.github.genesjc.codefixpanda.data.JsonFileHiddenIssuesStorage
import io.github.genesjc.codefixpanda.data.JsonFileRepoIssueGroupStorage
import io.github.genesjc.codefixpanda.data.ReadmeRepository
import io.github.genesjc.codefixpanda.data.ReadmeSnippetRepository
import io.github.genesjc.codefixpanda.data.RepoIssueGroupRepository
import io.github.genesjc.codefixpanda.data.SharedPreferencesApiTokenStorage
import io.github.genesjc.codefixpanda.data.SharedPreferencesIssueModeStorage
import io.github.genesjc.codefixpanda.data.SharedPreferencesStartupSplashStorage
import io.github.genesjc.codefixpanda.data.StartupSplashRepository
import io.github.genesjc.codefixpanda.data.SwitchedIssueSearchApi
import io.github.genesjc.codefixpanda.data.SwitchedReadmeApi
import io.github.genesjc.codefixpanda.data.SwitchedRepoIssueGroupStorage
import io.github.genesjc.codefixpanda.data.demo.DemoIssueSearchApi
import io.github.genesjc.codefixpanda.data.demo.DemoReadmeApi
import io.github.genesjc.codefixpanda.data.demo.InMemoryRepoIssueGroupStorage
import io.github.genesjc.codefixpanda.data.network.GitHubApiClient
import io.github.genesjc.codefixpanda.state.RepoIssuesStateHolder
import io.github.genesjc.codefixpanda.state.TopReposStateHolder
import io.github.genesjc.codefixpanda.ui.text.ContextStringResolver
import io.github.genesjc.codefixpanda.ui.text.StringResolver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * The one place that says how the app's shared objects are built, bottom layer first: shared
 * plumbing, the repositories (data sources inside them), then the state holders. Each `get()` is
 * resolved by type.
 *
 * Every definition is a `single`: one app-wide instance. ViewModels are not listed: each screen's
 * default factory creates its own, and it takes what it needs with `by inject()` against this module.
 */
val appModule = module {

    // Shared plumbing.
    single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    single {
        val tokens = get<ApiTokenRepository>()
        GitHubApiClient(tokenProvider = { tokens.effectiveToken.value }, moshi = get())
    }

    single { get<GitHubApiClient>().api }

    // Resolves string resources for the wording ViewModels build.
    single<StringResolver> { ContextStringResolver(androidContext()) }

    // Build-time constants: the one place BuildConfig is read.
    // The label names the flavor too, so the drawer shows which build is running (foss or commercial).
    single {
        AppConfig(
            "${BuildConfig.BUILD_LABEL} (${BuildConfig.FLAVOR})",
            SplashAnimation.fromConfigName(BuildConfig.SPLASH_ANIMATION),
            BuildConfig.SHOW_DETAILED_DRAWER_MENU,
        )
    }

    // Repositories over local storage.
    single {
        ApiTokenRepository(
            EncryptingApiTokenStorage(SharedPreferencesApiTokenStorage(androidContext()), KeystoreTokenCipher()),
            BuildConfig.GITHUB_API_TOKEN,
        )
    }

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
                useDemo = { tokens.isDemo.value },
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
                useDemo = { tokens.isDemo.value },
            ),
        )
    }

    single {
        val tokens = get<ApiTokenRepository>()

        ReadmeRepository(
            api = SwitchedReadmeApi(
                live = GitHubReadmeApi(get()),
                demo = DemoReadmeApi(),
                useDemo = { tokens.isDemo.value },
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
