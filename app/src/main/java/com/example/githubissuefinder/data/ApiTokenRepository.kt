package com.example.githubissuefinder.data

import android.content.Context
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
 * Plain SharedPreferences, deliberately NOT encrypted. The file lives in
 * the app's private storage, which Android's sandbox keeps other apps
 * out of, and the token this app asks for needs no permissions at all —
 * it only reads public data — so the worst a leak could do is spend the
 * user's rate limit. The remaining real exposure is Android's backup
 * and device-transfer copies of app data, which is why
 * res/xml/backup_rules.xml and data_extraction_rules.xml exclude this
 * file by name ([PREFS_NAME]) — keep those in sync if it ever changes.
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
 * The single owner of the GitHub token the app talks to GitHub with. It
 * combines the build-time token (from local.properties, only available
 * to someone building from source) with one the user can enter in the
 * app, so a person who installs a built APK can still supply their own.
 *
 * Why a repository, in the data layer: persistence and the
 * "which token wins" rule are data-layer concerns. Settings and the
 * network client both go through this class; nothing else knows the
 * token is stored in SharedPreferences, or in what order the sources
 * are consulted.
 *
 * Why [effectiveToken] and [userToken] are StateFlows: the network
 * client reads the current value on every request, so a token saved in
 * Settings applies immediately without a rebuild, and ViewModels can
 * observe a change (for example, Top Repos reloading once a token is
 * finally supplied). The private mutable / public read-only pairs are
 * the standard encapsulation — only this class can change the values.
 *
 * Both StateFlows are updated together in [setUserToken], under one
 * function, so they can never disagree with each other or with storage.
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
        _effectiveToken.value = resolveEffectiveToken(trimmed, buildToken)
        _tokenSource.value = tokenSourceOf(trimmed, buildToken)
        storage.save(trimmed.ifEmpty { null })
    }
}
