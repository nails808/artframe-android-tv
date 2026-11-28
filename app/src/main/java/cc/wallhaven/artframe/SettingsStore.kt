package cc.wallhaven.artframe

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsStore(context: Context) {
    private val app = context.applicationContext
    private val secure by lazy {
        val masterKey = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            app,
            "artframe_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private val prefs by lazy { app.getSharedPreferences("artframe_settings", Context.MODE_PRIVATE) }

    // A user-entered key always wins. Otherwise, fall back to the key supplied
    // by the GitHub Actions environment at build time (if one was configured).
    var apiKey: String
        get() {
            val savedKey = runCatching {
                secure.getString("wallhaven_api_key", "").orEmpty().trim()
            }.getOrDefault("")
            return savedKey.ifBlank { BuildConfig.DEFAULT_WALLHAVEN_API_KEY.trim() }
        }
        set(value) {
            secure.edit().putString("wallhaven_api_key", value.trim()).apply()
        }

    var query: String
        get() = prefs.getString("query", "abstract").orEmpty()
        set(value) { prefs.edit().putString("query", value).apply() }

    var durationSeconds: Int
        get() = prefs.getInt("duration_seconds", 60).coerceIn(10, 3600)
        set(value) { prefs.edit().putInt("duration_seconds", value.coerceIn(10, 3600)).apply() }

    var includeSfw: Boolean
        get() = prefs.getBoolean("sfw", true)
        set(value) { prefs.edit().putBoolean("sfw", value).apply() }

    var includeSketchy: Boolean
        get() = prefs.getBoolean("sketchy", false)
        set(value) { prefs.edit().putBoolean("sketchy", value).apply() }

    var includeNsfw: Boolean
        get() = prefs.getBoolean("nsfw", false)
        set(value) { prefs.edit().putBoolean("nsfw", value).apply() }

    var minimumSize: String
        get() = prefs.getString("minimum_size", "1920x1080").orEmpty()
        set(value) { prefs.edit().putString("minimum_size", value).apply() }

    var preferredDisplay: String
        get() = prefs.getString("preferred_display", "Auto (TV resolution)").orEmpty()
        set(value) { prefs.edit().putString("preferred_display", value).apply() }

    var aspectRatio: String
        get() = prefs.getString("aspect_ratio", "16:9").orEmpty()
        set(value) { prefs.edit().putString("aspect_ratio", value).apply() }
}
