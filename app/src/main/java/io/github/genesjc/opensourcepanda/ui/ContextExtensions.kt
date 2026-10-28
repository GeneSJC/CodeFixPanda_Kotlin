package io.github.genesjc.opensourcepanda.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The Activity a Compose [Context] belongs to (LocalContext may be wrapped), or null if there is none. */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
