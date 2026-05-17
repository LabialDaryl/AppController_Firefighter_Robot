package com.firefighter.robotcontroller

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Handles high-performance UDP communication and connection state monitoring.
 * Optimized for low latency and reliable state reporting.
 */
object WifiRobotManager {
    private const val TAG = "WifiRobotManager"
    private var udpSocket: DatagramSocket? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private var targetIp = "192.168.4.1"
    private var targetPort = 80
    private var cachedAddress: InetAddress? = null

    @Volatile
    private var lastPongReceived = 0L
    private const val TIMEOUT_MS = 3000L

    private var receiverThread: Thread? = null
    private var heartbeatTask: Future<*>? = null

    // Callback for telemetry data
    var telemetryListener: ((String) -> Unit)? = null

    @Synchronized
    fun updateConfig(ip: String, port: Int) {
        if (targetIp != ip || targetPort != port) {
            targetIp = ip
            targetPort = port
            cachedAddress = null // Invalidate cache
            Log.d(TAG, "Config updated: $targetIp:$targetPort")
            reconnect()
        }
    }

    private fun reconnect() {
        executor.execute {
            closeSocket()
            initSocket()
        }
    }

    private fun initSocket() {
        try {
            if (udpSocket == null || udpSocket!!.isClosed) {
                udpSocket = DatagramSocket()
                udpSocket?.soTimeout = 1000 
                Log.d(TAG, "UDP Socket initialized on port ${udpSocket?.localPort}")
            }
            ensureReceiverRunning()
            ensureHeartbeatRunning()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize UDP socket: ${e.message}")
        }
    }

    fun connect() {
        executor.execute { initSocket() }
    }

    private fun ensureReceiverRunning() {
        if (receiverThread == null || !receiverThread!!.isAlive) {
            receiverThread = Thread {
                val buffer = ByteArray(1024)
                Log.d(TAG, "Receiver thread started")
                while (udpSocket != null && !udpSocket!!.isClosed) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        udpSocket?.receive(packet)
                        val message = String(packet.data, 0, packet.length).trim()
                        if (message == "PONG") {
                            lastPongReceived = System.currentTimeMillis()
                            // Log.v(TAG, "Heartbeat: PONG received")
                        } else {
                            Log.d(TAG, "Robot Response: $message")
                            // Forward telemetry data to listener
                            telemetryListener?.invoke(message)
                        }
                    } catch (e: SocketTimeoutException) {
                        // Normal behavior for non-blocking receive
                    } catch (e: Exception) {
                        if (udpSocket != null && !udpSocket!!.isClosed) {
                            Log.e(TAG, "Receiver error: ${e.message}")
                        }
                        break
                    }
                }
                Log.d(TAG, "Receiver thread stopped")
            }.apply { 
                name = "UDP-Receiver"
                priority = Thread.MAX_PRIORITY
                start() 
            }
        }
    }

    private fun ensureHeartbeatRunning() {
        if (heartbeatTask == null || heartbeatTask!!.isDone) {
            heartbeatTask = scheduler.scheduleWithFixedDelay({
                send("PING")
            }, 0, 1000, TimeUnit.MILLISECONDS)
        }
    }

    fun send(command: String) {
        executor.execute {
            try {
                if (udpSocket == null || udpSocket!!.isClosed) {
                    initSocket()
                }

                val address = cachedAddress ?: InetAddress.getByName(targetIp).also { cachedAddress = it }
                val buffer = command.toByteArray()
                val packet = DatagramPacket(buffer, buffer.size, address, targetPort)
                
                udpSocket?.send(packet)
                if (command != "PING") {
                    Log.v(TAG, "UDP Sent: $command")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Send error ($command): ${e.message}")
            }
        }
    }

    private fun closeSocket() {
        try {
            udpSocket?.close()
            udpSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        }
    }

    fun disconnect() {
        executor.execute {
            heartbeatTask?.cancel(true)
            heartbeatTask = null
            closeSocket()
            Log.d(TAG, "Disconnected")
        }
    }

    fun isConnected(): Boolean {
        if (lastPongReceived == 0L) return false
        val now = System.currentTimeMillis()
        val connected = (now - lastPongReceived) < TIMEOUT_MS
        if (!connected) {
            Log.w(TAG, "Connection lost. Last PONG was ${now - lastPongReceived}ms ago")
        }
        return connected
    }
}
