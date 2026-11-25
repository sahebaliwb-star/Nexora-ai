package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nexoraDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nexora_preferences"
)

data class NexoraSettings(
    val assistantEnabled: Boolean = true,
    val floatingEnabled: Boolean = false,
    val voiceAutoSpeak: Boolean = true,
    val voiceLanguage: String = "bn-BD", // bn-BD or en-US
    val voiceSpeed: Float = 1.0f,
    val voiceVolume: Float = 0.9f,
    val aiMemoryEnabled: Boolean = true,
    val confirmSensitiveApps: Boolean = true,
    val screenCaptureConsentGranted: Boolean = false,
    val brokerEndpointUrl: String = "",
    val brokerName: String = "Custom REST Broker",
    val gamingVoiceCoachEnabled: Boolean = true
)

class NexoraPreferences(private val context: Context) {

    private object Keys {
        val ASSISTANT_ENABLED = booleanPreferencesKey("assistant_enabled")
        val FLOATING_ENABLED = booleanPreferencesKey("floating_enabled")
        val VOICE_AUTO_SPEAK = booleanPreferencesKey("voice_auto_speak")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val AI_MEMORY_ENABLED = booleanPreferencesKey("ai_memory_enabled")
        val CONFIRM_SENSITIVE_APPS = booleanPreferencesKey("confirm_sensitive_apps")
        val SCREEN_CAPTURE_CONSENT = booleanPreferencesKey("screen_capture_consent")
        val BROKER_ENDPOINT_URL = stringPreferencesKey("broker_endpoint_url")
        val BROKER_NAME = stringPreferencesKey("broker_name")
        val GAMING_VOICE_COACH = booleanPreferencesKey("gaming_voice_coach")
    }

    val settingsFlow: Flow<NexoraSettings> = context.nexoraDataStore.data.map { prefs ->
        NexoraSettings(
            assistantEnabled = prefs[Keys.ASSISTANT_ENABLED] ?: true,
            floatingEnabled = prefs[Keys.FLOATING_ENABLED] ?: false,
            voiceAutoSpeak = prefs[Keys.VOICE_AUTO_SPEAK] ?: true,
            voiceLanguage = prefs[Keys.VOICE_LANGUAGE] ?: "bn-BD",
            voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
            voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.9f,
            aiMemoryEnabled = prefs[Keys.AI_MEMORY_ENABLED] ?: true,
            confirmSensitiveApps = prefs[Keys.CONFIRM_SENSITIVE_APPS] ?: true,
            screenCaptureConsentGranted = prefs[Keys.SCREEN_CAPTURE_CONSENT] ?: false,
            brokerEndpointUrl = prefs[Keys.BROKER_ENDPOINT_URL] ?: "",
            brokerName = prefs[Keys.BROKER_NAME] ?: "Custom REST Broker",
            gamingVoiceCoachEnabled = prefs[Keys.GAMING_VOICE_COACH] ?: true
        )
    }

    suspend fun setAssistantEnabled(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.ASSISTANT_ENABLED] = enabled }
    }

    suspend fun setFloatingEnabled(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.FLOATING_ENABLED] = enabled }
    }

    suspend fun setVoiceAutoSpeak(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.VOICE_AUTO_SPEAK] = enabled }
    }

    suspend fun setVoiceLanguage(languageTag: String) {
        context.nexoraDataStore.edit { it[Keys.VOICE_LANGUAGE] = languageTag }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.nexoraDataStore.edit { it[Keys.VOICE_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setVoiceVolume(volume: Float) {
        context.nexoraDataStore.edit { it[Keys.VOICE_VOLUME] = volume.coerceIn(0.1f, 1.0f) }
    }

    suspend fun setAiMemoryEnabled(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.AI_MEMORY_ENABLED] = enabled }
    }

    suspend fun setConfirmSensitiveApps(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.CONFIRM_SENSITIVE_APPS] = enabled }
    }

    suspend fun setScreenCaptureConsent(granted: Boolean) {
        context.nexoraDataStore.edit { it[Keys.SCREEN_CAPTURE_CONSENT] = granted }
    }

    suspend fun setBrokerConfig(name: String, endpointUrl: String) {
        context.nexoraDataStore.edit {
            it[Keys.BROKER_NAME] = name.trim()
            it[Keys.BROKER_ENDPOINT_URL] = endpointUrl.trim()
        }
    }

    suspend fun setGamingVoiceCoachEnabled(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.GAMING_VOICE_COACH] = enabled }
    }
}
