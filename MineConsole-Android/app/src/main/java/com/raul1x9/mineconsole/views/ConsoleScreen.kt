package com.raul1x9.mineconsole.views

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raul1x9.mineconsole.models.ServerProfile
import com.raul1x9.mineconsole.network.RconClient
import com.raul1x9.mineconsole.security.SecurityHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    server: ServerProfile,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val coroutineScope = rememberCoroutineScope()
    
    val rcon = remember { RconClient() }
    val isConnected by rcon.isConnected.collectAsState()
    val isAuthenticated by rcon.isAuthenticated.collectAsState()
    val logStream by rcon.logStream.collectAsState()

    var commandInput by remember { mutableStateOf("") }
    val commandHistory = remember { mutableStateListOf<String>() }
    var showingHistory by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Connect RCON on start
    LaunchedEffect(Unit) {
        val securityHelper = SecurityHelper.getInstance(context)
        val decryptedPass = securityHelper.readString(server.keychainKey) ?: ""
        rcon.connect(server.ip, server.rconPort, decryptedPass)
    }

    // Disconnect RCON on leave
    DisposableEffect(Unit) {
        onDispose {
            rcon.disconnect()
        }
    }

    // Auto-scroll on new logs
    LaunchedEffect(logStream.size) {
        if (logStream.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(logStream.size - 1)
            }
        }
    }

    fun triggerHapticFeedback(type: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    "heavy" -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    "light" -> VibrationEffect.createOneShot(30, 80)
                    else -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    fun executeCommand() {
        val cmd = commandInput.trim()
        if (cmd.isEmpty()) return

        triggerHapticFeedback("heavy")

        // Role restriction check for Moderator
        if (server.sharedRole == "Moderator") {
            val destructiveCommands = listOf("/stop", "/ban", "/op", "/deop", "/whitelist")
            val lowerCmd = cmd.lowercase()
            for (dCmd in destructiveCommands) {
                if (lowerCmd.startsWith(dCmd)) {
                    rcon.appendLog("> $cmd")
                    rcon.appendLog("[Security Alert] Command $dCmd blocked: Moderator privilege level exceeded.")
                    commandInput = ""
                    return
                }
            }
        }

        rcon.appendLog("> $cmd")

        rcon.sendCommand(cmd) { response, error ->
            if (error != null) {
                rcon.appendLog("[Error] Command delivery failed: ${error.localizedMessage}")
            } else {
                triggerHapticFeedback("light")
                if (commandHistory.lastOrNull() != cmd) {
                    commandHistory.add(cmd)
                }
            }
        }

        commandInput = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        server.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF00FF66),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00FF66)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0C0C0C)
                )
            )
        },
        containerColor = Color(0xFF0C0C0C)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection Status Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.02f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (isAuthenticated) Color(0xFF00FF66) else Color.Red
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAuthenticated) "CONNECTED - ${server.ip}" else "DISCONNECTED",
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "ROLE: ${server.sharedRole.uppercase()}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            // Scrolling terminal logs screen
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF050505))
                    .border(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                itemsIndexed(logStream) { _, log ->
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = getLogColor(log)
                    )
                }
            }

            // Preset Command Row Keypad
            if (server.sharedRole != "Viewer") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.01f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val presets = mutableListOf("/list", "/tps", "/weather clear", "/time set day")
                    if (server.sharedRole == "Admin") {
                        presets.add("/say [Admin Alert]")
                    }
                    presets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00FF66).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .clickable {
                                    commandInput = preset
                                    executeCommand()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                color = Color(0xFF00FF66),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Input Console Pane
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.02f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (server.sharedRole == "Viewer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CONSOLE IS VIEW-ONLY (RESTRICTED)",
                            color = Color.White.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    IconButton(onClick = { showingHistory = true }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color(0xFF00FF66)
                        )
                    }

                    TextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = { Text("TYPE COMMAND...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { executeCommand() }),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                    )

                    IconButton(
                        onClick = { executeCommand() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF00FF66), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (showingHistory) {
            CommandHistoryDialog(
                history = commandHistory,
                onDismiss = { showingHistory = false },
                onSelectCommand = {
                    commandInput = it
                    showingHistory = false
                }
            )
        }
    }
}

private fun getLogColor(line: String): Color {
    return when {
        line.startsWith(">") -> Color(0xFF00FF66)
        line.contains("[Error]") || line.contains("[Security Alert]") -> Color.Red
        line.contains("[System]") -> Color.Yellow
        line.contains("joined the game") || line.contains("left the game") -> Color.Cyan
        else -> Color.White
    }
}

@Composable
fun CommandHistoryDialog(
    history: List<String>,
    onDismiss: () -> Unit,
    onSelectCommand: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "COMMAND_HISTORY",
                color = Color(0xFF00FF66),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "HISTORY_EMPTY",
                        color = Color.White.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(history) { _, cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCommand(cmd) }
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null,
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cmd,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color(0xFF00FF66), fontSize = 12.sp)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}
