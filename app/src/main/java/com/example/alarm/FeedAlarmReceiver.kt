package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FeedScheduleLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FeedAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "feed_reminder_channel"
        const val CHANNEL_NAME = "Jadwal Pemberian Pakan Broiler"
    }

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
        val snoozeMinutes = intent.getIntExtra(FeedAlarmScheduler.EXTRA_SNOOZE_MINUTES, 10)

        when (action) {
            FeedAlarmScheduler.ACTION_FEED_ALARM -> {
                showFeedingNotification(
                    context = context,
                    cycleId = cycleId,
                    coopId = coopId,
                    coopName = coopName,
                    timeStr = timeStr,
                    slotName = slotName,
                    instruction = instruction,
                    ageDays = ageDays,
                    phase = phase,
                    feedType = feedType
                )
            }
            FeedAlarmScheduler.ACTION_STATUS_DONE -> {
                handleStatusUpdate(context, cycleId, coopId, timeStr, slotName, instruction, ageDays, phase, feedType, "SELESAI")
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
                    snoozeMinutes = snoozeMinutes
                )
                dismissNotification(context, generateNotificationId(timeStr, cycleId))
            }
            FeedAlarmScheduler.ACTION_STATUS_SKIP -> {
                handleStatusUpdate(context, cycleId, coopId, timeStr, slotName, instruction, ageDays, phase, feedType, "DILEWATI")
                dismissNotification(context, generateNotificationId(timeStr, cycleId))
            }
        }
    }

    private fun showFeedingNotification(
        context: Context,
        cycleId: Long,
        coopId: Long,
        coopName: String,
        timeStr: String,
        slotName: String,
        instruction: String,
        ageDays: Int,
        phase: String,
        feedType: String
    ) {
        createNotificationChannel(context)
        val prefs = FeedAlarmPreferences(context)
        val notifId = generateNotificationId(timeStr, cycleId)

        // Open App Intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "feed_alarm")
            putExtra("CYCLE_ID", cycleId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notifId * 10 + 1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Done Action Intent
        val doneIntent = Intent(context, FeedAlarmReceiver::class.java).apply {
            action = FeedAlarmScheduler.ACTION_STATUS_DONE
            putExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, cycleId)
            putExtra(FeedAlarmScheduler.EXTRA_COOP_ID, coopId)
            putExtra(FeedAlarmScheduler.EXTRA_TIME_STR, timeStr)
            putExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME, slotName)
            putExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION, instruction)
            putExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, ageDays)
            putExtra(FeedAlarmScheduler.EXTRA_PHASE, phase)
            putExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE, feedType)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 10 + 2,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 10 Min Action Intent
        val snoozeIntent = Intent(context, FeedAlarmReceiver::class.java).apply {
            action = FeedAlarmScheduler.ACTION_STATUS_SNOOZE
            putExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, cycleId)
            putExtra(FeedAlarmScheduler.EXTRA_COOP_ID, coopId)
            putExtra(FeedAlarmScheduler.EXTRA_COOP_NAME, coopName)
            putExtra(FeedAlarmScheduler.EXTRA_TIME_STR, timeStr)
            putExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME, slotName)
            putExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION, instruction)
            putExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, ageDays)
            putExtra(FeedAlarmScheduler.EXTRA_PHASE, phase)
            putExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE, feedType)
            putExtra(FeedAlarmScheduler.EXTRA_SNOOZE_MINUTES, 10)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 10 + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Skip Action Intent
        val skipIntent = Intent(context, FeedAlarmReceiver::class.java).apply {
            action = FeedAlarmScheduler.ACTION_STATUS_SKIP
            putExtra(FeedAlarmScheduler.EXTRA_CYCLE_ID, cycleId)
            putExtra(FeedAlarmScheduler.EXTRA_COOP_ID, coopId)
            putExtra(FeedAlarmScheduler.EXTRA_TIME_STR, timeStr)
            putExtra(FeedAlarmScheduler.EXTRA_SLOT_NAME, slotName)
            putExtra(FeedAlarmScheduler.EXTRA_INSTRUCTION, instruction)
            putExtra(FeedAlarmScheduler.EXTRA_AGE_DAYS, ageDays)
            putExtra(FeedAlarmScheduler.EXTRA_PHASE, phase)
            putExtra(FeedAlarmScheduler.EXTRA_FEED_TYPE, feedType)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 10 + 4,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val bigText = """
            🏠 Kandang: $coopName
            🐥 Umur: Hari ke-$ageDays ($phase)
            🥣 Pakan: $feedType
            📋 Tugas: $instruction
        """.trimIndent()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_broiler)
            .setContentTitle("⏰ WAKTU PEMBERIAN PAKAN ($timeStr - $slotName)")
            .setContentText("Kandang $coopName (Hari ke-$ageDays): Saatnya pemberian pakan.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "✓ SUDAH DIBERIKAN", donePendingIntent)
            .addAction(0, "⏰ TUNDA 10 MNT", snoozePendingIntent)
            .addAction(0, "⚠️ LEWATI", skipPendingIntent)

        if (prefs.isSoundEnabled) {
            builder.setSound(soundUri)
        }
        if (prefs.isVibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        }

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not yet granted by user
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
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi dan Alarm Waktu Pemberian Pakan Ayam Broiler"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun generateNotificationId(timeStr: String, cycleId: Long): Int {
        val timeHash = timeStr.replace(":", "").toIntOrNull() ?: 100
        return (cycleId.toInt() * 10000) + timeHash
    }
}
