package io.github.genesjc.codefixpanda

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text

/**
 * Walkthrough sandbox. Never merged. Each chunk of the review gets a minimal example here.
 *
 * Chunk 1 (MainActivity.onCreate): the same five lines as the real Activity. It leaves out the
 * manifest's android:configChanges on purpose, so rotating the device recreates it and shows what
 * the real Activity avoids. The Text sits at the very top so enableEdgeToEdge() is visible.
 */
class MainActivity2 : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("Mini", "onCreate: fresh launch = ${savedInstanceState == null}")
        setContent { Text("Top of the screen: is this under the status bar?") }
    }
}
