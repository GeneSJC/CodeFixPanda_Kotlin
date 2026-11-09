package io.github.genesjc.codefixpanda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.genesjc.codefixpanda.ui.OpenSourcePandaApp

/**
 * The manifest's android:configChanges stops the system from recreating this Activity on rotation.
 * Recreation reset `hasShownSplash` (replaying the start animation on a running app) and was the
 * suspected cause of a white screen after rotating to landscape. With it, Compose just recomposes
 * in place, which is how TopReposScreen's landscape detection expects to work.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OpenSourcePandaApp() }
    }
}
