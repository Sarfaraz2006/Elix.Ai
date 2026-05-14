package com.elix.assistant.ui.settings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elix.assistant.R
import com.elix.assistant.databinding.ActivitySettingsBinding
import com.elix.assistant.model.PrimeContact
import org.json.JSONArray
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val primeContacts = mutableListOf<PrimeContact>()
    private lateinit var primeAdapter: PrimeContactAdapter

    private val models = listOf(
        "models/gemini-2.5-flash-native-audio-preview-12-2025" to "Native Audio (Human Voice)",
        "models/gemini-2.0-flash-live-001" to "Flash Live (Fast)",
        "models/gemini-2.5-flash-preview-native-audio-dialog" to "Pro Audio Dialog",
    )
    private val voices = listOf("Aoede", "Charon", "Kore", "Fenrir", "Puck", "Leda", "Orus", "Zephyr")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("elix_prefs", Context.MODE_PRIVATE)

        binding.apiKeyInput.setText(prefs.getString("api_key", "") ?: "")
        binding.apiKeyInput.setTextColor(Color.WHITE)
        binding.nameInput.setText(prefs.getString("user_name", "") ?: "")
        binding.nameInput.setTextColor(Color.WHITE)

        val modelLabels = models.map { it.second }
        val modelAdapter =
            object :
                ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, modelLabels) {
                override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                    val v = super.getView(pos, cv, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.textSize = 13f
                    v.setPadding(8, 12, 8, 12)
                    return v
                }

                override fun getDropDownView(pos: Int, cv: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(pos, cv, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.setBackgroundColor(Color.parseColor("#1A1A1A"))
                    v.setPadding(16, 16, 16, 16)
                    v.textSize = 13f
                    return v
                }
            }
        binding.modelSpinner.adapter = modelAdapter
        binding.modelSpinner.setSelection(
            models.indexOfFirst {
                it.first == prefs.getString("gemini_model", models[0].first)
            }.coerceAtLeast(0),
        )

        val voiceAdapter =
            object :
                ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, voices) {
                override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                    val v = super.getView(pos, cv, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.textSize = 13f
                    v.setPadding(8, 12, 8, 12)
                    return v
                }

                override fun getDropDownView(pos: Int, cv: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(pos, cv, parent) as TextView
                    v.setTextColor(Color.WHITE)
                    v.setBackgroundColor(Color.parseColor("#1A1A1A"))
                    v.setPadding(16, 16, 16, 16)
                    v.textSize = 13f
                    return v
                }
            }
        binding.voiceSpinner.adapter = voiceAdapter
        binding.voiceSpinner.setSelection(
            voices.indexOf(prefs.getString("gemini_voice", "Aoede")).coerceAtLeast(0),
        )

        when (prefs.getString("personality_mode", "gf")) {
            "professional" -> binding.radioGroup.check(R.id.radioProf)
            "assistant" -> binding.radioGroup.check(R.id.radioAssistant)
            else -> binding.radioGroup.check(R.id.radioGf)
        }

        loadPrimeContacts(prefs.getString("prime_contacts_json", null))
        primeAdapter = PrimeContactAdapter(primeContacts) { pos ->
            primeContacts.removeAt(pos)
            primeAdapter.notifyItemRemoved(pos)
        }
        binding.primeRecycler.layoutManager = LinearLayoutManager(this)
        binding.primeRecycler.adapter = primeAdapter
        binding.addPrimeBtn.setOnClickListener { showAddPrimeDialog() }

        val isAccessible = isAccessibilityEnabled()
        binding.accessibilityStatus.text = if (isAccessible) "Enabled" else "Disabled — Tap to enable"
        binding.accessibilityStatus.setTextColor(
            if (isAccessible) Color.parseColor("#00E676") else Color.parseColor("#FF1744"),
        )
        binding.accessibilityStatus.setOnClickListener {
            startActivity(android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.saveBtn.setOnClickListener {
            val apiKey = binding.apiKeyInput.text.toString().trim()
            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Please enter your Gemini API key!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val editor = prefs.edit()
            editor.putString("api_key", apiKey)
            editor.putString(
                "user_name",
                binding.nameInput.text.toString().trim().ifEmpty { "friend" },
            )
            editor.putString("gemini_model", models[binding.modelSpinner.selectedItemPosition].first)
            editor.putString("gemini_voice", voices[binding.voiceSpinner.selectedItemPosition])
            editor.putString(
                "personality_mode",
                when (binding.radioGroup.checkedRadioButtonId) {
                    R.id.radioProf -> "professional"
                    R.id.radioAssistant -> "assistant"
                    else -> "gf"
                },
            )

            val arr = JSONArray()
            primeContacts.forEach { c ->
                arr.put(JSONObject().apply { put("name", c.name); put("number", c.number) })
            }
            editor.putString("prime_contacts_json", arr.toString())
            editor.apply()

            Toast.makeText(this, "Settings saved! Restart app.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadPrimeContacts(json: String?) {
        primeContacts.clear()
        json ?: return
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                primeContacts.add(PrimeContact(obj.getString("name"), obj.getString("number")))
            }
        } catch (_: Exception) {
        }
    }

    private fun showAddPrimeDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_prime_contact, null)
        val nameInput = view.findViewById<EditText>(R.id.dialogNameInput)
        val numberInput = view.findViewById<EditText>(R.id.dialogNumberInput)
        AlertDialog.Builder(this)
            .setTitle("Add Prime Contact")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val number = numberInput.text.toString().trim()
                if (name.isNotEmpty() && number.isNotEmpty()) {
                    primeContacts.add(PrimeContact(name, number))
                    primeAdapter.notifyItemInserted(primeContacts.size - 1)
                } else {
                    Toast.makeText(this, "Name and number required!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/com.elix.assistant.service.AccessibilityHelperService"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        return enabled?.contains(service) == true
    }
}

class PrimeContactAdapter(
    private val contacts: MutableList<PrimeContact>,
    private val onDelete: (Int) -> Unit,
) : RecyclerView.Adapter<PrimeContactAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.primeItemName)
        val number: TextView = view.findViewById(R.id.primeItemNumber)
        val delete: ImageButton = view.findViewById(R.id.primeItemDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_prime_contact, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = contacts[position]
        holder.name.text = c.name
        holder.number.text = c.number
        holder.delete.setOnClickListener { onDelete(holder.adapterPosition) }
    }

    override fun getItemCount() = contacts.size
}
