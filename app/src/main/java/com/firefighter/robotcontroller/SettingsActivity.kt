package com.firefighter.robotcontroller

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Settings screen. Handles saving the robot address.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var ipEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var saveButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.settings_title)
        }

        ipEditText = findViewById(R.id.ipEditText)
        portEditText = findViewById(R.id.portEditText)
        saveButton = findViewById(R.id.saveButton)

        val prefs = getSharedPreferences("robot_prefs", Context.MODE_PRIVATE)
        ipEditText.setText(prefs.getString("ip", "192.168.4.1"))
        portEditText.setText(prefs.getInt("port", 80).toString())

        saveButton.setOnClickListener {
            val ip = ipEditText.text.toString()
            val portStr = portEditText.text.toString()
            val port = portStr.toIntOrNull() ?: 80

            prefs.edit().apply {
                putString("ip", ip)
                putInt("port", port)
                apply()
            }

            WifiRobotManager.updateConfig(ip, port)
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
