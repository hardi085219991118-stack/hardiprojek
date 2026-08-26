package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FeedScheduleLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedAlarmAudioService : Service() {

    companion object {
        const val CHANNEL_ID = "feed_alarm_audio_channel"
        const val CHANNEL_NAME = "Alarm Suara Pakan Aktif"
        const val NOTIFICATION_ID = 99991

        const val ACTION_START_ALARM = "com.example.action.START_ALARM_SERVICE"
        const val ACTION_STOP_ALARM = "com.example.action.STOP_ALARM_SERVICE"
        const val ACTION_ACTION_DONE = "com.example.action.SERVICE_STATUS_DONE"
        const val ACTION_ACTION_SNOOZE = "com.example.action.SERVICE_STATUS_SNOOZE"

        fun start(context: Context, alarmState: ActiveAlarmState) {
            val intent = Intent(context, FeedAlarmAudioService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, alarmState.cycleId)
                putExtra(FeedAlarmScheduler.EXTRA_COOP_ID, alarmState.coopId)
                putExtra(FeedAlarmScheduler.EXTRA_COOP_NAME, alarmState.coopName)
                putExtra(FeedAlarmScheduler.EXTRA_TIME_STR, alarmState.timeStr)
                putExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME, alarmState.slotName)
                putExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION, alarmState.instruction)
                putExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, alarmState.ageDays)
                putExtra(FeedAlarmScheduler.EXTRA_PHASE, alarmState.phase)
                putExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE, alarmState.feedType)
                putExtra(FeedAlarmScheduler.EXTRA_SOUND_ID, alarmState.soundId)
                putExtra(FeedAlarmScheduler.EXTRA_VOLUME, alarmState.volume)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FeedAlarmAudioService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_ALARM -> {
                val cycleId = intent.getLongExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, 0L)
                val coopId = intent.getLongExtra(FeedAlarmScheduler.EXTRA_COOP_ID, 0L)
                val coopName = intent.getStringExtra(FeedAlarmScheduler.EXTRA_COOP_NAME) ?: "Kandang Broiler"
                val timeStr = intent.getStringExtra(FeedAlarmScheduler.EXTRA_TIME_STR) ?: "06:00"
                val slotName = intent.getStringExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME) ?: "Pagi"
                val instruction = intent.getStringExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION) ?: ""
                val ageDays = intent.getIntExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, 0)
                val phase = intent.getStringExtra(FeedAlarmScheduler.EXTRA_PHASE) ?: "Fase Brooding"
                val feedType = intent.getStringExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE) ?: "Pakan Prestarter"
                val soundId = intent.getStringExtra(FeedAlarmScheduler.EXTRA_SOUND_ID) ?: FarmAudioCatalog.getDefaultSoundForSlot(timeStr)
                val volume = intent.getFloatExtra(FeedAlarmScheduler.EXTRA_VOLUME, 1.0f)

                val alarmState = ActiveAlarmState(
                    isActive = true,
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

                createNotificationChannel()
                val notification = buildForegroundNotification(alarmState)
                startForeground(NOTIFICATION_ID, notification)

                val prefs = FeedAlarmPreferences(this)
                if (prefs.isSoundEnabled) {
                    FarmAudioManager.startAlarm(this, alarmState)
                }
            }

            ACTION_STOP_ALARM -> {
                FarmAudioManager.stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_ACTION_DONE -> {
                val cycleId = intent.getLongExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, 0L)
                val coopId = intent.getLongExtra(FeedAlarmScheduler.EXTRA_COOP_ID, 0L)
                val timeStr = intent.getStringExtra(FeedAlarmScheduler.EXTRA_TIME_STR) ?: "06:00"
                val slotName = intent.getStringExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME) ?: "Pagi"
                val instruction = intent.getStringExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION) ?: ""
                val ageDays = intent.getIntExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, 0)
                val phase = intent.getStringExtra(FeedAlarmScheduler.EXTRA_PHASE) ?: "Fase Brooding"
                val feedType = intent.getStringExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE) ?: "Pakan Prestarter"

                handleStatusSave(cycleId, coopId, timeStr, slotName, instruction, ageDays, phase, feedType, "SELESAI")
                FarmAudioManager.stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_ACTION_SNOOZE -> {
                val cycleId = intent.getLongExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, 0L)
                val coopId = intent.getLongExtra(FeedAlarmScheduler.EXTRA_COOP_ID, 0L)
                val coopName = intent.getStringExtra(FeedAlarmScheduler.EXTRA_COOP_NAME) ?: "Kandang Broiler"
                val timeStr = intent.getStringExtra(FeedAlarmScheduler.EXTRA_TIME_STR) ?: "06:00"
                val slotName = intent.getStringExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME) ?: "Pagi"
                val instruction = intent.getStringExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION) ?: ""
                val ageDays = intent.getIntExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, 0)
                val phase = intent.getStringExtra(FeedAlarmScheduler.EXTRA_PHASE) ?: "Fase Brooding"
                val feedType = intent.getStringExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE) ?: "Pakan Prestarter"
                val snoozeMinutes = intent.getIntExtra(FeedAlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)

                handleStatusSave(cycleId, coopId, timeStr, slotName, instruction, ageDays, phase, feedType, "DITUNDA", snoozeMinutes)
                FeedAlarmScheduler.scheduleSnooze(
                    context = this,
                    scheduleId = 0L,
                    cycleId = cycleId,
                    coopId = coopId,
                    coopName = coopName,
                    timeStr = timeStr,
                    slotName = slotName,
                    instruction = instruction,
                    ageDays = ageDays,
                    phase = phase,
                    feedType = feedType,
                    snoozeMinutes = snoozeMinutes
                )
                FarmAudioManager.stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun buildForegroundNotification(state: ActiveAlarmState): android.app.Notification {
        val soundItem = FarmAudioCatalog.getSoundById(state.soundId)

        // Open app pending intent
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "feed_alarm")
            putExtra("CYCLE_ID", state.cycleId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            101,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Done action
        val doneIntent = Intent(this, FeedAlarmAudioService::class.java).apply {
            action = ACTION_ACTION_DONE
            putExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, state.cycleId)
            putExtra(FeedAlarmScheduler.EXTRA_COOP_ID, state.coopId)
            putExtra(FeedAlarmScheduler.EXTRA_TIME_STR, state.timeStr)
            putExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME, state.slotName)
            putExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION, state.instruction)
            putExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, state.ageDays)
            putExtra(FeedAlarmScheduler.EXTRA_PHASE, state.phase)
            putExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE, state.feedType)
        }
        val donePendingIntent = PendingIntent.getService(
            this,
            102,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Alarm (Silence)
        val stopIntent = Intent(this, FeedAlarmAudioService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            103,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 5 Min Action
        val snoozeIntent = Intent(this, FeedAlarmAudioService::class.java).apply {
            action = ACTION_ACTION_SNOOZE
            putExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, state.cycleId)
            putExtra(FeedAlarmScheduler.EXTRA_COOP_ID, state.coopId)
            putExtra(FeedAlarmScheduler.EXTRA_COOP_NAME, state.coopName)
            putExtra(FeedAlarmScheduler.EXTRA_TIME_STR, state.timeStr)
            putExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME, state.slotName)
            putExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION, state.instruction)
            putExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, state.ageDays)
            putExtra(FeedAlarmScheduler.EXTRA_PHASE, state.phase)
            putExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE, state.feedType)
            putExtra(FeedAlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            104,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = """
            ${soundItem.iconEmoji} Suara: ${soundItem.name}
            🏠 Kandang: ${state.coopName}
            🐥 Umur: Hari ke-${state.ageDays} (${state.phase})
            🥣 Pakan: ${state.feedType}
            📋 Tugas: ${state.instruction}
        """.trimIndent()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_broiler)
            .setContentTitle("⏰ WAKTU PEMBERIAN PAKAN (${state.timeStr} - ${state.slotName})")
            .setContentText("Kandang ${state.coopName}: Saatnya pemberian pakan ayam!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(0, "✓ SUDAH DIBERIKAN", donePendingIntent)
            .addAction(0, "🔕 MATIKAN ALARM", stopPendingIntent)
            .addAction(0, "⏰ TUNDA 5 MENIT", snoozePendingIntent)
            .build()
    }

    private fun handleStatusSave(
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
            val db = AppDatabase.getDatabase(this@FeedAlarmAudioService)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi Peringatan Alarm Pakan Sejahtera Bersama"
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        FarmAudioManager.stopAlarm()
        super.onDestroy()
    }
}
