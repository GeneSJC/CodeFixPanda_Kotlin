package com.example.githubissuefinder.network

import com.example.githubissuefinder.BuildConfig
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
 * The GitHub token comes from BuildConfig.GITHUB_API_TOKEN, which is
 * populated at build time from local.properties (see app/build.gradle.kts)
 * — never hardcoded here, never committed to source control.
 */
object GitHubApiClient {

    /**
     * Whether a token is configured. FeedViewModel checks this to decide
     * between live search and local sample-data fallback — kept here
     * rather than having FeedViewModel read BuildConfig directly, so the
     * "what counts as configured" logic lives in one place.
     */
    val hasToken: Boolean get() = BuildConfig.GITHUB_API_TOKEN.isNotBlank()

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")

        if (hasToken) {
            requestBuilder.addHeader("Authorization", "Bearer ${BuildConfig.GITHUB_API_TOKEN}")
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

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GitHubApiService by lazy { retrofit.create(GitHubApiService::class.java) }
}
