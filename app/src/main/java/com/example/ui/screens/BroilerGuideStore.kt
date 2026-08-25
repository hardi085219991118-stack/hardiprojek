package com.example.ui.screens

import android.content.Context

object BroilerGuideStore {
    private fun key(cycleId: Long, date: String, item: String) = "guide_${cycleId}_${date}_${item.hashCode()}"
    fun isChecked(context: Context, cycleId: Long, date: String, item: String): Boolean = context.getSharedPreferences("broiler_guide", Context.MODE_PRIVATE).getBoolean(key(cycleId,date,item), false)
    fun setChecked(context: Context, cycleId: Long, date: String, item: String, checked: Boolean) = context.getSharedPreferences("broiler_guide", Context.MODE_PRIVATE).edit().putBoolean(key(cycleId,date,item), checked).apply()
}
