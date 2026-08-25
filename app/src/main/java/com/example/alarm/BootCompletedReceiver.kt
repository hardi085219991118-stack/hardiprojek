package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Device rebooted. Rescheduling feeding alarms...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val cycles = db.farmDao().getAllCyclesDirect()
                    val activeCycle = cycles.find { it.status == "ACTIVE" }
                    if (activeCycle != null) {
                        val coop = db.farmDao().getCoopById(activeCycle.coopId)
                        val coopName = coop?.name ?: "Kandang Broiler"
                        val ageDays = FeedGuideRules.calculateAgeDays(activeCycle.chickInDate)

                        FeedAlarmScheduler.scheduleAllDailySlots(
                            context = context,
                            cycleId = activeCycle.id,
                            coopId = activeCycle.coopId,
                            coopName = coopName,
                            ageDays = ageDays
                        )
                        Log.d(TAG, "Successfully rescheduled alarms for cycle ${activeCycle.cycleNumber}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling on boot: ${e.message}", e)
                }
            }
        }
    }
}
