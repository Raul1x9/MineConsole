package com.raul1x9.mineconsole.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RconClient {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _logStream = MutableStateFlow<List<String>>(emptyList())
    val logStream: StateFlow<List<String>> = _logStream

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var currentRequestID = 42
    private val scope = CoroutineScope(Dispatchers.Default)

    private var commandCallback: ((String?, Exception?) -> Unit)? = null

    fun connect(host: String, port: Int, password: String) {
        scope.launch(Dispatchers.IO) {
            try {
                addLog("[System] TCP Socket connecting to $host:$port...")
                socket = Socket()
                socket?.connect(InetSocketAddress(host, port), 5000)
                outputStream = socket?.getOutputStream()
                inputStream = socket?.getInputStream()

                _isConnected.value = true
                addLog("[System] TCP Socket Connected. Authenticating...")

                // Send Auth packet
                authenticate(password)
                startReceiveLoop()
            } catch (e: Exception) {
                _isConnected.value = false
                _isAuthenticated.value = false
                addLog("[Error] Connection failed: ${e.localizedMessage}")
                disconnect()
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null
        _isConnected.value = false
        _isAuthenticated.value = false
        addLog("[System] Connection cancelled.")
    }

    fun appendLog(message: String) {
        addLog(message)
    }


    private fun addLog(message: String) {
        scope.launch(Dispatchers.Main) {
            _logStream.value = _logStream.value + message
        }
    }

    private fun authenticate(password: String) {
        val authPacket = RconPacket(id = 1000, type = 3, payload = password)
        sendPacket(authPacket)
    }

    fun sendCommand(command: String, completion: (String?, Exception?) -> Unit) {
        if (!isAuthenticated.value) {
            completion(null, Exception("Authentication failed or not established."))
            return
        }

        currentRequestID++
        commandCallback = completion
        val cmdPacket = RconPacket(id = currentRequestID, type = 2, payload = command)
        sendPacket(cmdPacket)
    }

    private fun sendPacket(packet: RconPacket) {
        scope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(packet.encode())
                outputStream?.flush()
            } catch (e: Exception) {
                addLog("[Error] Failed to send packet: ${e.localizedMessage}")
                disconnect()
            }
        }
    }

    private fun startReceiveLoop() {
        scope.launch(Dispatchers.IO) {
            try {
                val headerBuffer = ByteArray(4)
                while (isConnected.value) {
                    val stream = inputStream ?: break
                    
                    // Read exactly 4 bytes length
                    var bytesRead = 0
                    while (bytesRead < 4) {
                        val result = stream.read(headerBuffer, bytesRead, 4 - bytesRead)
                        if (result == -1) throw Exception("Socket disconnected by remote host.")
                        bytesRead += result
                    }

                    val length = readInt32LE(headerBuffer, 0)
                    if (length <= 0 || length > 4096) continue

                    // Read remaining packet bytes
                    val payloadBuffer = ByteArray(length)
                    var payloadBytesRead = 0
                    while (payloadBytesRead < length) {
                        val result = stream.read(payloadBuffer, payloadBytesRead, length - payloadBytesRead)
                        if (result == -1) throw Exception("Socket disconnected during payload receipt.")
                        payloadBytesRead += result
                    }

                    handleIncomingPacket(length, payloadBuffer)
                }
            } catch (e: Exception) {
                addLog("[Error] Receive stream broken: ${e.localizedMessage}")
                disconnect()
            }
        }
    }

    private fun handleIncomingPacket(length: Int, data: ByteArray) {
        if (data.size < 8) return

        val id = readInt32LE(data, 0)
        val type = readInt32LE(data, 4)

        // Read payload string until first null byte
        var payloadLength = 0
        for (i in 8 until data.size) {
            if (data[i] == 0.toByte()) {
                payloadLength = i - 8
                break
            }
        }

        val payloadString = String(data, 8, payloadLength, Charsets.UTF_8)

        if (id == 1000 || id == -1) {
            // Authentication Response
            if (id == 1000) {
                _isAuthenticated.value = true
                addLog("[System] Authenticated Successfully! Ready for commands.")
            } else {
                _isAuthenticated.value = false
                addLog("[Error] Authentication Failed: Invalid password.")
                disconnect()
            }
        } else {
            // Command delivery result
            addLog(payloadString)
            commandCallback?.invoke(payloadString, null)
            commandCallback = null
        }
    }

    private fun readInt32LE(buffer: ByteArray, offset: Int): Int {
        return ByteBuffer.wrap(buffer, offset, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
    }
}

data class RconPacket(val id: Int, val type: Int, val payload: String) {
    fun encode(): ByteArray {
        val payloadBytes = payload.toByteArray(Charsets.UTF_8)
        val length = 4 + 4 + payloadBytes.size + 2 // ID (4) + Type (4) + Payload length + 2 null terminators
        
        val buffer = ByteBuffer.allocate(4 + length)
            .order(ByteOrder.LITTLE_ENDIAN)
        
        buffer.putInt(length)
        buffer.putInt(id)
        buffer.putInt(type)
        buffer.put(payloadBytes)
        buffer.put(0.toByte())
        buffer.put(0.toByte())
        
        return buffer.array()
    }
}
