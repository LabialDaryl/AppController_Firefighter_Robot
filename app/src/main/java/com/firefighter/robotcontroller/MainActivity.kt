package com.firefighter.robotcontroller

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlin.math.abs

/**
 * Main control screen.
 *
 * Layout — three horizontal panels:
 *   LEFT   │  MOVEMENT joystick  (tank drive: F/B/L/R + diagonals)
 *   CENTRE │  Control buttons    (Suppress Fire, Lights, Horn, E-STOP)
 *   RIGHT  │  NOZZLE AIM joystick (pan/tilt servos)
 *
 * All control commands are single ASCII strings sent over a communication channel.
 */
class MainActivity : AppCompatActivity() {

    // ── constants ──────────────────────────────────────────────────────────────
    companion object {
        /** Minimum ms between consecutive movement commands (rate-limit). */
        private const val CMD_INTERVAL_MS  = 80L
        /** Dead-zone threshold for joystick axes (0 = infinite sensitivity). */
        private const val MOVE_THRESHOLD   = 0.28f
        private const val NOZZLE_THRESHOLD = 0.22f
    }

    // ── views ──────────────────────────────────────────────────────────────────
    private lateinit var connectionStatusText: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var movementJoystick: JoystickView
    private lateinit var nozzleJoystick: JoystickView
    private lateinit var pumpButton: MaterialButton
    private lateinit var lightsButton: MaterialButton
    private lateinit var hornButton: MaterialButton
    private lateinit var emergencyStopButton: MaterialButton

    // ── state ──────────────────────────────────────────────────────────────────
    private var pumpActive   = false
    private var lightsActive = false
    private var currentMovCmd  = "S"
    private var currentNozCmd  = "NC"
    private var lastCmdTime    = 0L
    private val handler = Handler(Looper.getMainLooper())

    // ══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        bindViews()
        initJoysticks()
        initButtons()
        initConnection()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("robot_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("ip", "192.168.4.1") ?: "192.168.4.1"
        val port = prefs.getInt("port", 80)
        WifiRobotManager.updateConfig(ip, port)
        WifiRobotManager.connect()
        updateConnectionStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        WifiRobotManager.disconnect()
    }

    private fun initConnection() {
        // Initial connection check
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateConnectionStatus()
                handler.postDelayed(this, 2000)
            }
        }, 2000)
    }

    private fun updateConnectionStatus() {
        if (WifiRobotManager.isConnected()) {
            connectionStatusText.text = "CONNECTED"
            connectionStatusText.setTextColor(ContextCompat.getColor(this, R.color.green_active))
        } else {
            connectionStatusText.text = "DISCONNECTED"
            connectionStatusText.setTextColor(ContextCompat.getColor(this, R.color.fire_orange))
            // Auto-reconnect
            WifiRobotManager.connect()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // View binding
    // ══════════════════════════════════════════════════════════════════════════

    private fun bindViews() {
        connectionStatusText = findViewById(R.id.connectionStatusText)
        settingsButton       = findViewById(R.id.settingsButton)
        movementJoystick     = findViewById(R.id.movementJoystick)
        nozzleJoystick       = findViewById(R.id.nozzleJoystick)
        pumpButton           = findViewById(R.id.pumpButton)
        lightsButton         = findViewById(R.id.lightsButton)
        hornButton           = findViewById(R.id.hornButton)
        emergencyStopButton  = findViewById(R.id.emergencyStopButton)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Joystick setup
    // ══════════════════════════════════════════════════════════════════════════

    private fun initJoysticks() {
        // LEFT – tank movement
        movementJoystick.setJoystickListener(object : JoystickView.JoystickListener {
            override fun onJoystickMoved(x: Float, y: Float) = handleMovement(x, y)
        })

        // RIGHT – nozzle pan / tilt
        nozzleJoystick.setJoystickListener(object : JoystickView.JoystickListener {
            override fun onJoystickMoved(x: Float, y: Float) = handleNozzle(x, y)
        })
    }

    private fun handleMovement(x: Float, y: Float) {
        val t = MOVE_THRESHOLD
        val cmd = when {
            y < -t && abs(x) <= t  -> "F"
            y >  t && abs(x) <= t  -> "B"
            x < -t && abs(y) <= t  -> "L"
            x >  t && abs(y) <= t  -> "R"
            y < -t && x < -t       -> "FL"
            y < -t && x >  t       -> "FR"
            y >  t && x < -t       -> "BL"
            y >  t && x >  t       -> "BR"
            else                   -> "S"
        }
        if (cmd != currentMovCmd) {
            currentMovCmd = cmd
            throttledSend(cmd)
        }
    }

    private fun handleNozzle(x: Float, y: Float) {
        val t = NOZZLE_THRESHOLD
        val cmd = when {
            y < -t && abs(x) <= t  -> "NU"
            y >  t && abs(x) <= t  -> "ND"
            x < -t && abs(y) <= t  -> "NL"
            x >  t && abs(y) <= t  -> "NR"
            y < -t && x < -t       -> "NUL"
            y < -t && x >  t       -> "NUR"
            y >  t && x < -t       -> "NDL"
            y >  t && x >  t       -> "NDR"
            else                   -> "NC"
        }
        if (cmd != currentNozCmd) {
            currentNozCmd = cmd
            throttledSend(cmd)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Button setup
    // ══════════════════════════════════════════════════════════════════════════

    private fun initButtons() {
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        pumpButton.setOnClickListener {
            pumpActive = !pumpActive
            if (pumpActive) {
                sendCommand("PUMP_ON")
                pumpButton.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.green_active)
                pumpButton.text = "💧 SUPPRESS\nFIRE ✔"
            } else {
                sendCommand("PUMP_OFF")
                pumpButton.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.red_inactive)
                pumpButton.text = "💧 SUPPRESS\nFIRE"
            }
        }

        lightsButton.setOnClickListener {
            lightsActive = !lightsActive
            if (lightsActive) {
                sendCommand("LIGHT_ON")
                lightsButton.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.green_active)
                lightsButton.text = "💡 LIGHTS ✔"
            } else {
                sendCommand("LIGHT_OFF")
                lightsButton.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.red_inactive)
                lightsButton.text = "💡 LIGHTS"
            }
        }

        hornButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendCommand("HORN_ON")
                    hornButton.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.safety_yellow)
                    hornButton.setTextColor(getColor(R.color.dark_background))
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    sendCommand("HORN_OFF")
                    hornButton.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.fire_orange)
                    hornButton.setTextColor(getColor(R.color.white))
                }
            }
            true
        }

        emergencyStopButton.setOnClickListener {
            sendCommand("ESTOP")
            pumpActive   = false
            lightsActive = false
            pumpButton.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.red_inactive)
            lightsButton.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.red_inactive)
            pumpButton.text   = "💧 SUPPRESS\nFIRE"
            lightsButton.text = "💡 LIGHTS"
            Toast.makeText(this, "⛔ EMERGENCY STOP ACTIVATED!", Toast.LENGTH_SHORT).show()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Command helpers
    // ══════════════════════════════════════════════════════════════════════════

    private fun throttledSend(cmd: String) {
        val now = System.currentTimeMillis()
        if (now - lastCmdTime >= CMD_INTERVAL_MS) {
            sendCommand(cmd)
            lastCmdTime = now
        }
    }

    private fun sendCommand(cmd: String) {
        WifiRobotManager.send(cmd)
    }
}
