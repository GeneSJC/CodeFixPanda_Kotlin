package com.example.githubissuefinder.network

import com.example.githubissuefinder.BuildConfig
import com.example.githubissuefinder.data.ApiTokenRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Plain Kotlin singleton, NOT a Hilt-managed dependency graph — a
 * deliberate, temporary stand-in. This whole object goes away once Hilt
 * is introduced in a later Commit Milestone and replaces manual wiring
 * like this with proper constructor injection.
 *
 * The GitHub token comes from ApiTokenRepository: a token the user
 * entered in Settings if there is one, otherwise
 * BuildConfig.GITHUB_API_TOKEN, which is populated at build time from
 * local.properties (see app/build.gradle.kts) — never hardcoded here,
 * never committed to source control. It is read on every request, so a
 * token saved in Settings takes effect immediately, without a rebuild.
 */
object GitHubApiClient {

    /**
     * Whether a token is configured. The repo-first flow's ViewModels
     * check this before issuing a live search — kept here rather than
     * having each caller read BuildConfig directly, so the "what counts
     * as configured" logic lives in one place.
     */
    val hasToken: Boolean get() = ApiTokenRepository.instance.effectiveToken.value.isNotBlank()

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")

        val token = ApiTokenRepository.instance.effectiveToken.value
        if (token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    // v0.45: was private — exposed so RepoIssueGroupRepository can serialize
    // its own cache file with the exact same Moshi instance (same
    // adapters, same config) rather than building a second, separate
    // Moshi just for that, which would be redundant setup for no
    // benefit — this project doesn't yet have DI to share it any more
    // formally than that.
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GitHubApiService by lazy { retrofit.create(GitHubApiService::class.java) }
}
