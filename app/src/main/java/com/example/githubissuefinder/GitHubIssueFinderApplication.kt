package com.example.githubissuefinder

import android.app.Application
import com.example.githubissuefinder.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Starts the dependency container once per process, before any activity
 * exists. An Application is created exactly once, so unlike
 * MainActivity.onCreate (which runs again on recreation) there is no
 * risk of starting the container twice.
 */
class GitHubIssueFinderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GitHubIssueFinderApplication)
            modules(appModule)
        }
    }
}
