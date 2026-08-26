package com.example.alarm

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class FarmSoundItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val iconEmoji: String,
    val iconVector: ImageVector,
    val description: String,
    val defaultForSlot: String? = null // e.g. "06:00", "11:00", "16:00", "20:00"
)

object FarmAudioCatalog {

    const val SOUND_ROOSTER_MORNING = "rooster_morning"
    const val SOUND_CHICKEN_CLUCK = "chicken_cluck"
    const val SOUND_CHICKEN_FEEDING = "chicken_feeding"
    const val SOUND_BARN_ACTIVE = "barn_active"
    const val SOUND_NIGHT_CALM = "night_calm"
    const val SOUND_CHICKS_CHIRP = "chicks_chirp"
    const val SOUND_FARM_BELL = "farm_bell"
    const val SOUND_DYNAMIC_ALERT = "dynamic_alert"

    val ALL_SOUNDS = listOf(
        FarmSoundItem(
            id = SOUND_ROOSTER_MORNING,
            name = "Kokok Ayam Jantan Pagi",
            subtitle = "Kokok lantang subuh 'Kukuruyuk'",
            iconEmoji = "🐓",
            iconVector = Icons.Default.WbSunny,
            description = "Suara kokok ayam jantan yang lantang dan jelas untuk membangunkan semangat feeding pagi hari.",
            defaultForSlot = "06:00"
        ),
        FarmSoundItem(
            id = SOUND_CHICKEN_FEEDING,
            name = "Suara Ayam Makan",
            subtitle = "Patukan pakan & riuh makan",
            iconEmoji = "🥣",
            iconVector = Icons.Default.Restaurant,
            description = "Suara ayam mematuk feeder beramai-ramai, menandakan waktu penambahan & perataan pakan siang.",
            defaultForSlot = "11:00"
        ),
        FarmSoundItem(
            id = SOUND_BARN_ACTIVE,
            name = "Suasana Ayam Aktif",
            subtitle = "Keramaian kandang aktif sore",
            iconEmoji = "🌾",
            iconVector = Icons.Default.Groups,
            description = "Suara riuh kawanan ayam beraktivitas sore hari untuk mengingatkan pengecekan pakan sebelum malam.",
            defaultForSlot = "16:00"
        ),
        FarmSoundItem(
            id = SOUND_NIGHT_CALM,
            name = "Suasana Kandang Tenang",
            subtitle = "Dengkur halus & kandang malam",
            iconEmoji = "🌙",
            iconVector = Icons.Default.NightsStay,
            description = "Suara suasana malam yang tenang dengan dengkur halus ayam untuk jadwal kontrol pakan malam.",
            defaultForSlot = "20:00"
        ),
        FarmSoundItem(
            id = SOUND_CHICKEN_CLUCK,
            name = "Ayam Berkotek",
            subtitle = "Kotokan khas 'Petok-petok'",
            iconEmoji = "🐔",
            iconVector = Icons.Default.VolumeUp,
            description = "Suara kotokan ayam betina yang ceria dan khas, cocok untuk jadwal siang hari."
        ),
        FarmSoundItem(
            id = SOUND_CHICKS_CHIRP,
            name = "Anak Ayam DOC",
            subtitle = "Ciap-ciap lincah anak ayam",
            iconEmoji = "🐥",
            iconVector = Icons.Default.Pets,
            description = "Suara ciap-ciap riang anak ayam DOC, sangat pas untuk masa brooding & pre-starter."
        ),
        FarmSoundItem(
            id = SOUND_FARM_BELL,
            name = "Alarm Peternakan",
            subtitle = "Dentang lonceng pakan & ayam",
            iconEmoji = "🔔",
            iconVector = Icons.Default.NotificationsActive,
            description = "Kombinasi dentang lonceng pakan peternakan dan kokok ayam yang tegas terdengar dari kejauhan."
        ),
        FarmSoundItem(
            id = SOUND_DYNAMIC_ALERT,
            name = "Alarm Dinamis Khusus",
            subtitle = "Melodi ritmis peternak modern",
            iconEmoji = "🔊",
            iconVector = Icons.Default.GraphicEq,
            description = "Nada alarm melodis modern bertema peternakan dengan ketukan ritmis yang tidak bising namun tegas."
        )
    )

    fun getSoundById(id: String?): FarmSoundItem {
        return ALL_SOUNDS.find { it.id == id } ?: ALL_SOUNDS.first()
    }

    fun getDefaultSoundForSlot(timeStr: String): String {
        return when {
            timeStr.startsWith("05") || timeStr.startsWith("06") || timeStr.startsWith("07") -> SOUND_ROOSTER_MORNING
            timeStr.startsWith("10") || timeStr.startsWith("11") || timeStr.startsWith("12") -> SOUND_CHICKEN_FEEDING
            timeStr.startsWith("14") || timeStr.startsWith("15") || timeStr.startsWith("16") || timeStr.startsWith("17") -> SOUND_BARN_ACTIVE
            timeStr.startsWith("18") || timeStr.startsWith("19") || timeStr.startsWith("20") || timeStr.startsWith("21") -> SOUND_NIGHT_CALM
            else -> SOUND_ROOSTER_MORNING
        }
    }
}
