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
        private const val KEY_SLOT_0600 = "key_slot_0600"
        private const val KEY_SLOT_1100 = "key_slot_1100"
        private const val KEY_SLOT_1600 = "key_slot_1600"
        private const val KEY_SLOT_2000 = "key_slot_2000"
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

    fun isSlotEnabled(timeStr: String): Boolean {
        return when (timeStr) {
            "06:00" -> prefs.getBoolean(KEY_SLOT_0600, true)
            "11:00" -> prefs.getBoolean(KEY_SLOT_1100, true)
            "16:00" -> prefs.getBoolean(KEY_SLOT_1600, true)
            "20:00" -> prefs.getBoolean(KEY_SLOT_2000, true)
            else -> true
        }
    }

    fun setSlotEnabled(timeStr: String, enabled: Boolean) {
        when (timeStr) {
            "06:00" -> prefs.edit().putBoolean(KEY_SLOT_0600, enabled).apply()
            "11:00" -> prefs.edit().putBoolean(KEY_SLOT_1100, enabled).apply()
            "16:00" -> prefs.edit().putBoolean(KEY_SLOT_1600, enabled).apply()
            "20:00" -> prefs.edit().putBoolean(KEY_SLOT_2000, enabled).apply()
        }
    }
}
