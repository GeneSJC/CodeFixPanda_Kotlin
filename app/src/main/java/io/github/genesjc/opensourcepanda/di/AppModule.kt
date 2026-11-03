package io.github.genesjc.opensourcepanda.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.genesjc.opensourcepanda.BuildConfig
import io.github.genesjc.opensourcepanda.config.AppConfig
import io.github.genesjc.opensourcepanda.config.SplashAnimation
import io.github.genesjc.opensourcepanda.data.ApiTokenRepository
import io.github.genesjc.opensourcepanda.data.BookmarksRepository
import io.github.genesjc.opensourcepanda.data.GitHubIssueSearchApi
import io.github.genesjc.opensourcepanda.data.GitHubReadmeApi
import io.github.genesjc.opensourcepanda.data.GitHubReadmeSource
import io.github.genesjc.opensourcepanda.data.HiddenIssuesRepository
import io.github.genesjc.opensourcepanda.data.EncryptingApiTokenStorage
import io.github.genesjc.opensourcepanda.data.IssueModeRepository
import io.github.genesjc.opensourcepanda.data.KeystoreTokenCipher
import io.github.genesjc.opensourcepanda.data.IssueSearchRepository
import io.github.genesjc.opensourcepanda.data.JsonFileBookmarksStorage
import io.github.genesjc.opensourcepanda.data.JsonFileHiddenIssuesStorage
import io.github.genesjc.opensourcepanda.data.JsonFileRepoIssueGroupStorage
import io.github.genesjc.opensourcepanda.data.ReadmeRepository
import io.github.genesjc.opensourcepanda.data.ReadmeSnippetRepository
import io.github.genesjc.opensourcepanda.data.RepoIssueGroupRepository
import io.github.genesjc.opensourcepanda.data.SharedPreferencesApiTokenStorage
import io.github.genesjc.opensourcepanda.data.SharedPreferencesIssueModeStorage
import io.github.genesjc.opensourcepanda.data.SharedPreferencesStartupSplashStorage
import io.github.genesjc.opensourcepanda.data.StartupSplashRepository
import io.github.genesjc.opensourcepanda.data.SwitchedIssueSearchApi
import io.github.genesjc.opensourcepanda.data.SwitchedReadmeApi
import io.github.genesjc.opensourcepanda.data.SwitchedRepoIssueGroupStorage
import io.github.genesjc.opensourcepanda.data.demo.DemoIssueSearchApi
import io.github.genesjc.opensourcepanda.data.demo.DemoReadmeApi
import io.github.genesjc.opensourcepanda.data.demo.InMemoryRepoIssueGroupStorage
import io.github.genesjc.opensourcepanda.data.network.GitHubApiClient
import io.github.genesjc.opensourcepanda.state.RepoIssuesStateHolder
import io.github.genesjc.opensourcepanda.state.TopReposStateHolder
import io.github.genesjc.opensourcepanda.ui.text.ContextStringResolver
import io.github.genesjc.opensourcepanda.ui.text.StringResolver
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
