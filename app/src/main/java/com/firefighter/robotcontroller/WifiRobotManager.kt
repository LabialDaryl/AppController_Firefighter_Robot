package com.firefighter.robotcontroller

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Handles UDP communication and connection state monitoring.
 */
object WifiRobotManager {
    private const val TAG = "WifiRobotManager"
    private var udpSocket: DatagramSocket? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private var targetIp = "192.168.4.1"
    private var targetPort = 80
    
    private var lastPongReceived = 0L
    private const val TIMEOUT_MS = 3000L

    fun updateConfig(ip: String, port: Int) {
        targetIp = ip
        targetPort = port
        Log.d(TAG, "Config updated: $targetIp:$targetPort")
        initSocket()
    }

    private fun initSocket() {
        executor.execute {
            try {
                if (udpSocket == null || udpSocket!!.isClosed) {
                    udpSocket = DatagramSocket()
                    udpSocket?.soTimeout = 1000 // Timeout for receive
                    Log.d(TAG, "UDP Socket initialized")
                    startReceiver()
                    startHeartbeat()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize UDP socket: ${e.message}")
            }
        }
    }

    fun connect() {
        initSocket()
    }

    private fun startReceiver() {
        Thread {
            val buffer = ByteArray(1024)
            while (udpSocket != null && !udpSocket!!.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()
                    if (message == "PONG") {
                        lastPongReceived = System.currentTimeMillis()
                        Log.v(TAG, "Heartbeat: PONG received")
                    }
                } catch (e: Exception) {
                    // Timeout or socket closed
                }
            }
        }.start()
    }

    private fun startHeartbeat() {
        scheduler.scheduleWithFixedDelay({
            send("PING")
        }, 0, 1500, TimeUnit.MILLISECONDS)
    }

    fun send(command: String) {
        executor.execute {
            try {
                if (udpSocket == null || udpSocket!!.isClosed) {
                    udpSocket = DatagramSocket()
                    udpSocket?.soTimeout = 1000
                }

                val address = InetAddress.getByName(targetIp)
                val buffer = command.toByteArray()
                val packet = DatagramPacket(buffer, buffer.size, address, targetPort)
                
                udpSocket?.send(packet)
                if (command != "PING") {
                    Log.d(TAG, "UDP Sent: $command")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending UDP packet: ${e.message}")
            }
        }
    }

    fun disconnect() {
        executor.execute {
            try {
                udpSocket?.close()
                udpSocket = null
                Log.d(TAG, "UDP Socket closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing UDP socket: ${e.message}")
            }
        }
    }

    fun isConnected(): Boolean {
        // Now checks if we've heard from the robot recently
        return (System.currentTimeMillis() - lastPongReceived) < TIMEOUT_MS
    }
}
