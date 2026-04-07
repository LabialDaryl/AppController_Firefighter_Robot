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
 * Uses UDP communication with a command-repeating mechanism for steady movement.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var connectionStatusText: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var modeToggleButton: MaterialButton
    
    // Drive Buttons (Left Panel)
    private lateinit var btnForward: ImageButton
    private lateinit var btnBackward: ImageButton
    
    // Steering/Nozzle Buttons (Right Panel)
    private lateinit var btnTurnLeft: ImageButton
    private lateinit var btnTurnRight: ImageButton
    private lateinit var btnNozzleUp: ImageButton
    private lateinit var btnNozzleDown: ImageButton

    // Control Buttons (Center Panel)
    private lateinit var pumpButton: MaterialButton
    private lateinit var lightsButton: MaterialButton
    private lateinit var hornButton: MaterialButton

    private var pumpActive = false
    private var lightsActive = false
    private var isAutoMode = false
    
    // Command repeating state
    private var activeCommand: String = "S"
    private val handler = Handler(Looper.getMainLooper())
    private val repeatInterval = 100L 

    private val commandRepeater = object : Runnable {
        override fun run() {
            // Only send movement commands if in Manual Mode
            if (!isAutoMode) {
                if (activeCommand != "S" || WifiRobotManager.isConnected()) {
                    WifiRobotManager.send(activeCommand)
                }
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
        btnNozzleUp          = findViewById(R.id.btnNozzleUp)
        btnNozzleDown        = findViewById(R.id.btnNozzleDown)

        pumpButton           = findViewById(R.id.pumpButton)
        lightsButton         = findViewById(R.id.lightsButton)
        hornButton           = findViewById(R.id.hornButton)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        modeToggleButton.setOnClickListener {
            showModeSwitchConfirmation()
        }

        // Movement Controls
        setupRepeatableTouch(btnForward, "F")
        setupRepeatableTouch(btnBackward, "B")
        setupRepeatableTouch(btnTurnLeft, "L")
        setupRepeatableTouch(btnTurnRight, "R")
        setupRepeatableTouch(btnNozzleUp, "NU")
        setupRepeatableTouch(btnNozzleDown, "ND")

        pumpButton.setOnClickListener {
            if (isAutoMode) return@setOnClickListener
            pumpActive = !pumpActive
            WifiRobotManager.send(if (pumpActive) "PUMP_ON" else "PUMP_OFF")
            updateButtonState(pumpButton, pumpActive, "💧 PUMP")
        }

        lightsButton.setOnClickListener {
            if (isAutoMode) return@setOnClickListener
            lightsActive = !lightsActive
            WifiRobotManager.send(if (lightsActive) "LIGHT_ON" else "LIGHT_OFF")
            updateButtonState(lightsButton, lightsActive, "💡 LIGHTS")
        }

        hornButton.setOnTouchListener { _, event ->
            if (isAutoMode) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    WifiRobotManager.send("HORN_ON")
                    hornButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.safety_yellow)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    WifiRobotManager.send("HORN_OFF")
                    hornButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fire_orange)
                }
            }
            true
        }
    }

    private fun showModeSwitchConfirmation() {
        val targetMode = if (isAutoMode) getString(R.string.mode_manual) else getString(R.string.mode_auto)
        
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_mode_change_title)
            .setMessage(getString(R.string.confirm_mode_change_message, targetMode))
            .setPositiveButton(R.string.confirm_yes) { _, _ ->
                toggleMode()
            }
            .setNegativeButton(R.string.confirm_no, null)
            .show()
    }

    private fun toggleMode() {
        isAutoMode = !isAutoMode
        if (isAutoMode) {
            WifiRobotManager.send("MODE_AUTO")
            modeToggleButton.text = getString(R.string.mode_auto)
            modeToggleButton.setIconResource(R.drawable.ic_toggle_auto)
            modeToggleButton.strokeColor = ContextCompat.getColorStateList(this, R.color.safety_yellow)
            modeToggleButton.setTextColor(ContextCompat.getColor(this, R.color.safety_yellow))
            setAllControlsEnabled(false)
        } else {
            WifiRobotManager.send("MODE_MANUAL")
            modeToggleButton.text = getString(R.string.mode_manual)
            modeToggleButton.setIconResource(R.drawable.ic_toggle_manual)
            modeToggleButton.strokeColor = ContextCompat.getColorStateList(this, R.color.white)
            modeToggleButton.setTextColor(ContextCompat.getColor(this, R.color.white))
            setAllControlsEnabled(true)
        }
    }

    private fun setAllControlsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.4f
        
        // Drive & Nozzle Buttons
        val buttons = arrayOf(
            btnForward, btnBackward, btnTurnLeft, btnTurnRight, 
            btnNozzleUp, btnNozzleDown, pumpButton, lightsButton, hornButton
        )
        
        for (view in buttons) {
            view.alpha = alpha
            view.isEnabled = enabled
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRepeatableTouch(view: View, command: String) {
        view.setOnTouchListener { _, event ->
            if (isAutoMode) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> activeCommand = command
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> activeCommand = "S"
            }
            false 
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
