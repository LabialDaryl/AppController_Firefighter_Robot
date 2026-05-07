package com.firefighter.robotcontroller

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

/**
 * Main control activity for the Firefighter Robot.
 * Updated UI with Extinguish, Sirens, and Standby controls.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var connectionStatusText: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var modeToggleButton: MaterialButton
    
    // Drive Buttons (Left Panel)
    private lateinit var btnForward: ImageButton
    private lateinit var btnBackward: ImageButton
    
    // Steering Buttons (Right Panel)
    private lateinit var btnTurnLeft: ImageButton
    private lateinit var btnTurnRight: ImageButton

    // Control Buttons (Center Panel)
    private lateinit var extinguishButton: MaterialButton
    private lateinit var sirenButton: MaterialButton
    private lateinit var standbyButton: MaterialButton

    private var extinguishActive = false
    private var sirenActive = false
    private var isAutoMode = false
    
    private var activeCommand: String = "S"
    private val handler = Handler(Looper.getMainLooper())
    private val repeatInterval = 100L 

    private val commandRepeater = object : Runnable {
        override fun run() {
            if (!isAutoMode) {
                WifiRobotManager.send(activeCommand)
            }
            handler.postDelayed(this, repeatInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        bindViews()
        setupListeners()
        initConnection()
        
        handler.post(commandRepeater)
    }

    private fun bindViews() {
        connectionStatusText = findViewById(R.id.connectionStatusText)
        settingsButton       = findViewById(R.id.settingsButton)
        modeToggleButton     = findViewById(R.id.modeToggleButton)
        
        btnForward           = findViewById(R.id.btnForward)
        btnBackward          = findViewById(R.id.btnBackward)
        
        btnTurnLeft          = findViewById(R.id.btnTurnLeft)
        btnTurnRight         = findViewById(R.id.btnTurnRight)

        extinguishButton     = findViewById(R.id.extinguishButton)
        sirenButton          = findViewById(R.id.sirenButton)
        standbyButton        = findViewById(R.id.standbyButton)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        modeToggleButton.setOnClickListener {
            showModeSwitchConfirmation()
        }

        setupRepeatableTouch(btnForward, "F")
        setupRepeatableTouch(btnBackward, "B")
        setupRepeatableTouch(btnTurnLeft, "L")
        setupRepeatableTouch(btnTurnRight, "R")

        extinguishButton.setOnClickListener {
            if (isAutoMode) return@setOnClickListener
            extinguishActive = !extinguishActive
            WifiRobotManager.send(if (extinguishActive) "EXT_ON" else "EXT_OFF")
            updateButtonState(extinguishButton, extinguishActive, "🔥 EXTINGUISH")
        }

        sirenButton.setOnClickListener {
            if (isAutoMode) return@setOnClickListener
            sirenActive = !sirenActive
            WifiRobotManager.send(if (sirenActive) "SIREN_ON" else "SIREN_OFF")
            updateButtonState(sirenButton, sirenActive, "🚨 SIRENS")
        }

        standbyButton.setOnClickListener {
            if (isAutoMode) return@setOnClickListener
            // Standby sends a command to reset everything to safe state
            WifiRobotManager.send("STANDBY")
            
            // Reset local UI states
            extinguishActive = false
            sirenActive = false
            updateButtonState(extinguishButton, false, "🔥 EXTINGUISH")
            updateButtonState(sirenButton, false, "🚨 SIRENS")
            
            // Visual feedback for standby press
            standbyButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green_active)
            handler.postDelayed({
                standbyButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fire_orange)
            }, 500)
        }
    }

    private fun showModeSwitchConfirmation() {
        val targetMode = if (isAutoMode) getString(R.string.mode_manual) else getString(R.string.mode_auto)
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_mode_change_title)
            .setMessage(getString(R.string.confirm_mode_change_message, targetMode))
            .setPositiveButton(R.string.confirm_yes) { _, _ -> toggleMode() }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }

    private fun toggleMode() {
        isAutoMode = !isAutoMode
        if (isAutoMode) {
            WifiRobotManager.send("MODE_AUTO")
            modeToggleButton.text = getString(R.string.mode_auto)
            modeToggleButton.setIconResource(R.drawable.ic_toggle_auto)
            setAllControlsEnabled(false)
        } else {
            WifiRobotManager.send("MODE_MANUAL")
            modeToggleButton.text = getString(R.string.mode_manual)
            modeToggleButton.setIconResource(R.drawable.ic_toggle_manual)
            setAllControlsEnabled(true)
        }
    }

    private fun setAllControlsEnabled(enabled: Boolean) {
        val buttons = arrayOf(
            btnForward, btnBackward, btnTurnLeft, btnTurnRight, 
            extinguishButton, sirenButton, standbyButton
        )
        for (view in buttons) {
            view.alpha = if (enabled) 1.0f else 0.4f
            view.isEnabled = enabled
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRepeatableTouch(view: View, command: String) {
        view.setOnTouchListener { _, event ->
            if (isAutoMode) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    activeCommand = command
                    WifiRobotManager.send(command) // Instant send on press
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    activeCommand = "S"
                    WifiRobotManager.send("S") // Instant stop on release
                }
            }
            true 
        }
    }

    private fun updateButtonState(button: MaterialButton, active: Boolean, baseText: String) {
        button.backgroundTintList = ContextCompat.getColorStateList(this, if (active) R.color.green_active else R.color.red_inactive)
        button.text = if (active) "$baseText ✔" else baseText
    }

    private fun initConnection() {
        handler.post(object : Runnable {
            override fun run() {
                updateConnectionStatus()
                handler.postDelayed(this, 1500)
            }
        })
    }

    private fun updateConnectionStatus() {
        if (WifiRobotManager.isConnected()) {
            connectionStatusText.text = "CONNECTED"
            connectionStatusText.setTextColor(ContextCompat.getColor(this, R.color.green_active))
        } else {
            connectionStatusText.text = "OFFLINE"
            connectionStatusText.setTextColor(ContextCompat.getColor(this, R.color.fire_orange))
            WifiRobotManager.connect()
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("robot_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("ip", "192.168.4.1") ?: "192.168.4.1"
        val port = prefs.getInt("port", 80)
        WifiRobotManager.updateConfig(ip, port)
        WifiRobotManager.connect()
    }
}
