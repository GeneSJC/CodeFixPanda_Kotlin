package io.github.genesjc.codefixpanda

import android.app.Application
import io.github.genesjc.codefixpanda.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Starts the dependency container once per process, before any activity
 * exists. An Application is created exactly once, so unlike
 * MainActivity.onCreate (which runs again on recreation) there is no
 * risk of starting the container twice.
 */
class OpenSourcePandaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OpenSourcePandaApplication)
            modules(appModule)
        }
    }
}
