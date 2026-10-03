package com.example.lumaassistant.ai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {
    private const val FILE_NAME = "luma_secure_prefs"
    private const val KEY_SELECTED_PROVIDER = "selected_provider"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun apiKeyField(provider: AiProvider) = "api_key_${provider.name}"

    fun saveApiKey(context: Context, provider: AiProvider, key: String) {
        prefs(context).edit().putString(apiKeyField(provider), key).apply()
    }

    fun getApiKey(context: Context, provider: AiProvider): String? =
        prefs(context).getString(apiKeyField(provider), null)

    fun clearApiKey(context: Context, provider: AiProvider) {
        prefs(context).edit().remove(apiKeyField(provider)).apply()
    }

    fun saveSelectedProvider(context: Context, provider: AiProvider) {
        prefs(context).edit().putString(KEY_SELECTED_PROVIDER, provider.name).apply()
    }

    fun getSelectedProvider(context: Context): AiProvider {
        val name = prefs(context).getString(KEY_SELECTED_PROVIDER, null)
        return AiProvider.entries.find { it.name == name } ?: AiProvider.GEMINI
    }
}
