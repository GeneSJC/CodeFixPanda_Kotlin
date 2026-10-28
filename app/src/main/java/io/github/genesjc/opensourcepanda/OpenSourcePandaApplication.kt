package io.github.genesjc.opensourcepanda

import android.app.Application
import io.github.genesjc.opensourcepanda.ads.adsModule
import io.github.genesjc.opensourcepanda.di.appModule
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
            // adsModule is supplied by the build flavor (src/foss or src/commercial).
            modules(appModule, adsModule)
        }
    }
}
