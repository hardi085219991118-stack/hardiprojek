package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

object FeedAlarmScheduler {
    private const val TAG = "FeedAlarmScheduler"

    const val ACTION_FEED_ALARM = "com.example.action.FEED_ALARM"
    const val ACTION_STATUS_DONE = "com.example.action.FEED_STATUS_DONE"
    const val ACTION_STATUS_SNOOZE = "com.example.action.FEED_STATUS_SNOOZE"
    const val ACTION_STATUS_SKIP = "com.example.action.FEED_STATUS_SKIP"

    const val EXTRA_CYCLE_ID = "extra_cycle_id"
    const val EXTRA_COOP_ID = "extra_coop_id"
    const val EXTRA_COOP_NAME = "extra_coop_name"
    const val EXTRA_TIME_STR = "extra_time_str"
    const val EXTRA_SLOT_NAME = "extra_slot_name"
    const val EXTRA_INSTRUCTION = "extra_instruction"
    const val EXTRA_AGE_DAYS = "extra_age_days"
    const val EXTRA_PHASE = "extra_phase"
    const val EXTRA_FEED_TYPE = "extra_feed_type"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    /**
     * Jadwalkan ulang semua slot alarm untuk siklus aktif hari ini / besok.
     */
    fun scheduleAllDailySlots(
        context: Context,
        cycleId: Long,
        coopId: Long,
        coopName: String,
        ageDays: Int
    ) {
        val prefs = FeedAlarmPreferences(context)
        if (!prefs.isAlarmMasterEnabled) {
            cancelAllAlarms(context, cycleId)
            return
        }

        val phaseDetail = FeedGuideRules.getPhaseDetailForAge(ageDays)

        FeedGuideRules.STANDARD_SLOTS.forEachIndexed { index, slot ->
            if (prefs.isSlotEnabled(slot.time)) {
                scheduleSingleSlot(
                    context = context,
                    slotIndex = index,
                    timeStr = slot.time,
                    slotName = slot.slotName,
                    instruction = slot.taskInstruction,
                    cycleId = cycleId,
                    coopId = coopId,
                    coopName = coopName,
                    ageDays = ageDays,
                    phase = phaseDetail.phaseName,
                    feedType = phaseDetail.feedType
                )
            } else {
                cancelSingleSlot(context, index, cycleId)
            }
        }
    }

    /**
     * Jadwalkan satu slot waktu feeding (06:00, 11:00, 16:00, 20:00).
     */
    fun scheduleSingleSlot(
        context: Context,
        slotIndex: Int,
        timeStr: String,
        slotName: String,
        instruction: String,
        cycleId: Long,
        coopId: Long,
        coopName: String,
        ageDays: Int,
        phase: String,
        feedType: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 6
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Jika waktu untuk hari ini sudah lewat, jadwalkan untuk besok
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, FeedAlarmReceiver::class.java).apply {
            action = ACTION_FEED_ALARM
            putExtra(EXTRA_CYCLE_ID, cycleId)
            putExtra(EXTRA_COOP_ID, coopId)
            putExtra(EXTRA_COOP_NAME, coopName)
            putExtra(EXTRA_TIME_STR, timeStr)
            putExtra(EXTRA_SLOT_NAME, slotName)
            putExtra(EXTRA_INSTRUCTION, instruction)
            putExtra(EXTRA_AGE_DAYS, ageDays)
            putExtra(EXTRA_PHASE, phase)
            putExtra(EXTRA_FEED_TYPE, feedType)
        }

        val requestCode = generateRequestCode(slotIndex, cycleId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm slot $slotName ($timeStr) at ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}", e)
        }
    }

    /**
     * Menjadwalkan tunda alarm (Snooze 5, 10, 15 menit).
     */
    fun scheduleSnooze(
        context: Context,
        scheduleId: Long,
        cycleId: Long,
        coopId: Long,
        coopName: String,
        timeStr: String,
        slotName: String,
        instruction: String,
        ageDays: Int,
        phase: String,
        feedType: String,
        snoozeMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, FeedAlarmReceiver::class.java).apply {
            action = ACTION_FEED_ALARM
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_CYCLE_ID, cycleId)
            putExtra(EXTRA_COOP_ID, coopId)
            putExtra(EXTRA_COOP_NAME, coopName)
            putExtra(EXTRA_TIME_STR, timeStr)
            putExtra(EXTRA_SLOT_NAME, "$slotName (Ditunda ${snoozeMinutes}m)")
            putExtra(EXTRA_INSTRUCTION, instruction)
            putExtra(EXTRA_AGE_DAYS, ageDays)
            putExtra(EXTRA_PHASE, phase)
            putExtra(EXTRA_FEED_TYPE, feedType)
        }

        val requestCode = (scheduleId.toInt() * 100) + 777
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled snooze for $snoozeMinutes mins")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling snooze: ${e.message}", e)
        }
    }

    private fun cancelSingleSlot(context: Context, slotIndex: Int, cycleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FeedAlarmReceiver::class.java).apply {
            action = ACTION_FEED_ALARM
        }
        val requestCode = generateRequestCode(slotIndex, cycleId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAllAlarms(context: Context, cycleId: Long) {
        for (i in 0..3) {
            cancelSingleSlot(context, i, cycleId)
        }
    }

    private fun generateRequestCode(slotIndex: Int, cycleId: Long): Int {
        return (cycleId.toInt() * 10) + slotIndex
    }
}
