package com.example.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FeedScheduleLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val cycleId = intent.getLongExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, 0L)
        val coopId = intent.getLongExtra(FeedAlarmScheduler.EXTRA_COOP_ID, 0L)
        val coopName = intent.getStringExtra(FeedAlarmScheduler.EXTRA_COOP_NAME) ?: "Kandang Broiler"
        val timeStr = intent.getStringExtra(FeedAlarmScheduler.EXTRA_TIME_STR) ?: "06:00"
        val slotName = intent.getStringExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME) ?: "Pagi"
        val instruction = intent.getStringExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION) ?: ""
        val ageDays = intent.getIntExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, 0)
        val phase = intent.getStringExtra(FeedAlarmScheduler.EXTRA_PHASE) ?: "Fase Brooding"
        val feedType = intent.getStringExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE) ?: "Pakan Prestarter"
        val scheduleId = intent.getLongExtra(FeedAlarmScheduler.EXTRA_SCHEDULE_ID, 0L)
        val snoozeMinutes = intent.getIntExtra(FeedAlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)

        val prefs = FeedAlarmPreferences(context)
        val soundId = intent.getStringExtra(FeedAlarmScheduler.EXTRA_SOUND_ID) ?: prefs.getSlotSoundId(timeStr)
        val volume = intent.getFloatExtra(FeedAlarmScheduler.EXTRA_VOLUME, prefs.getSlotVolume(timeStr))

        val alarmState = ActiveAlarmState(
            isActive = true,
            scheduleId = scheduleId,
            cycleId = cycleId,
            coopId = coopId,
            coopName = coopName,
            timeStr = timeStr,
            slotName = slotName,
            instruction = instruction,
            ageDays = ageDays,
            phase = phase,
            feedType = feedType,
            soundId = soundId,
            volume = volume
        )

        when (action) {
            FeedAlarmScheduler.ACTION_FEED_ALARM -> {
                // Jalankan foreground service untuk memutar audio alarm looping & notifikasi heads-up
                FeedAlarmAudioService.start(context, alarmState)
            }

            FeedAlarmScheduler.ACTION_STOP_ALARM -> {
                FeedAlarmAudioService.stop(context)
                dismissNotification(context, generateNotificationId(timeStr, cycleId))
            }

            FeedAlarmScheduler.ACTION_STATUS_DONE -> {
                handleStatusUpdate(context, cycleId, coopId, timeStr, slotName, instruction, ageDays, phase, feedType, "SELESAI")
                FeedAlarmAudioService.stop(context)
                dismissNotification(context, generateNotificationId(timeStr, cycleId))
            }

            FeedAlarmScheduler.ACTION_STATUS_SNOOZE -> {
                handleStatusUpdate(context, cycleId, coopId, timeStr, slotName, instruction, ageDays, phase, feedType, "DITUNDA", snoozeMinutes)
                FeedAlarmScheduler.scheduleSnooze(
                    context = context,
                    scheduleId = scheduleId,
                    cycleId = cycleId,
                    coopId = coopId,
                    coopName = coopName,
                    timeStr = timeStr,
                    slotName = slotName,
                    instruction = instruction,
                    ageDays = ageDays,
                    phase = phase,
                    feedType = feedType,
                    snoozeMinutes = snoozeMinutes,
                    soundId = soundId,
                    volume = volume
                )
                FeedAlarmAudioService.stop(context)
                dismissNotification(context, generateNotificationId(timeStr, cycleId))
            }

            FeedAlarmScheduler.ACTION_STATUS_SKIP -> {
                handleStatusUpdate(context, cycleId, coopId, timeStr, slotName, instruction, ageDays, phase, feedType, "DILEWATI")
                FeedAlarmAudioService.stop(context)
                dismissNotification(context, generateNotificationId(timeStr, cycleId))
            }
        }
    }

    private fun handleStatusUpdate(
        context: Context,
        cycleId: Long,
        coopId: Long,
        timeStr: String,
        slotName: String,
        instruction: String,
        ageDays: Int,
        phase: String,
        feedType: String,
        status: String,
        snoozeMinutes: Int = 0
    ) {
        val today = FeedGuideRules.getTodayDateString()
        val actualTime = FeedGuideRules.getCurrentTimeString()
        val now = System.currentTimeMillis()
        val snoozeEpoch = if (status == "DITUNDA") now + (snoozeMinutes * 60 * 1000L) else 0L

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val dao = db.farmDao()
            val existing = dao.getFeedScheduleBySlot(cycleId, today, timeStr)

            if (existing != null) {
                dao.updateFeedSchedule(
                    existing.copy(
                        status = status,
                        actualTime = actualTime,
                        snoozeMinutes = snoozeMinutes,
                        snoozeUntilEpoch = snoozeEpoch,
                        updatedAt = now
                    )
                )
            } else {
                dao.insertFeedSchedule(
                    FeedScheduleLogEntity(
                        cycleId = cycleId,
                        coopId = coopId,
                        date = today,
                        scheduledTime = timeStr,
                        slotName = slotName,
                        instruction = instruction,
                        actualTime = actualTime,
                        ageDays = ageDays,
                        phase = phase,
                        feedType = feedType,
                        status = status,
                        snoozeMinutes = snoozeMinutes,
                        snoozeUntilEpoch = snoozeEpoch,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }

    private fun dismissNotification(context: Context, notifId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(notifId)
        manager?.cancel(FeedAlarmAudioService.NOTIFICATION_ID)
    }

    private fun generateNotificationId(timeStr: String, cycleId: Long): Int {
        val timeHash = timeStr.replace(":", "").toIntOrNull() ?: 100
        return (cycleId.toInt() * 10000) + timeHash
    }
}
