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

    var apiKey: String
        get() = runCatching { secure.getString("wallhaven_api_key", "").orEmpty() }.getOrDefault("")
        set(value) { secure.edit().putString("wallhaven_api_key", value.trim()).apply() }

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
}
