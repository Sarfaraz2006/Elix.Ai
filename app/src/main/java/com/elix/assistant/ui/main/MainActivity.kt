package com.elix.assistant.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.elix.assistant.R
import com.elix.assistant.ui.settings.SettingsActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ImageButton?>(R.id.settingsBtn)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}

