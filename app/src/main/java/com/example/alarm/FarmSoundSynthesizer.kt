package com.example.alarm

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

object FarmSoundSynthesizer {

    private const val SAMPLE_RATE = 22050 // 22.05 kHz for high-speed synthesis and crisp sound
    private val random = java.util.Random(42)

    /**
     * Pastikan seluruh file audio bawaan peternakan ter-generate dan tersimpan di storage internal.
     */
    fun ensureAudioFilesExist(context: Context) {
        val soundDir = File(context.filesDir, "sounds")
        if (!soundDir.exists()) {
            soundDir.mkdirs()
        }

        FarmAudioCatalog.ALL_SOUNDS.forEach { item ->
            val targetFile = File(soundDir, "${item.id}.wav")
            if (!targetFile.exists() || targetFile.length() < 1000) {
                try {
                    val pcmData = generateSoundPcm(item.id)
                    writeWavFile(targetFile, pcmData, SAMPLE_RATE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getAudioFile(context: Context, soundId: String): File {
        val soundDir = File(context.filesDir, "sounds")
        if (!soundDir.exists()) soundDir.mkdirs()
        val file = File(soundDir, "$soundId.wav")
        if (!file.exists() || file.length() < 1000) {
            val pcmData = generateSoundPcm(soundId)
            writeWavFile(file, pcmData, SAMPLE_RATE)
        }
        return file
    }

    private fun generateSoundPcm(soundId: String): ShortArray {
        return when (soundId) {
            FarmAudioCatalog.SOUND_ROOSTER_MORNING -> generateRoosterCrow()
            FarmAudioCatalog.SOUND_CHICKEN_CLUCK -> generateChickenCluck()
            FarmAudioCatalog.SOUND_CHICKEN_FEEDING -> generateChickenFeeding()
            FarmAudioCatalog.SOUND_BARN_ACTIVE -> generateBarnActive()
            FarmAudioCatalog.SOUND_NIGHT_CALM -> generateNightCalm()
            FarmAudioCatalog.SOUND_CHICKS_CHIRP -> generateChicksChirp()
            FarmAudioCatalog.SOUND_FARM_BELL -> generateFarmBell()
            FarmAudioCatalog.SOUND_DYNAMIC_ALERT -> generateDynamicAlert()
            else -> generateRoosterCrow()
        }
    }

    // 1. 🐓 Kokok Ayam Jantan Pagi ("Ku-ku-ru-yuuuk")
    private fun generateRoosterCrow(): ShortArray {
        val totalDurationSec = 3.2
        val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        fun renderSegment(startSec: Double, durationSec: Double, startFreq: Double, endFreq: Double, vibrato: Boolean = false) {
            val startIdx = (startSec * SAMPLE_RATE).toInt()
            val count = (durationSec * SAMPLE_RATE).toInt()
            var phase = 0.0

            for (i in 0 until count) {
                val idx = startIdx + i
                if (idx >= totalSamples) break

                val progress = i.toDouble() / count
                var freq = startFreq + (endFreq - startFreq) * progress
                if (vibrato && progress > 0.2) {
                    freq += sin(2.0 * PI * 5.5 * (i.toDouble() / SAMPLE_RATE)) * 25.0
                }

                val phaseInc = 2.0 * PI * freq / SAMPLE_RATE
                phase += phaseInc

                // Harmonic sum for brassy rooster timbre
                val raw = sin(phase) +
                        0.7 * sin(2.0 * phase) +
                        0.45 * sin(3.0 * phase) +
                        0.25 * sin(4.0 * phase) +
                        0.15 * sin(5.0 * phase)

                // Amplitude envelope (attack, decay, sustain, release)
                val amp = when {
                    progress < 0.1 -> progress / 0.1
                    progress > 0.85 -> (1.0 - progress) / 0.15
                    else -> 1.0
                }

                val sampleVal = (raw / 2.5 * amp * 28000).toInt().coerceIn(-32767, 32767).toShort()
                buffer[idx] = (buffer[idx] + sampleVal).coerceIn(-32767, 32767).toShort()
            }
        }

        // Syllable 1: "Ku" (0.0 to 0.25s)
        renderSegment(0.1, 0.22, 420.0, 520.0)
        // Syllable 2: "ku" (0.4 to 0.62s)
        renderSegment(0.4, 0.24, 460.0, 600.0)
        // Syllable 3: "ru" (0.7 to 0.95s)
        renderSegment(0.72, 0.26, 560.0, 720.0)
        // Syllable 4: "yuuuuuk" (1.05 to 2.9s)
        renderSegment(1.05, 1.85, 840.0, 660.0, vibrato = true)

        return buffer
    }

    // 2. 🐔 Ayam Berkotek ("Petok-petok-petok petookk")
    private fun generateChickenCluck(): ShortArray {
        val totalDurationSec = 3.0
        val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        fun cluck(startSec: Double, durSec: Double, startF: Double, endF: Double, isLong: Boolean = false) {
            val startIdx = (startSec * SAMPLE_RATE).toInt()
            val count = (durSec * SAMPLE_RATE).toInt()
            var phase = 0.0

            for (i in 0 until count) {
                val idx = startIdx + i
                if (idx >= totalSamples) break
                val p = i.toDouble() / count
                val freq = if (isLong) {
                    if (p < 0.4) startF + (endF - startF) * (p / 0.4)
                    else endF - (endF - 320.0) * ((p - 0.4) / 0.6)
                } else {
                    startF + (endF - startF) * (p * p)
                }

                phase += 2.0 * PI * freq / SAMPLE_RATE
                val raw = sin(phase) + 0.5 * sin(2.0 * phase) + 0.3 * sin(3.0 * phase)
                val noise = (random.nextDouble() * 2.0 - 1.0) * 0.15
                val amp = if (isLong) {
                    if (p < 0.15) p / 0.15 else (1.0 - p)
                } else {
                    exp(-p * 6.0)
                }
                val sample = ((raw + noise) * amp * 27000).toInt().coerceIn(-32767, 32767).toShort()
                buffer[idx] = (buffer[idx] + sample).coerceIn(-32767, 32767).toShort()
            }
        }

        // Rhythmic clucks
        cluck(0.1, 0.14, 520.0, 310.0)
        cluck(0.3, 0.14, 540.0, 320.0)
        cluck(0.52, 0.15, 560.0, 300.0)
        cluck(0.78, 0.48, 380.0, 620.0, isLong = true)

        cluck(1.5, 0.14, 530.0, 310.0)
        cluck(1.72, 0.14, 550.0, 315.0)
        cluck(2.0, 0.55, 390.0, 640.0, isLong = true)

        return buffer
    }

    // 3. 🥣 Suara Ayam Makan (Patukan Pakan Feeder & Riuh)
    private fun generateChickenFeeding(): ShortArray {
        val totalDurationSec = 3.2
        val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Generate series of sharp rhythmic pecks
        val peckTimes = doubleArrayOf(
            0.1, 0.22, 0.35, 0.44, 0.6, 0.72, 0.81, 0.95,
            1.15, 1.28, 1.42, 1.58, 1.75, 1.9, 2.08, 2.25, 2.45, 2.65, 2.85
        )

        for (t in peckTimes) {
            val startIdx = (t * SAMPLE_RATE).toInt()
            val dur = 0.05
            val count = (dur * SAMPLE_RATE).toInt()
            val baseFreq = 850.0 + random.nextDouble() * 400.0

            for (i in 0 until count) {
                val idx = startIdx + i
                if (idx >= totalSamples) break
                val p = i.toDouble() / count
                val click = sin(2.0 * PI * baseFreq * (i.toDouble() / SAMPLE_RATE)) * exp(-p * 18.0)
                val noise = (random.nextDouble() * 2.0 - 1.0) * exp(-p * 22.0)
                val s = ((click * 0.7 + noise * 0.3) * 29000).toInt().coerceIn(-32767, 32767).toShort()
                buffer[idx] = (buffer[idx] + s).coerceIn(-32767, 32767).toShort()
            }
        }

        // Soft contentment murmur in the background
        var bgPhase = 0.0
        for (i in 0 until totalSamples) {
            bgPhase += 2.0 * PI * 340.0 / SAMPLE_RATE
            val amp = 0.12 * (sin(2.0 * PI * 1.2 * (i.toDouble() / SAMPLE_RATE)) + 1.0) / 2.0
            val murmur = (sin(bgPhase) * amp * 12000).toInt().toShort()
            buffer[i] = (buffer[i] + murmur).coerceIn(-32767, 32767).toShort()
        }

        return buffer
    }

    // 4. 🌾 Suasana Ayam Aktif Sore
    private fun generateBarnActive(): ShortArray {
        val totalDurationSec = 3.5
        val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Distant rooster + flock chatter
        val cluckOffsets = doubleArrayOf(0.15, 0.45, 0.7, 1.1, 1.4, 1.85, 2.2, 2.6, 2.9)
        for (t in cluckOffsets) {
            val startIdx = (t * SAMPLE_RATE).toInt()
            val dur = 0.12 + random.nextDouble() * 0.1
            val count = (dur * SAMPLE_RATE).toInt()
            val startF = 440.0 + random.nextDouble() * 200.0
            var phase = 0.0

            for (i in 0 until count) {
                val idx = startIdx + i
                if (idx >= totalSamples) break
                val p = i.toDouble() / count
                phase += 2.0 * PI * (startF - p * 150.0) / SAMPLE_RATE
                val s = ((sin(phase) + 0.4 * sin(2.0 * phase)) * exp(-p * 5.0) * 19000).toInt().toShort()
                buffer[idx] = (buffer[idx] + s).coerceIn(-32767, 32767).toShort()
            }
        }

        // Flutter / Wing flap swooshes
        val flapTimes = doubleArrayOf(0.8, 2.0)
        for (ft in flapTimes) {
            val startIdx = (ft * SAMPLE_RATE).toInt()
            val count = (0.28 * SAMPLE_RATE).toInt()
            for (i in 0 until count) {
                val idx = startIdx + i
                if (idx >= totalSamples) break
                val p = i.toDouble() / count
                val noise = (random.nextDouble() * 2.0 - 1.0) * sin(PI * p) * 14000
                buffer[idx] = (buffer[idx] + noise.toInt().toShort()).coerceIn(-32767, 32767).toShort()
            }
        }

        return buffer
    }

    // 5. 🌙 Suasana Kandang Tenang Malam
    private fun generateNightCalm(): ShortArray {
        val totalDurationSec = 3.5
        val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Warm, soothing low-frequency gentle cluck & night crickets
        var cricketPhase = 0.0
        var dronePhase = 0.0

        for (i in 0 until totalSamples) {
            val time = i.toDouble() / SAMPLE_RATE

            // Cricket chirps pulsed at 4.2 kHz
            cricketPhase += 2.0 * PI * 4200.0 / SAMPLE_RATE
            val cricketPulse = (sin(2.0 * PI * 3.5 * time) > 0.4)
            val cricketVal = if (cricketPulse) sin(cricketPhase) * 4500.0 else 0.0

            // Gentle low-frequency warm purr
            dronePhase += 2.0 * PI * 210.0 / SAMPLE_RATE
            val droneAmp = (sin(2.0 * PI * 0.6 * time) + 1.0) / 2.0
            val droneVal = (sin(dronePhase) + 0.3 * sin(2.0 * dronePhase)) * droneAmp * 12000.0

            val sample = (cricketVal + droneVal).toInt().coerceIn(-32767, 32767).toShort()
            buffer[i] = sample
        }

        return buffer
    }

    // 6. 🐥 Anak Ayam DOC ("Ciap-ciap-ciap")
    private fun generateChicksChirp(): ShortArray {
        val totalDurationSec = 2.8
        val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val chirpTimes = doubleArrayOf(0.1, 0.28, 0.48, 0.8, 0.98, 1.18, 1.6, 1.78, 2.1, 2.3)
        for (t in chirpTimes) {
            val startIdx = (t * SAMPLE_RATE).toInt()
            val dur = 0.08
            val count = (dur * SAMPLE_RATE).toInt()
            var phase = 0.0
            val startF = 2600.0 + random.nextDouble() * 300.0
            val endF = 3800.0 + random.nextDouble() * 300.0

            for (i in 0 until count) {
                val idx = startIdx + i
                if (idx >= totalSamples) break
                val p = i.toDouble() / count
                val freq = startF + (endF - startF) * p
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val amp = sin(PI * p)
                val s = ((sin(phase) + 0.3 * sin(2.0 * phase)) * amp * 28000).toInt().coerceIn(-32767, 32767).toShort()
                buffer[idx] = (buffer[idx] + s).coerceIn(-32767, 32767).toShort()
            }
        }

        return buffer
    }

    // 7. 🔔 Alarm Lonceng Peternakan & Kokok
    private fun generateFarmBell(): ShortArray {
        val totalDurationSec = 3.2
        val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val strikeTimes = doubleArrayOf(0.05, 1.2)
        for (st in strikeTimes) {
            val startIdx = (st * SAMPLE_RATE).toInt()
            val dur = 1.6
            val count = (dur * SAMPLE_RATE).toInt()
            val f0 = 784.0 // G5

            for (i in 0 until count) {
                val idx = startIdx + i
                if (idx >= totalSamples) break
                val t = i.toDouble() / SAMPLE_RATE

                // Bell modal frequencies
                val m1 = sin(2.0 * PI * f0 * 1.0 * t) * exp(-t * 2.5)
                val m2 = sin(2.0 * PI * f0 * 2.76 * t) * exp(-t * 4.0) * 0.6
                val m3 = sin(2.0 * PI * f0 * 5.40 * t) * exp(-t * 7.0) * 0.35
                val m4 = sin(2.0 * PI * f0 * 8.93 * t) * exp(-t * 11.0) * 0.2

                val sample = ((m1 + m2 + m3 + m4) / 2.1 * 29000).toInt().coerceIn(-32767, 32767).toShort()
                buffer[idx] = (buffer[idx] + sample).coerceIn(-32767, 32767).toShort()
            }
        }

        return buffer
    }

    // 8. 🔊 Alarm Dinamis Khusus (Melodi Peternak Modern)
    private fun generateDynamicAlert(): ShortArray {
        val totalDurationSec = 2.6
        val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Arpeggio notes: C5 (523Hz), E5 (659Hz), G5 (784Hz), C6 (1046Hz)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteDur = 0.22

        for (n in 0 until 2) { // 2 passes
            val passOffset = n * 1.1
            for (idx in notes.indices) {
                val startIdx = ((passOffset + idx * noteDur) * SAMPLE_RATE).toInt()
                val count = (noteDur * 1.3 * SAMPLE_RATE).toInt()
                val freq = notes[idx]
                var phase = 0.0

                for (i in 0 until count) {
                    val bufIdx = startIdx + i
                    if (bufIdx >= totalSamples) break
                    val p = i.toDouble() / count
                    phase += 2.0 * PI * freq / SAMPLE_RATE
                    val raw = sin(phase) + 0.4 * sin(2.0 * phase) + 0.2 * sin(3.0 * phase)
                    val amp = if (p < 0.1) p / 0.1 else exp(-p * 3.5)
                    val sample = (raw * amp * 27000).toInt().coerceIn(-32767, 32767).toShort()
                    buffer[bufIdx] = (buffer[bufIdx] + sample).coerceIn(-32767, 32767).toShort()
                }
            }
        }

        return buffer
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val byteData = ByteArray(pcmData.size * 2)
        ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmData)

        val totalDataLen = byteData.size + 36
        val byteRate = sampleRate * 1 * 2 // 1 channel, 16 bit (2 bytes)

        FileOutputStream(file).use { out ->
            // RIFF header
            out.write("RIFF".toByteArray())
            out.write(intToByteArray(totalDataLen))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToByteArray(16)) // 16 for PCM
            out.write(shortToByteArray(1)) // AudioFormat (1 = PCM)
            out.write(shortToByteArray(1)) // Channels (1 = Mono)
            out.write(intToByteArray(sampleRate))
            out.write(intToByteArray(byteRate))
            out.write(shortToByteArray(2)) // BlockAlign (Channels * BitsPerSample / 8)
            out.write(shortToByteArray(16)) // BitsPerSample
            out.write("data".toByteArray())
            out.write(intToByteArray(byteData.size))
            out.write(byteData)
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}
