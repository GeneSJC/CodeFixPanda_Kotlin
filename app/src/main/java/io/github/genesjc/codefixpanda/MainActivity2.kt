package io.github.genesjc.codefixpanda

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.dsl.module

/**
 * Walkthrough sandbox. Never merged. Each chunk of the review gets a minimal example here.
 *
 * Chunk 2 (Application + Koin): CodeFixPandaApplication already ran startKoin before this
 * Activity exists. Here we add one tiny module and watch when its `single` is actually built.
 */
class Greeter {
    init { Log.d("Mini", "Greeter constructed") }   // appears only on first use, not at registration
    fun hello() = "Hello from Koin"
}

class MainActivity2 : ComponentActivity() {

    private val greeter: Greeter by inject()   // lazy delegate: nothing resolved yet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getKoin().loadModules(listOf(module { single { Greeter() } }))
        Log.d("Mini", "module registered, Greeter not built yet")
        setContent { Text(greeter.hello()) }   // first read of `greeter` builds it
    }
}
