package com.firefighter.robotcontroller

import android.util.Log
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Handles TCP communication with the ESP32 robot.
 * Renamed to WifiRobotManager to avoid collision with android.net.wifi.WifiManager.
 */
object WifiRobotManager {
    private const val TAG = "WifiRobotManager"
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var writer: PrintWriter? = null
    private val executor = Executors.newSingleThreadExecutor()

    private var targetIp = "192.168.4.1"
    private var targetPort = 80

    fun updateConfig(ip: String, port: Int) {
        targetIp = ip
        targetPort = port
        // Reconnect if config changes
        disconnect()
    }

    fun connect() {
        executor.execute {
            try {
                if (socket == null || socket!!.isClosed || !socket!!.isConnected) {
                    Log.d(TAG, "Connecting to $targetIp:$targetPort...")
                    val newSocket = Socket()
                    newSocket.connect(InetSocketAddress(targetIp, targetPort), 2000)
                    socket = newSocket
                    outputStream = socket?.getOutputStream()
                    writer = PrintWriter(outputStream!!, true)
                    Log.d(TAG, "Connected successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}")
                disconnect()
            }
        }
    }

    fun send(command: String) {
        executor.execute {
            try {
                if (socket == null || !socket!!.isConnected || socket!!.isClosed) {
                    // Try to reconnect if disconnected
                    val newSocket = Socket()
                    newSocket.connect(InetSocketAddress(targetIp, targetPort), 1000)
                    socket = newSocket
                    outputStream = socket?.getOutputStream()
                    writer = PrintWriter(outputStream!!, true)
                }
                
                writer?.println(command)
                Log.d(TAG, "Sent: $command")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command: ${e.message}")
                disconnect()
            }
        }
    }

    fun disconnect() {
        executor.execute {
            try {
                writer?.close()
                outputStream?.close()
                socket?.close()
                socket = null
                Log.d(TAG, "Disconnected")
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting: ${e.message}")
            }
        }
    }

    fun isConnected(): Boolean {
        return socket != null && socket!!.isConnected && !socket!!.isClosed
    }
}
