package com.elix.assistant.ui.settings

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elix.assistant.R

class SettingsActivity : AppCompatActivity() {
    private lateinit var store: AppSettingsStore
    private lateinit var primeContactsAdapter: PrimeContactsAdapter

    private val modelLabels = listOf(
        "Gemini 1.5 Flash",
        "Gemini 1.5 Pro",
    )

    private val voiceLabels = listOf(
        "Default",
        "Female",
        "Male",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        store = AppSettingsStore(this)
        val settings = store.load()

        val modelSpinner = findViewById<Spinner>(R.id.modelSpinner)
        val voiceSpinner = findViewById<Spinner>(R.id.voiceSpinner)

        val apiKeyInput = findViewById<EditText>(R.id.apiKeyInput)
        val nameInput = findViewById<EditText>(R.id.nameInput)

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

        apiKeyInput.setText(settings.apiKey)
        apiKeyInput.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        nameInput.setText(settings.userName)

        modelSpinner.setSelection(settings.modelIndex.coerceIn(0, modelLabels.lastIndex))
        voiceSpinner.setSelection(settings.voiceIndex.coerceIn(0, voiceLabels.lastIndex))

        when (settings.personality) {
            AppSettingsStore.Personality.GF -> findViewById<View>(R.id.radioGf).performClick()
            AppSettingsStore.Personality.PRO -> findViewById<View>(R.id.radioProf).performClick()
            AppSettingsStore.Personality.ASSISTANT ->
                findViewById<View>(R.id.radioAssistant).performClick()
        }

        val primeRecycler = findViewById<RecyclerView>(R.id.primeRecycler)
        primeContactsAdapter =
            PrimeContactsAdapter(
                initialItems = settings.primeContacts,
                onDelete = { primeContactsAdapter.remove(it) },
            )
        primeRecycler.layoutManager = LinearLayoutManager(this)
        primeRecycler.adapter = primeContactsAdapter

        findViewById<Button>(R.id.addPrimeBtn).setOnClickListener { showAddPrimeDialog() }

        val accessibilityStatus = findViewById<TextView>(R.id.accessibilityStatus)
        updateAccessibilityStatus(accessibilityStatus)
        accessibilityStatus.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.saveBtn).setOnClickListener {
            val personality =
                when {
                    findViewById<View>(R.id.radioGf).isSelected -> AppSettingsStore.Personality.GF
                    findViewById<View>(R.id.radioProf).isSelected ->
                        AppSettingsStore.Personality.PRO
                    else -> AppSettingsStore.Personality.ASSISTANT
                }

            store.save(
                AppSettingsStore.AppSettings(
                    apiKey = apiKeyInput.text?.toString().orEmpty(),
                    userName = nameInput.text?.toString().orEmpty(),
                    modelIndex = modelSpinner.selectedItemPosition,
                    voiceIndex = voiceSpinner.selectedItemPosition,
                    personality = personality,
                    primeContacts = primeContactsAdapter.items(),
                ),
            )
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            updateAccessibilityStatus(accessibilityStatus)
        }
    }

    private fun showAddPrimeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_prime_contact, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.dialogNameInput)
        val numberInput = dialogView.findViewById<EditText>(R.id.dialogNumberInput)

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Add Prime Contact")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", null)
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text?.toString().orEmpty().trim()
                val phoneNumber = numberInput.text?.toString().orEmpty().trim()

                var hasError = false
                if (name.isEmpty()) {
                    nameInput.error = "Required"
                    hasError = true
                }
                if (phoneNumber.isEmpty()) {
                    numberInput.error = "Required"
                    hasError = true
                }
                if (hasError) return@setOnClickListener

                primeContactsAdapter.add(PrimeContact(name = name, phoneNumber = phoneNumber))
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateAccessibilityStatus(statusView: TextView) {
        val enabled = isAccessibilityServiceEnabled()
        statusView.text =
            if (enabled) "Enabled (tap to manage)" else "Disabled (tap to enable)"
        statusView.setTextColor(if (enabled) Color.parseColor("#00E676") else Color.parseColor("#FF1744"))
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices =
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false

        val expected = "${packageName}/${com.elix.assistant.service.AccessibilityHelperService::class.java.name}"
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
