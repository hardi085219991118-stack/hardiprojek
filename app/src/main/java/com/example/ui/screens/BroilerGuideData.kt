package com.example.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class BroilerGuide(
    val day: Int,
    val targetWeightG: Int,
    val feedG: Int,
    val waterMl: Int,
    val temp: String,
    val humidity: String,
    val light: String,
    val ventilation: String,
    val tasks: List<String>
)

object BroilerGuideData {
    private val feed = intArrayOf(0,8,12,16,20,24,28,32,38,45,52,60,68,76,84,92,100,108,116,124,132,140,148,156,164,172,180,188,196,204,212,220,228,236,244,252)
    private val weight = intArrayOf(40,60,80,105,135,170,210,260,320,390,470,560,650,750,850,960,1070,1180,1290,1400,1510,1620,1730,1840,1950,2060,2170,2280,2390,2500,2610,2720,2830,2940,3050,3160)
    fun forDay(day: Int): BroilerGuide {
        val d = day.coerceIn(0,35)
        val temp = when (d) { in 0..3 -> "32–34 °C"; in 4..7 -> "31–32 °C"; in 8..14 -> "29–31 °C"; in 15..21 -> "27–29 °C"; else -> "24–27 °C" }
        val light = when (d) { 0 -> "23–24 jam"; in 1..7 -> "20–23 jam"; in 8..14 -> "18–20 jam"; else -> "16–18 jam" }
        val vent = when (d) { in 0..7 -> "Udara segar tanpa angin langsung"; in 8..14 -> "Tingkatkan pertukaran udara bertahap"; else -> "Ventilasi aktif, cek amonia dan litter" }
        return BroilerGuide(d, weight[d], feed[d], (feed[d] * 1.8).toInt(), temp, "60–70%", light, vent,
            listOf("Periksa ayam aktif dan sebaran merata", "Pastikan pakan tersedia dan feeder bersih", "Periksa air minum dan nipple", "Periksa suhu, kelembaban dan ventilasi", "Periksa litter dan bau amonia", "Catat ayam mati/afkir serta kondisi kandang", "Lakukan biosekuriti sesuai prosedur"))
    }
    fun ageDays(chickInDate: String, now: Date = Date()): Int {
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val start = f.parse(chickInDate) ?: return 0
            TimeUnit.MILLISECONDS.toDays(now.time - start.time).toInt().coerceAtLeast(0)
        } catch (_: Exception) { 0 }
    }
}
