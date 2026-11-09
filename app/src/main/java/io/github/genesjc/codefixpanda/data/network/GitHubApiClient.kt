package io.github.genesjc.codefixpanda.data.network

import com.squareup.moshi.Moshi
import io.github.genesjc.codefixpanda.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Builds the Retrofit service for GitHub's REST API: one instance for the app, made by the Koin
 * module with the app-wide Moshi, so requests and the cache files share the same adapters.
 *
 * The token comes from [tokenProvider]: the one the user entered in Settings, else
 * BuildConfig.GITHUB_API_TOKEN (filled from local.properties at build time, never committed). It is
 * read on every request, so a token saved in Settings applies at once, without a rebuild. The client
 * takes a function, not the repository, so the network package knows nothing about where tokens live.
 */
class GitHubApiClient(
    private val tokenProvider: () -> String,
    moshi: Moshi,
) {

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")

        val token = tokenProvider()
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
