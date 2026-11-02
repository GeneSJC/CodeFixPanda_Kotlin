package io.github.genesjc.opensourcepanda.ui.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Turns a string resource into text for the wording a ViewModel builds. Fixed text in a composable
 * uses stringResource instead; this is for text that depends on state (an error with its status
 * code, "2 new repos"), so the ViewModel keeps the wording decision without holding a Context.
 * Tests register a fake that echoes the id and arguments, so they run on the JVM.
 */
interface StringResolver {
    operator fun invoke(@StringRes id: Int, vararg args: Any): String

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String
}

/** The app's resolver: reads the application's resources, which follow the device language. */
class ContextStringResolver(private val context: Context) : StringResolver {
    override fun invoke(id: Int, vararg args: Any): String = context.getString(id, *args)

    override fun plural(id: Int, count: Int, vararg args: Any): String =
        context.resources.getQuantityString(id, count, *args)
}

/**
 * A resolver over the current Context, for text a composable builds from a value that changes while
 * it is on screen (see RelativeTime.kt). Fixed text still uses stringResource.
 */
@Composable
fun rememberStringResolver(): StringResolver {
    val context = LocalContext.current
    return remember(context) { ContextStringResolver(context) }
}
