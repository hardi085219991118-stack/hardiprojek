package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream

data class ActiveAlarmState(
    val isActive: Boolean = false,
    val scheduleId: Long = 0L,
    val cycleId: Long = 0L,
    val coopId: Long = 0L,
    val coopName: String = "",
    val timeStr: String = "",
    val slotName: String = "",
    val instruction: String = "",
    val ageDays: Int = 0,
    val phase: String = "",
    val feedType: String = "",
    val soundId: String = FarmAudioCatalog.SOUND_ROOSTER_MORNING,
    val volume: Float = 1.0f
)

object FarmAudioManager {
    private const val TAG = "FarmAudioManager"

    private var previewMediaPlayer: MediaPlayer? = null
    private var alarmMediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val _previewSoundId = MutableStateFlow<String?>(null)
    val previewSoundId: StateFlow<String?> = _previewSoundId.asStateFlow()

    private val _activeAlarmState = MutableStateFlow(ActiveAlarmState())
    val activeAlarmState: StateFlow<ActiveAlarmState> = _activeAlarmState.asStateFlow()

    /**
     * Putar pratinjau audio satu kali (preview).
     */
    @Synchronized
    fun playPreview(
        context: Context,
        soundId: String,
        volume: Float = 1.0f,
        onCompletion: (() -> Unit)? = null
    ) {
        stopPreview()
        stopAlarm()

        try {
            val audioFile = FarmSoundSynthesizer.getAudioFile(context, soundId)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                FileInputStream(audioFile).use { fis ->
                    setDataSource(fis.fd)
                }
                setVolume(volume.coerceIn(0.1f, 1.0f), volume.coerceIn(0.1f, 1.0f))
                isLooping = false
                setOnCompletionListener {
                    stopPreview()
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Preview MediaPlayer error: what=$what extra=$extra")
                    stopPreview()
                    true
                }
                prepare()
                start()
            }
            previewMediaPlayer = mp
            _previewSoundId.value = soundId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio preview: ${e.message}", e)
            stopPreview()
        }
    }

    /**
     * Hentikan pratinjau audio.
     */
    @Synchronized
    fun stopPreview() {
        try {
            previewMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping preview: ${e.message}")
        } finally {
            previewMediaPlayer = null
            _previewSoundId.value = null
        }
    }

    fun isPreviewPlaying(soundId: String): Boolean {
        return _previewSoundId.value == soundId
    }

    /**
     * Jalankan alarm berulang (looping) dengan audio khusus pakan sampai pengguna mematikan.
     */
    @Synchronized
    fun startAlarm(
        context: Context,
        alarmState: ActiveAlarmState
    ) {
        stopPreview()
        stopAlarm()

        acquireWakeLock(context)
        requestAudioFocus(context)

        try {
            val audioFile = FarmSoundSynthesizer.getAudioFile(context, alarmState.soundId)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                FileInputStream(audioFile).use { fis ->
                    setDataSource(fis.fd)
                }
                val vol = alarmState.volume.coerceIn(0.2f, 1.0f)
                setVolume(vol, vol)
                isLooping = true
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Alarm MediaPlayer error: what=$what extra=$extra")
                    stopAlarm()
                    true
                }
                prepare()
                start()
            }
            alarmMediaPlayer = mp
            _activeAlarmState.value = alarmState.copy(isActive = true)
            Log.d(TAG, "Alarm audio started looping with sound: ${alarmState.soundId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm audio: ${e.message}", e)
            stopAlarm()
        }
    }

    /**
     * Hentikan alarm pakan yang sedang berbunyi.
     */
    @Synchronized
    fun stopAlarm() {
        try {
            alarmMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm: ${e.message}")
        } finally {
            alarmMediaPlayer = null
            _activeAlarmState.value = _activeAlarmState.value.copy(isActive = false)
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock(context: Context) {
        try {
            if (wakeLock == null) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SejahteraBersama:FeedAlarmWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(3 * 60 * 1000L) // Max 3 mins wake lock
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock: ${e.message}")
        }
    }

    private fun requestAudioFocus(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus: ${e.message}")
        }
    }
}
