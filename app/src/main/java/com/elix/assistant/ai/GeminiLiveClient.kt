package com.elix.assistant.ai

import android.content.Context
import android.os.Handler
import android.os.Looper

class GeminiLiveClient(private val context: Context) {
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onAudioReceived: ((ByteArray) -> Unit)? = null
    var onInputTranscript: ((String) -> Unit)? = null
    var onOutputTranscript: ((String) -> Unit)? = null
    var onTurnComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isConnected = false

    fun configure(apiKey: String, model: String, voice: String, systemPrompt: String) = Unit

    fun connect() {
        if (isConnected) return
        isConnected = true
        mainHandler.post { onConnected?.invoke() }
    }

    fun disconnect() {
        if (!isConnected) return
        isConnected = false
        mainHandler.post { onDisconnected?.invoke() }
    }

    fun sendAudio(pcm: ByteArray) = Unit

    fun sendText(text: String) {
        if (!isConnected) return
        mainHandler.post {
            onInputTranscript?.invoke(text)
            onOutputTranscript?.invoke("")
            onTurnComplete?.invoke()
        }
    }

    fun sendInterrupt() = Unit
}

