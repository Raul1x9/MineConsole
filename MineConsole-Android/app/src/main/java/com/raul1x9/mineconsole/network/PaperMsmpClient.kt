package com.raul1x9.mineconsole.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PaperMsmpClient {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _logStream = MutableStateFlow<List<String>>(emptyList())
    val logStream: StateFlow<List<String>> = _logStream

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var serverId: String? = null
    private var currentId = 1
    private val scope = CoroutineScope(Dispatchers.Default)
    private val callbacks = mutableMapOf<Int, (String?, Exception?) -> Unit>()

    fun connect(host: String, port: Int, secret: String, useTLS: Boolean, serverId: String) {
        this.serverId = serverId
        scope.launch(Dispatchers.IO) {
            try {
                addLog("[System] Paper MSMP WS connecting to $host:$port...")
                
                val protocol = if (useTLS) "wss" else "ws"
                val url = "$protocol://$host:$port"

                val clientBuilder = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                
                client = clientBuilder.build()

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $secret")
                    .build()

                webSocket = client?.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _isConnected.value = true
                        _isAuthenticated.value = true
                        addLog("[System] MSMP Socket Connected & Authenticated.")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleMessage(text)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        _isConnected.value = false
                        _isAuthenticated.value = false
                        addLog("[System] MSMP Connection Closed: $reason")
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        _isConnected.value = false
                        _isAuthenticated.value = false
                        addLog("[Error] MSMP Connection Failure: ${t.localizedMessage}")
                    }
                })

            } catch (e: Exception) {
                _isConnected.value = false
                _isAuthenticated.value = false
                addLog("[Error] MSMP Connection failed: ${e.localizedMessage}")
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
        client = null
        _isConnected.value = false
        _isAuthenticated.value = false
        addLog("[System] MSMP Connection Cancelled.")
    }

    fun appendLog(message: String) {
        addLog(message)
    }

    private fun addLog(message: String) {
        scope.launch(Dispatchers.Main) {
            _logStream.value = _logStream.value + message
            serverId?.let { id ->
                ConsoleLogManager.addLog(id, message)
            }
        }
    }

    fun sendCommand(command: String, completion: (String?, Exception?) -> Unit) {
        val ws = webSocket
        if (ws == null || !_isConnected.value) {
            completion(null, Exception("Not connected"))
            return
        }

        val id = synchronized(this) {
            currentId++
            callbacks[currentId] = completion
            currentId
        }

        try {
            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "minecraft:server/run_command")
                put("params", JSONObject().apply {
                    put("command", command)
                })
                put("id", id)
            }

            ws.send(payload.toString())
        } catch (e: Exception) {
            synchronized(this) { callbacks.remove(id) }
            completion(null, e)
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            
            if (json.has("id") && !json.isNull("id")) {
                val id = json.getInt("id")
                val callback = synchronized(this) { callbacks.remove(id) }
                
                if (json.has("result")) {
                    val result = json.get("result")
                    val output = if (result is JSONObject && result.has("output")) {
                        result.getString("output")
                    } else {
                        result.toString()
                    }
                    addLog(output)
                    callback?.invoke(output, null)
                } else if (json.has("error")) {
                    val errorObj = json.getJSONObject("error")
                    val errorMsg = errorObj.optString("message", "Unknown error")
                    addLog("[Error] $errorMsg")
                    callback?.invoke(null, Exception(errorMsg))
                }
            } else if (json.has("method")) {
                val method = json.getString("method")
                if (method.startsWith("notification:")) {
                    val params = json.optJSONObject("params")
                    val logMessage = if (params != null) {
                        parseNotification(method, params)
                    } else {
                        "[$method]"
                    }
                    addLog(logMessage)
                }
            }
        } catch (e: Exception) {
            addLog(text)
        }
    }

    private fun parseNotification(method: String, params: JSONObject): String {
        return try {
            val cleanMethod = method.substringAfter("notification:")
            
            when (cleanMethod) {
                "chat_message" -> {
                    val sender = params.optString("sender", "System")
                    val message = params.optString("message", "")
                    "<$sender> $message"
                }
                "players/joined" -> {
                    val name = params.optString("name", "Unknown Player")
                    "[System] $name joined the game"
                }
                "players/left" -> {
                    val name = params.optString("name", "Unknown Player")
                    "[System] $name left the game"
                }
                "server/log" -> {
                    params.optString("message", "")
                }
                else -> {
                    "[$cleanMethod] $params"
                }
            }
        } catch (e: Exception) {
            "[$method] $params"
        }
    }
}
