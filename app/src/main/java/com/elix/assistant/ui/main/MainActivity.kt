package com.elix.assistant.ui.main

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.elix.assistant.R
import com.elix.assistant.ai.AudioEngine
import com.elix.assistant.ai.CommandParser
import com.elix.assistant.ai.GeminiLiveClient
import com.elix.assistant.databinding.ActivityMainBinding
import com.elix.assistant.model.ChatMessage
import com.elix.assistant.service.CallMonitorService
import com.elix.assistant.service.ElixOverlayService
import com.elix.assistant.ui.settings.SettingsActivity
import com.elix.assistant.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var geminiLive: GeminiLiveClient
    private lateinit var audioEngine: AudioEngine
    private lateinit var chatAdapter: ChatAdapter

    private val inputBuffer = StringBuilder()
    private val outputBuffer = StringBuilder()
    private var isMuted = false
    private var isInCallMode = false
    private val handler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val callEndedReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            isInCallMode = false
            setActiveMode(false)
        }
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ANSWER_PHONE_CALLS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.parseColor("#050505")
        checkPermissions()
        initViews()
        startSystemServices()
        startStatusUpdates()
        registerReceiver(callEndedReceiver, IntentFilter("com.elix.CALL_ENDED"))
        handler.postDelayed({ initGeminiLive() }, 500)
        handleIncomingCallIntent(intent)
    }

    private fun initViews() {
        chatAdapter = ChatAdapter()
        binding.chatRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply { stackFromEnd = true }
            adapter = chatAdapter
        }
        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        isMuted = false
        binding.micButton.setImageResource(R.drawable.ic_mic_on)
        binding.micButton.setOnClickListener {
            isMuted = !isMuted
            if (::audioEngine.isInitialized) audioEngine.setMuted(isMuted)
            binding.micButton.setImageResource(if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on)
            binding.statusText.text = if (isMuted) "Muted" else "Sun rahi hoon..."
        }
        binding.micButton.setOnLongClickListener {
            if (::audioEngine.isInitialized) audioEngine.clearPlaybackQueue()
            if (::geminiLive.isInitialized) geminiLive.sendInterrupt()
            binding.orbView.startListening()
            binding.statusText.text = "Sun rahi hoon..."
            true
        }
        viewModel.commandResult.observe(this) { result ->
            if (result != null && ::geminiLive.isInitialized) {
                geminiLive.sendText(result)
                viewModel.commandResult.value = null
            }
        }
    }

    private fun initGeminiLive() {
        val prefs = getSharedPreferences("elix_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        val model = prefs.getString("gemini_model", "models/gemini-2.5-flash-native-audio-preview-12-2025") ?: ""
        val voice = prefs.getString("gemini_voice", "Aoede") ?: "Aoede"
        val userName = prefs.getString("user_name", "friend") ?: "friend"
        val personality = prefs.getString("personality_mode", "gf") ?: "gf"

        if (apiKey.isEmpty()) {
            binding.statusText.text = "API Key missing! Go to Settings."
            binding.orbView.setIdle()
            Toast.makeText(this, "Please add Gemini API key in Settings!", Toast.LENGTH_LONG).show()
            return
        }

        geminiLive = GeminiLiveClient(this)
        audioEngine = AudioEngine(this)
        geminiLive.configure(apiKey, model, voice, buildSystemPrompt(userName, personality))

        geminiLive.onConnected = {
            runOnUiThread {
                binding.statusText.text = "Sun rahi hoon..."
                binding.orbView.startListening()
            }
            audioEngine.startRecording()
            audioEngine.startPlayback()
            handler.postDelayed({ geminiLive.sendText(buildGreeting(userName, personality)) }, 800)
        }

        geminiLive.onDisconnected = {
            runOnUiThread {
                binding.statusText.text = "Reconnecting..."
                binding.orbView.setIdle()
            }
        }

        geminiLive.onAudioReceived = { pcm -> audioEngine.queueAudio(pcm) }
        geminiLive.onInputTranscript = { text -> inputBuffer.append(text) }
        geminiLive.onOutputTranscript = { text -> outputBuffer.append(text) }

        geminiLive.onTurnComplete = {
            val userText = inputBuffer.toString().trim()
            val elixText = outputBuffer.toString().trim()
            inputBuffer.clear(); outputBuffer.clear()
            runOnUiThread {
                if (userText.isNotEmpty()) chatAdapter.addMessage(ChatMessage(userText, true))
                if (elixText.isNotEmpty()) {
                    chatAdapter.addMessage(ChatMessage(elixText, false))
                    binding.chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            if (userText.isNotEmpty()) {
                CommandParser.parse(userText)?.let { viewModel.executeCommand(it, this) }
            }
        }

        geminiLive.onError = { msg ->
            runOnUiThread {
                binding.statusText.text = "Error: $msg"
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

        audioEngine.onAudioChunkReady = { chunk ->
            if (!isInCallMode && !isMuted && ::geminiLive.isInitialized) {
                geminiLive.sendAudio(chunk)
            }
        }

        audioEngine.onSpeakingStarted = {
            runOnUiThread {
                binding.orbView.startSpeaking()
                binding.statusText.text = "Bol rahi hoon..."
                binding.waveformView.startAnimation()
                setActiveMode(true)
            }
        }

        audioEngine.onSpeakingStopped = {
            runOnUiThread {
                binding.orbView.startListening()
                binding.statusText.text = "Sun rahi hoon..."
                binding.waveformView.stopAnimation()
                setActiveMode(false)
            }
        }

        audioEngine.onAmplitudeChanged = { rms ->
            runOnUiThread {
                binding.waveformView.setAmplitude(rms)
                binding.orbView.setAmplitude(rms)
            }
        }

        geminiLive.connect()
    }

    private fun buildSystemPrompt(userName: String, personality: String): String {
        val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val block = when (personality) {
            "professional" -> "You are Elix, a professional AI assistant. Formal English. Max 2 sentences."
            "assistant" -> "You are Elix, a friendly assistant. Hinglish or English. Max 2-3 sentences."
            else -> "You are Elix, a caring AI girlfriend. Hinglish naturally. Warm tone. Use: haan, acha, bilkul. Max 2-3 sentences."
        }
        return "$block\nUser name: $userName\nDate: $date\nSpeak naturally and conversationally."
    }

    private fun buildGreeting(userName: String, personality: String) = when (personality) {
        "professional" -> "Good day $userName. Elix is ready."
        "assistant" -> "Hello $userName! Main Elix hoon. Kaise help karun?"
        else -> "Hey $userName! Main aa gayi hoon. Kya chal raha hai?"
    }

    private fun setActiveMode(active: Boolean) {
        val overlay = binding.redOverlay
        if (active) {
            overlay.visibility = View.VISIBLE
            overlay.startAnimation(AlphaAnimation(0f, 0.08f).apply { duration = 300; fillAfter = true })
        } else {
            overlay.startAnimation(AlphaAnimation(0.08f, 0f).apply { duration = 500; fillAfter = true })
        }
    }

    fun announceCall(callerName: String) {
        isInCallMode = true
        binding.orbView.startSpeaking()
        binding.statusText.text = "Incoming call..."
        if (::geminiLive.isInitialized) {
            geminiLive.sendText("$callerName ka call aa raha hai. Uthao ya reject karun?")
        }
        handler.postDelayed({ startCallDecisionSTT() }, 4500)
    }

    private fun startCallDecisionSTT() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.lowercase() ?: ""
                if (text.containsAny("uthao", "haan", "accept", "yes")) viewModel.acceptCall(this@MainActivity)
                else if (text.containsAny("reject", "nahi", "no", "band")) viewModel.rejectCall(this@MainActivity)
                isInCallMode = false; setActiveMode(false)
            }
            override fun onError(error: Int) { isInCallMode = false }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        })
    }

    private fun startStatusUpdates() {
        val r = object : Runnable {
            override fun run() { updateStatusBar(); handler.postDelayed(this, 30_000) }
        }
        handler.post(r)
    }

    private fun updateStatusBar() {
        binding.timeText.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            binding.batteryText.text = "🔋 ${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            binding.ramText.text = "RAM: ${mi.availMem / (1024 * 1024)}MB"
        } catch (e: Exception) { }
    }

    private fun startSystemServices() {
        listOf(ElixOverlayService::class.java, CallMonitorService::class.java).forEach {
            val intent = Intent(this, it)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        }
    }

    private fun checkPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIncomingCallIntent(intent) }
    private fun handleIncomingCallIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("INCOMING_CALL", false) == true) {
            handler.postDelayed({ announceCall(intent.getStringExtra("CALLER_NAME") ?: "Unknown") }, 500)
        }
    }

    override fun onPause() { super.onPause(); if (::audioEngine.isInitialized) audioEngine.setMuted(true) }
    override fun onResume() { super.onResume(); if (!isMuted && ::audioEngine.isInitialized) audioEngine.setMuted(false) }
    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(callEndedReceiver) } catch (e: Exception) {}
        if (::geminiLive.isInitialized) geminiLive.disconnect()
        if (::audioEngine.isInitialized) audioEngine.release()
        speechRecognizer?.destroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun String.containsAny(vararg k: String) = k.any { this.contains(it) }
}
