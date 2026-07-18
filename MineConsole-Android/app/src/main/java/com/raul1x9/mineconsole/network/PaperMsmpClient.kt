package com.raul1x9.mineconsole.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import org.json.JSONArray
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

    private data class PendingCmdInfo(
        val originalCommand: String,
        val successMessage: String?,
        val formatter: ((JSONObject) -> String)?
    )
    
    private val pendingCommandInfos = mutableMapOf<Int, PendingCmdInfo>()

    private data class TranslatedCommand(
        val method: String,
        val params: Any,
        val successMessage: String? = null,
        val formatter: ((JSONObject) -> String)? = null
    )

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
            val translated = translateCommand(command)
            if (translated.method == "unsupported") {
                completion(null, Exception("MSMP does not support running arbitrary console commands. Please use RCON connection type for full console shell access."))
                return
            }
            synchronized(this) {
                pendingCommandInfos[id] = PendingCmdInfo(command, translated.successMessage, translated.formatter)
            }

            val payload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", translated.method)
                if (translated.params is JSONObject && (translated.params as JSONObject).length() > 0) {
                    put("params", translated.params)
                } else if (translated.params is JSONArray && (translated.params as JSONArray).length() > 0) {
                    put("params", translated.params)
                }
                put("id", id)
            }

            ws.send(payload.toString())
        } catch (e: Exception) {
            synchronized(this) {
                callbacks.remove(id)
                pendingCommandInfos.remove(id)
            }
            completion(null, e)
        }
    }

    private fun translateCommand(input: String): TranslatedCommand {
        val clean = input.trim().removePrefix("/").trim()
        val parts = clean.split("\\s+".toRegex())
        if (parts.isEmpty()) return TranslatedCommand("minecraft:server/run_command", JSONObject().apply { put("command", input) })
        
        val base = parts[0].lowercase()
        
        when (base) {
            "list" -> {
                return TranslatedCommand(
                    method = "minecraft:players",
                    params = JSONObject(),
                    formatter = { response ->
                        val resultObj = response.optJSONObject("result")
                        val resultArr = response.optJSONArray("result")
                        val players = if (resultObj != null) {
                            resultObj.optJSONArray("players") ?: resultObj.optJSONArray("list") ?: resultObj
                        } else {
                            resultArr
                        }
                        if (players is JSONArray && players.length() > 0) {
                            val names = mutableListOf<String>()
                            for (i in 0 until players.length()) {
                                val player = players.optJSONObject(i)
                                if (player != null) {
                                    val name = player.optString("name")
                                    if (name.isNotEmpty()) names.add(name)
                                } else {
                                    val pStr = players.optString(i)
                                    if (pStr.isNotEmpty()) names.add(pStr)
                                }
                            }
                            "Connected Players: ${names.joinToString(", ")}"
                        } else {
                            "No players online."
                        }
                    }
                )
            }
            "whitelist" -> {
                if (parts.size >= 2) {
                    val sub = parts[1].lowercase()
                    if (sub == "add" && parts.size >= 3) {
                        val player = parts[2]
                        return TranslatedCommand(
                            method = "minecraft:allowlist/add",
                            params = JSONArray().apply {
                                put(JSONArray().apply {
                                    put(JSONObject().apply { put("name", player) })
                                })
                            },
                            successMessage = "Added $player to the whitelist."
                        )
                    } else if (sub == "remove" && parts.size >= 3) {
                        val player = parts[2]
                        return TranslatedCommand(
                            method = "minecraft:allowlist/remove",
                            params = JSONArray().apply {
                                put(JSONArray().apply {
                                    put(JSONObject().apply { put("name", player) })
                                })
                            },
                            successMessage = "Removed $player from the whitelist."
                        )
                    } else if (sub == "list") {
                        return TranslatedCommand(
                            method = "minecraft:allowlist/list",
                            params = JSONObject(),
                            formatter = { response ->
                                val result = response.optJSONArray("result") ?: response.optJSONObject("result")?.optJSONArray("players")
                                if (result is JSONArray && result.length() > 0) {
                                    val names = mutableListOf<String>()
                                    for (i in 0 until result.length()) {
                                        val player = result.optJSONObject(i)
                                        if (player != null) {
                                            val name = player.optString("name")
                                            if (name.isNotEmpty()) names.add(name)
                                        } else {
                                            val pStr = result.optString(i)
                                            if (pStr.isNotEmpty()) names.add(pStr)
                                        }
                                    }
                                    "Whitelisted Players: ${names.joinToString(", ")}"
                                } else {
                                    "Whitelist is empty."
                                }
                            }
                        )
                    }
                }
            }
            "op" -> {
                if (parts.size >= 2) {
                    val player = parts[1]
                    return TranslatedCommand(
                        method = "minecraft:operators/add",
                        params = JSONArray().apply {
                            put(JSONArray().apply {
                                put(JSONObject().apply { put("name", player) })
                            })
                        },
                        successMessage = "Opped $player."
                    )
                }
            }
            "deop" -> {
                if (parts.size >= 2) {
                    val player = parts[1]
                    return TranslatedCommand(
                        method = "minecraft:operators/remove",
                        params = JSONArray().apply {
                            put(JSONArray().apply {
                                put(JSONObject().apply { put("name", player) })
                            })
                        },
                        successMessage = "De-opped $player."
                    )
                }
            }
            "stop" -> {
                return TranslatedCommand(
                    method = "minecraft:server/stop",
                    params = JSONObject(),
                    successMessage = "Stopping the server..."
                )
            }
            "save-all" -> {
                return TranslatedCommand(
                    method = "minecraft:server/save",
                    params = JSONObject(),
                    successMessage = "Saving the world..."
                )
            }
        }
        
        return TranslatedCommand(
            method = "unsupported",
            params = JSONObject()
        )
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            
            if (json.has("id") && !json.isNull("id")) {
                val id = json.getInt("id")
                val callback = synchronized(this) { callbacks.remove(id) }
                val cmdInfo = synchronized(this) { pendingCommandInfos.remove(id) }
                
                if (json.has("result")) {
                    val result = json.get("result")
                    val output = if (cmdInfo?.formatter != null) {
                        try {
                            cmdInfo.formatter.invoke(json)
                        } catch (e: Exception) {
                            result.toString()
                        }
                    } else if (cmdInfo?.successMessage != null) {
                        cmdInfo.successMessage
                    } else if (result is JSONObject && result.has("output")) {
                        result.getString("output")
                    } else {
                        result.toString()
                    }
                    addLog(output)
                    callback?.invoke(output, null)
                } else if (json.has("error")) {
                    val errorObj = json.getJSONObject("error")
                    val errorCode = errorObj.optInt("code", 0)
                    val errorMsg = errorObj.optString("message", "Unknown error")
                    
                    val displayMsg = if (errorCode == -32601) {
                        "[Error] Command execution failed: Vanilla MSMP does not support running arbitrary console commands. Try using specific supported commands like /list, /whitelist, /op, /deop, /stop, or /save-all."
                    } else {
                        "[Error] $errorMsg"
                    }
                    addLog(displayMsg)
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
