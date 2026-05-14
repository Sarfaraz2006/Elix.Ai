package com.elix.assistant.viewmodel

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.elix.assistant.ai.CommandParser

class MainViewModel : ViewModel() {
    val commandResult = MutableLiveData<String?>()

    fun executeCommand(command: CommandParser.Command, context: Context) {
        commandResult.value = null
    }

    fun acceptCall(context: Context) {
        Toast.makeText(context, "Accepting call...", Toast.LENGTH_SHORT).show()
        context.sendBroadcast(Intent("com.elix.CALL_ENDED"))
    }

    fun rejectCall(context: Context) {
        Toast.makeText(context, "Rejecting call...", Toast.LENGTH_SHORT).show()
        context.sendBroadcast(Intent("com.elix.CALL_ENDED"))
    }
}

