package io.github.genesjc.githubissuefinder.data

import android.content.Context
import io.github.genesjc.githubissuefinder.model.DEMO_TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "api_token_prefs"
private const val KEY_USER_TOKEN = "user_token"

/** Where a token currently in effect came from — shown in Settings so a user can tell which one the app is using. */
enum class TokenSource {
    /** A token the user entered in Settings. */
    USER,

    /** The token baked in at build time from local.properties. */
    BUILD,

    /** Neither is set, so live requests can't be made. */
    NONE,
}

/**
 * The token the app should actually use: whatever the user entered in
 * Settings if there is one, otherwise the build-time token. A value the
 * user typed always wins over the build one — that is the whole point of
 * letting someone install a built APK and supply their own. Blank or
 * whitespace-only counts as "not set" for both, so a stray space can't
 * hide a working build token or masquerade as a real one.
 */
fun resolveEffectiveToken(userToken: String, buildToken: String): String =
    userToken.trim().ifBlank { buildToken.trim() }

/** Which of the two [resolveEffectiveToken] picked, by the same blank-means-unset rule. */
fun tokenSourceOf(userToken: String, buildToken: String): TokenSource = when {
    userToken.isNotBlank() -> TokenSource.USER
    buildToken.isNotBlank() -> TokenSource.BUILD
    else -> TokenSource.NONE
}

/**
 * Where the token the user typed is persisted, split out from
 * ApiTokenRepository so the repository can be unit-tested against a
 * plain in-memory fake with no Android Context — same shape as
 * IssueModeStorage.
 */
interface ApiTokenStorage {
    /** The saved token, or null if none has been saved (or it was cleared). */
    fun load(): String?

    /** Saves [token], or clears the stored value when null. */
    fun save(token: String?)
}

/**
 * Plain SharedPreferences in the app's private storage; the app wraps it in [EncryptingApiTokenStorage],
 * so the value in the file is ciphertext. Backup and device-transfer copies are also kept out, which is
 * why res/xml/backup_rules.xml and data_extraction_rules.xml exclude this file by name ([PREFS_NAME]);
 * keep them in sync if it changes. More in docs/design-notes.md, "Token storage".
 */
class SharedPreferencesApiTokenStorage(context: Context) : ApiTokenStorage {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): String? = prefs.getString(KEY_USER_TOKEN, null)

    override fun save(token: String?) {
        prefs.edit().putString(KEY_USER_TOKEN, token).apply()
    }
}

/**
 * The single owner of the GitHub token. It combines the build-time token (from local.properties,
 * only available to someone building from source) with one the user enters in the app, so a person
 * who installs a built APK can supply their own; the user's token wins. [effectiveToken] and
 * [userToken] are StateFlows: the network client reads the current value on every request, so a
 * saved token applies without a rebuild, and ViewModels can react to a change. Both are updated
 * together in [setUserToken], so they cannot disagree with each other or with storage.
 */
class ApiTokenRepository(
    private val storage: ApiTokenStorage,
    private val buildToken: String,
) {

    private val _userToken = MutableStateFlow(storage.load().orEmpty().trim())

    /** The token the user entered, or empty if none. Pre-fills the Settings field. */
    val userToken: StateFlow<String> = _userToken.asStateFlow()

    private val _effectiveToken = MutableStateFlow(resolveEffectiveToken(_userToken.value, buildToken))

    /** The token to send to GitHub, or empty if there is none at all. */
    val effectiveToken: StateFlow<String> = _effectiveToken.asStateFlow()

    private val _activeTag = MutableStateFlow(activeTagFor(_effectiveToken.value))

    /**
     * The tag of the data in use: none with a token (real GitHub data), [DEMO_TAG] without one (demo
     * data). The one fact that decides which saved items a screen lists. Updated with [effectiveToken].
     */
    val activeTag: StateFlow<String?> = _activeTag.asStateFlow()

    private val _isDemo = MutableStateFlow(_activeTag.value == DEMO_TAG)

    /** True while the demo data is in use (no token), for the banner. */
    val isDemo: StateFlow<Boolean> = _isDemo.asStateFlow()

    private val _tokenSource = MutableStateFlow(tokenSourceOf(_userToken.value, buildToken))

    /** Which source [effectiveToken] came from. */
    val tokenSource: StateFlow<TokenSource> = _tokenSource.asStateFlow()

    /**
     * Saves the token the user entered. Surrounding whitespace is
     * trimmed (a pasted token often carries a trailing newline), and a
     * blank value clears the stored one, falling back to the build token.
     */
    fun setUserToken(raw: String) {
        val trimmed = raw.trim()
        _userToken.value = trimmed
        // Saving or removing a token is the user choosing, so a demo started this session ends.
        demoOnly = false
        publish()
        storage.save(trimmed.ifEmpty { null })
    }

    /** True from [startDemo] until the user saves or removes a token, or the app is closed. Never stored. */
    private var demoOnly = false

    /**
     * Shows the demo data for the rest of this session even if a token is set (a saved one or a build one).
     * Nothing is deleted or stored: the token is simply not used, so the next launch is back to normal.
     */
    fun startDemo() {
        demoOnly = true
        publish()
    }

    /** Recomputes everything that follows from the saved token, the build token and whether a demo is on. */
    private fun publish() {
        val effective = if (demoOnly) "" else resolveEffectiveToken(_userToken.value, buildToken)
        _effectiveToken.value = effective
        _activeTag.value = activeTagFor(effective)
        _isDemo.value = _activeTag.value == DEMO_TAG
        _tokenSource.value = if (demoOnly) TokenSource.NONE else tokenSourceOf(_userToken.value, buildToken)
    }
}

/** No token means the demo data is in use; any token means the real data. */
internal fun activeTagFor(effectiveToken: String): String? = if (effectiveToken.isBlank()) DEMO_TAG else null
