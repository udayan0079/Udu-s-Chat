package com.uduschat.app.core

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class VoicePlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f
)

data class RecordedVoiceNote(
    val filePath: String,
    val durationMs: Long
)

/**
 * Manages real microphone AAC voice note recording and single-track playback.
 */
class VoiceNoteManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingStartedAtMs: Long = 0L

    private var player: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _playbackState = MutableStateFlow(VoicePlaybackState())
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    private var recordingTimerJob: Job? = null

    fun startRecording(): Boolean {
        if (_isRecording.value) return false
        stopPlayback()
        return runCatching {
            val dir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            val outFile = File(dir, "vn_${System.currentTimeMillis()}.m4a")
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            recordingFile = outFile
            recordingStartedAtMs = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationMs.value = 0L
            recordingTimerJob?.cancel()
            recordingTimerJob = scope.launch {
                while (isActive && _isRecording.value) {
                    _recordingDurationMs.value = System.currentTimeMillis() - recordingStartedAtMs
                    delay(100L)
                }
            }
            true
        }.getOrElse {
            cancelRecording()
            false
        }
    }

    fun stopRecording(): RecordedVoiceNote? {
        if (!_isRecording.value) return null
        recordingTimerJob?.cancel()
        _isRecording.value = false
        val duration = (System.currentTimeMillis() - recordingStartedAtMs).coerceAtLeast(500L)
        val file = recordingFile
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recorder?.release()
        }
        recorder = null
        recordingFile = null
        _recordingDurationMs.value = 0L
        return if (file != null && file.exists() && file.length() > 0L) {
            RecordedVoiceNote(filePath = file.absolutePath, durationMs = duration)
        } else {
            null
        }
    }

    fun cancelRecording() {
        recordingTimerJob?.cancel()
        _isRecording.value = false
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        recordingFile?.delete()
        recordingFile = null
        _recordingDurationMs.value = 0L
    }

    fun togglePlayback(messageId: String, source: String?, fallbackDurationMs: Long = 0L) {
        if (source.isNullOrBlank()) return
        val current = _playbackState.value
        if (current.activeMessageId == messageId && player != null) {
            val mp = player ?: return
            if (mp.isPlaying) {
                mp.pause()
                progressJob?.cancel()
                _playbackState.value = current.copy(isPlaying = false, positionMs = mp.currentPosition.toLong())
            } else {
                mp.start()
                startProgressLoop()
                _playbackState.value = current.copy(isPlaying = true)
            }
            return
        }

        stopPlayback()
        runCatching {
            val mp = MediaPlayer().apply {
                if (source.startsWith("http://") || source.startsWith("https://")) {
                    setDataSource(source)
                } else if (source.startsWith("content://")) {
                    setDataSource(context, Uri.parse(source))
                } else {
                    setDataSource(source)
                }
                setOnPreparedListener { ready ->
                    val dur = ready.duration.takeIf { it > 0 }?.toLong() ?: fallbackDurationMs
                    applySpeed(ready, _playbackState.value.speed)
                    ready.start()
                    _playbackState.value = _playbackState.value.copy(
                        activeMessageId = messageId,
                        isPlaying = true,
                        positionMs = 0L,
                        durationMs = dur
                    )
                    startProgressLoop()
                }
                setOnCompletionListener {
                    stopPlayback()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    true
                }
                prepareAsync()
            }
            player = mp
        }.onFailure {
            stopPlayback()
        }
    }

    fun cycleSpeed() {
        val next = when (_playbackState.value.speed) {
            1f -> 1.5f
            1.5f -> 2f
            else -> 1f
        }
        _playbackState.value = _playbackState.value.copy(speed = next)
        player?.let { applySpeed(it, next) }
    }

    private fun applySpeed(mp: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val params = mp.playbackParams
                params.speed = speed
                mp.playbackParams = params
            }
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val mp = player ?: break
                if (!mp.isPlaying) break
                _playbackState.value = _playbackState.value.copy(
                    positionMs = mp.currentPosition.toLong(),
                    durationMs = mp.duration.takeIf { it > 0 }?.toLong() ?: _playbackState.value.durationMs
                )
                delay(100L)
            }
        }
    }

    fun stopPlayback() {
        progressJob?.cancel()
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        _playbackState.value = VoicePlaybackState(speed = _playbackState.value.speed)
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
