package com.elix.assistant.ai

import android.content.Context
import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.sqrt

class AudioEngine(private val context: Context) {
    companion object {
        private const val TAG = "AudioEngine"
        private const val MIC_SAMPLE_RATE = 16000
        private const val SPEAKER_SAMPLE_RATE = 24000
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_SIZE = 1024
    }

    var onAudioChunkReady: ((ByteArray) -> Unit)? = null
    var onAmplitudeChanged: ((Float) -> Unit)? = null
    var onSpeakingStarted: (() -> Unit)? = null
    var onSpeakingStopped: (() -> Unit)? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val playbackQueue = LinkedBlockingQueue<ByteArray>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMuted = false
    private var isSpeaking = false
    private var isRecording = false

    fun startRecording() {
        val minBuf = AudioRecord.getMinBufferSize(MIC_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, MIC_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, ENCODING, maxOf(minBuf, CHUNK_SIZE * 4)
        )
        audioRecord?.startRecording()
        isRecording = true
        scope.launch {
            val buffer = ByteArray(CHUNK_SIZE)
            while (isActive && isRecording) {
                val read = audioRecord?.read(buffer, 0, CHUNK_SIZE) ?: -1
                if (read > 0 && !isMuted && !isSpeaking) {
                    val chunk = buffer.copyOf(read)
                    onAudioChunkReady?.invoke(chunk)
                    val rms = calculateRMS(chunk)
                    withContext(Dispatchers.Main) { onAmplitudeChanged?.invoke(rms) }
                }
            }
        }
    }

    fun startPlayback() {
        val minBuf = AudioTrack.getMinBufferSize(SPEAKER_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(SPEAKER_SAMPLE_RATE)
                .setEncoding(ENCODING)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(maxOf(minBuf, CHUNK_SIZE * 8))
            .setTransferMode(AudioTrack.MODE_STREAM).build()
        audioTrack?.play()
        scope.launch {
            while (isActive) {
                val chunk = playbackQueue.poll()
                if (chunk != null) {
                    if (!isSpeaking) {
                        isSpeaking = true
                        withContext(Dispatchers.Main) { onSpeakingStarted?.invoke() }
                    }
                    audioTrack?.write(chunk, 0, chunk.size)
                } else {
                    if (isSpeaking) {
                        isSpeaking = false
                        withContext(Dispatchers.Main) { onSpeakingStopped?.invoke() }
                    }
                    delay(20)
                }
            }
        }
    }

    fun queueAudio(pcmBytes: ByteArray) { playbackQueue.offer(pcmBytes) }

    fun clearPlaybackQueue() {
        playbackQueue.clear()
        isSpeaking = false
        CoroutineScope(Dispatchers.Main).launch { onSpeakingStopped?.invoke() }
    }

    fun setMuted(muted: Boolean) { isMuted = muted }
    fun isMuted() = isMuted

    private fun calculateRMS(bytes: ByteArray): Float {
        var sum = 0.0
        val count = bytes.size / 2
        for (i in 0 until count) {
            val sample = (bytes[i * 2].toInt() or (bytes[i * 2 + 1].toInt() shl 8)).toShort().toFloat()
            sum += sample * sample
        }
        return (sqrt(sum / count).toFloat() / 32768f).coerceIn(0f, 1f)
    }

    fun release() {
        isRecording = false
        scope.cancel()
        try { audioRecord?.stop(); audioRecord?.release() } catch (e: Exception) { Log.e(TAG, "${e.message}") }
        try { audioTrack?.stop(); audioTrack?.release() } catch (e: Exception) { Log.e(TAG, "${e.message}") }
        audioRecord = null; audioTrack = null
        playbackQueue.clear()
    }
}
