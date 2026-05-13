package com.elix.assistant.ui.settings

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PrimeContact(
    val name: String,
    val phoneNumber: String,
)

class AppSettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings =
        AppSettings(
            apiKey = prefs.getString(KEY_API_KEY, "").orEmpty(),
            userName = prefs.getString(KEY_USER_NAME, "").orEmpty(),
            modelIndex = prefs.getInt(KEY_MODEL_INDEX, 0),
            voiceIndex = prefs.getInt(KEY_VOICE_INDEX, 0),
            personality = prefs.getString(KEY_PERSONALITY, Personality.ASSISTANT.storageValue)
                ?.let(Personality::fromStorageValue)
                ?: Personality.ASSISTANT,
            primeContacts = readPrimeContacts(),
        )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_API_KEY, settings.apiKey)
            .putString(KEY_USER_NAME, settings.userName)
            .putInt(KEY_MODEL_INDEX, settings.modelIndex)
            .putInt(KEY_VOICE_INDEX, settings.voiceIndex)
            .putString(KEY_PERSONALITY, settings.personality.storageValue)
            .putString(KEY_PRIME_CONTACTS_JSON, encodePrimeContacts(settings.primeContacts))
            .apply()
    }

    private fun readPrimeContacts(): List<PrimeContact> {
        val raw = prefs.getString(KEY_PRIME_CONTACTS_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val o = array.optJSONObject(index) ?: continue
                    val name = o.optString("name", "").trim()
                    val phoneNumber = o.optString("phoneNumber", "").trim()
                    if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                        add(PrimeContact(name = name, phoneNumber = phoneNumber))
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodePrimeContacts(contacts: List<PrimeContact>): String {
        val array = JSONArray()
        contacts.forEach { c ->
            array.put(
                JSONObject()
                    .put("name", c.name)
                    .put("phoneNumber", c.phoneNumber),
            )
        }
        return array.toString()
    }

    data class AppSettings(
        val apiKey: String,
        val userName: String,
        val modelIndex: Int,
        val voiceIndex: Int,
        val personality: Personality,
        val primeContacts: List<PrimeContact>,
    )

    enum class Personality(val storageValue: String) {
        GF("gf"),
        PRO("pro"),
        ASSISTANT("assistant"),
        ;

        companion object {
            fun fromStorageValue(value: String): Personality =
                entries.firstOrNull { it.storageValue == value } ?: ASSISTANT
        }
    }

    private companion object {
        private const val PREFS_NAME = "elix_settings"

        private const val KEY_API_KEY = "api_key"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_MODEL_INDEX = "model_index"
        private const val KEY_VOICE_INDEX = "voice_index"
        private const val KEY_PERSONALITY = "personality"
        private const val KEY_PRIME_CONTACTS_JSON = "prime_contacts_json"
    }
}

