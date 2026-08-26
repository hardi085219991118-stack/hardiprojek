package com.example.alarm

import android.content.Context
import android.content.SharedPreferences

class FeedAlarmPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("feed_alarm_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALARM_MASTER_ENABLED = "key_alarm_master_enabled"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "key_vibration_enabled"

        private const val PREFIX_SLOT_ENABLED = "key_slot_enabled_"
        private const val PREFIX_SLOT_SOUND = "key_slot_sound_"
        private const val PREFIX_SLOT_VOLUME = "key_slot_volume_"
        private const val PREFIX_SLOT_SNOOZE = "key_slot_snooze_"
    }

    var isAlarmMasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALARM_MASTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ALARM_MASTER_ENABLED, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    // --- PER-SLOT ACTIVE STATUS ---
    fun isSlotEnabled(timeStr: String): Boolean {
        val sanitized = sanitizeSlotKey(timeStr)
        return prefs.getBoolean("$PREFIX_SLOT_ENABLED$sanitized", true)
    }

    fun setSlotEnabled(timeStr: String, enabled: Boolean) {
        val sanitized = sanitizeSlotKey(timeStr)
        prefs.edit().putBoolean("$PREFIX_SLOT_ENABLED$sanitized", enabled).apply()
    }

    // --- PER-SLOT SOUND ID ---
    fun getSlotSoundId(timeStr: String): String {
        val sanitized = sanitizeSlotKey(timeStr)
        val defaultSound = FarmAudioCatalog.getDefaultSoundForSlot(timeStr)
        return prefs.getString("$PREFIX_SLOT_SOUND$sanitized", defaultSound) ?: defaultSound
    }

    fun setSlotSoundId(timeStr: String, soundId: String) {
        val sanitized = sanitizeSlotKey(timeStr)
        prefs.edit().putString("$PREFIX_SLOT_SOUND$sanitized", soundId).apply()
    }

    // --- PER-SLOT VOLUME (0.1f - 1.0f) ---
    fun getSlotVolume(timeStr: String): Float {
        val sanitized = sanitizeSlotKey(timeStr)
        return prefs.getFloat("$PREFIX_SLOT_VOLUME$sanitized", 1.0f)
    }

    fun setSlotVolume(timeStr: String, volume: Float) {
        val sanitized = sanitizeSlotKey(timeStr)
        prefs.edit().putFloat("$PREFIX_SLOT_VOLUME$sanitized", volume.coerceIn(0.1f, 1.0f)).apply()
    }

    // --- PER-SLOT SNOOZE MINUTES (Default 5 menit) ---
    fun getSlotSnoozeMinutes(timeStr: String): Int {
        val sanitized = sanitizeSlotKey(timeStr)
        return prefs.getInt("$PREFIX_SLOT_SNOOZE$sanitized", 5)
    }

    fun setSlotSnoozeMinutes(timeStr: String, minutes: Int) {
        val sanitized = sanitizeSlotKey(timeStr)
        prefs.edit().putInt("$PREFIX_SLOT_SNOOZE$sanitized", minutes.coerceAtLeast(1)).apply()
    }

    private fun sanitizeSlotKey(timeStr: String): String {
        return timeStr.replace(":", "_").trim()
    }
}
