package com.example.alarm

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Data Model dan Aturan Pakan yang BENAR-BENAR bersumber dari:
 * PANDUAN SEJAHTERA BERSAMA.pdf (Bab 2, Bab 3, Bab 4)
 */
object FeedGuideRules {

    data class PhaseDetail(
        val phaseName: String,
        val ageRange: String,
        val feedType: String,
        val frequency: String,
        val containerType: String,
        val instructions: String,
        val waterRatio: String,
        val transitionNote: String
    )

    data class StandardSlot(
        val time: String,         // "06:00", "11:00", "16:00", "20:00"
        val slotName: String,     // "Pagi", "Siang", "Sore", "Malam"
        val taskInstruction: String
    )

    // 4 Jadwal Feeding Standar dari Bab 4 Bagian 4 (Jadwal Feeding Profesional)
    val STANDARD_SLOTS = listOf(
        StandardSlot(
            time = "06:00",
            slotName = "Pagi",
            taskInstruction = "Isi feeder, Bersihkan pakan basah, Cek feed waste"
        ),
        StandardSlot(
            time = "11:00",
            slotName = "Siang",
            taskInstruction = "Tambah pakan, Ratakan pakan, Cek ayam makan"
        ),
        StandardSlot(
            time = "16:00",
            slotName = "Sore",
            taskInstruction = "Isi feeder, Pastikan cukup sampai malam"
        ),
        StandardSlot(
            time = "20:00",
            slotName = "Malam",
            taskInstruction = "Cek sisa pakan, Tambah jika perlu"
        )
    )

    /**
     * Menghitung fase pertumbuhan ayam berdasarkan umur (0 - 35 hari)
     * Sesuai Bab 2, 3, dan 4 Panduan Sejahtera Bersama.
     */
    fun getPhaseDetailForAge(ageDays: Int): PhaseDetail {
        return when {
            ageDays <= 7 -> PhaseDetail(
                phaseName = "Fase Brooding / Pre-Starter",
                ageRange = "Umur 0–7 Hari",
                feedType = "Pakan Prestarter (Butiran Halus / Crumble Kecil)",
                frequency = "5–6 kali / hari (sedikit tapi sering)",
                containerType = "Tray pakan DOC (1 tray / 50 ekor)",
                instructions = "Isi pakan 1/4 - 1/3 tray agar selalu segar dan tidak tercecer.",
                waterRatio = "1,8 – 2,0 : 1 terhadap pakan",
                transitionNote = "Pakan awal DOC berkualitas baik."
            )
            ageDays in 8..21 -> PhaseDetail(
                phaseName = "Fase Pembesaran / Starter",
                ageRange = "Umur 8–21 Hari",
                feedType = "Pakan Starter (Crumble)",
                frequency = "4–5 kali / hari",
                containerType = "Feeder gantung / feeder utama",
                instructions = "Atur ketinggian feeder sejajar punggung ayam. Pastikan debit air cukup.",
                waterRatio = "1,8 – 2,0 : 1 terhadap pakan",
                transitionNote = "Jika pergantian pakan, lakukan transisi 4 hari bertahap (75/25, 50/50, 25/75, 100%)."
            )
            else -> PhaseDetail(
                phaseName = "Fase Finisher / Akhir",
                ageRange = "Umur 22–35 Hari",
                feedType = "Pakan Finisher (Pellet / Crumble Kasar)",
                frequency = "3–4 kali / hari",
                containerType = "Feeder utama kapasitas penuh",
                instructions = "Hindari pemberian pakan saat suhu puncak (11:00-15:00) untuk mencegah heat stress.",
                waterRatio = "1,8 – 2,2 : 1 terhadap pakan",
                transitionNote = "Persiapan panen & jaga sirkulasi ventilasi maksimal."
            )
        }
    }

    /**
     * Menghitung umur ayam hari ini berdasarkan chickInDate (Format yyyy-MM-dd).
     */
    fun calculateAgeDays(chickInDateStr: String?): Int {
        if (chickInDateStr.isNullOrBlank()) return 0
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val chickInDate = sdf.parse(chickInDateStr) ?: return 0
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val diffMs = now.time - chickInDate.time
            val days = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
            days.coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Format tanggal hari ini (yyyy-MM-dd).
     */
    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Format jam sekarang (HH:mm).
     */
    fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
