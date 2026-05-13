package com.elix.assistant.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.elix.assistant.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val modelLabels = listOf(
            "Gemini 1.5 Flash",
            "Gemini 1.5 Pro",
        )
        val voiceLabels = listOf(
            "Default",
            "Female",
            "Male",
        )

        val modelSpinner = findViewById<Spinner>(R.id.modelSpinner)
        val voiceSpinner = findViewById<Spinner>(R.id.voiceSpinner)

        val modelAdapter =
            object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, modelLabels) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.setPadding(12, 12, 12, 12)
                    return v
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup,
                ): View {
                    val v = super.getDropDownView(position, convertView, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.setBackgroundColor(Color.parseColor("#111111"))
                    v.setPadding(12, 16, 12, 16)
                    return v
                }
            }
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSpinner.adapter = modelAdapter
        modelSpinner.setBackgroundColor(Color.parseColor("#111111"))

        val voiceAdapter =
            object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, voiceLabels) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.setPadding(12, 12, 12, 12)
                    return v
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup,
                ): View {
                    val v = super.getDropDownView(position, convertView, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.setBackgroundColor(Color.parseColor("#111111"))
                    v.setPadding(12, 16, 12, 16)
                    return v
                }
            }
        voiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        voiceSpinner.adapter = voiceAdapter
        voiceSpinner.setBackgroundColor(Color.parseColor("#111111"))
    }
}
