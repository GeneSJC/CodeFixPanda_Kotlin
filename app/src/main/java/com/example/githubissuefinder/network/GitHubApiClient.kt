package com.example.githubissuefinder.network

import com.example.githubissuefinder.BuildConfig
import com.example.githubissuefinder.data.ApiTokenRepository
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Builds the Retrofit service for GitHub's REST API. One instance for the
 * app, created by the Koin module with the app-wide Moshi, so the
 * request converter and the cache files use the same adapters.
 *
 * The GitHub token comes from ApiTokenRepository: a token the user
 * entered in Settings if there is one, otherwise
 * BuildConfig.GITHUB_API_TOKEN, which is populated at build time from
 * local.properties (see app/build.gradle.kts) — never hardcoded here,
 * never committed to source control. It is read on every request, so a
 * token saved in Settings takes effect immediately, without a rebuild.
 */
class GitHubApiClient(
    private val tokenRepository: ApiTokenRepository,
    moshi: Moshi,
) {

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")

        val token = tokenRepository.effectiveToken.value
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

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GitHubApiService by lazy { retrofit.create(GitHubApiService::class.java) }
}
