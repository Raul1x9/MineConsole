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
import com.raul1x9.mineconsole.network.ConsoleLogManager
import com.raul1x9.mineconsole.network.RconClient
import com.raul1x9.mineconsole.network.PaperMsmpClient
import com.raul1x9.mineconsole.security.SecurityHelper
import com.raul1x9.mineconsole.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    server: ServerProfile,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val coroutineScope = rememberCoroutineScope()
    
    val isMsmp = server.connectionType == "PAPER_MSMP"
    val rcon = remember { RconClient() }
    val msmp = remember { PaperMsmpClient() }
    
    val isConnected by if (isMsmp) msmp.isConnected.collectAsState() else rcon.isConnected.collectAsState()
    val isAuthenticated by if (isMsmp) msmp.isAuthenticated.collectAsState() else rcon.isAuthenticated.collectAsState()
    
    val logStreamMap by ConsoleLogManager.logs.collectAsState()
    val logStream = logStreamMap[server.id] ?: emptyList()

    var commandInput by remember { mutableStateOf("") }
    val commandHistory = remember { mutableStateListOf<String>() }
    var showingHistory by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val themeColors = remember(viewModel.appTheme.value, systemDark) {
        ThemeManager.getThemeColors(viewModel.appTheme.value, systemDark)
    }
    val themeAccent = remember(viewModel.appAccentColor.value) {
        ThemeManager.getAccentColor(viewModel.appAccentColor.value)
    }

    // Connect on start
    LaunchedEffect(Unit) {
        val securityHelper = SecurityHelper.getInstance(context)
        val decryptedPass = securityHelper.readString(server.keychainKey) ?: ""
        if (isMsmp) {
            msmp.connect(server.ip, server.rconPort, decryptedPass, server.useTLS, server.id)
        } else {
            rcon.connect(server.ip, server.rconPort, decryptedPass, server.id)
        }
    }

    // Prevent screen turn-off and keep CPU awake during active Console monitoring
    DisposableEffect(Unit) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MineConsole::RconWakeLock")
        
        val activity = context as? android.app.Activity
        val window = activity?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        try {
            wakeLock.acquire(10 * 60 * 1000L) // 10 minutes maximum keep alive
        } catch (_: Exception) {}

        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            try {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
            if (isMsmp) {
                msmp.disconnect()
            } else {
                rcon.disconnect(true)
            }
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
                    val msg = "[Security Alert] Command $dCmd blocked: Moderator privilege level exceeded."
                    if (isMsmp) {
                        msmp.appendLog("> $cmd")
                        msmp.appendLog(msg)
                    } else {
                        rcon.appendLog("> $cmd")
                        rcon.appendLog(msg)
                    }
                    commandInput = ""
                    return
                }
            }
        }

        if (isMsmp) {
            msmp.appendLog("> $cmd")
            msmp.sendCommand(cmd) { response, error ->
                if (error != null) {
                    msmp.appendLog("[Error] Command delivery failed: ${error.localizedMessage}")
                } else {
                    triggerHapticFeedback("light")
                    if (commandHistory.lastOrNull() != cmd) {
                        commandHistory.add(cmd)
                    }
                }
            }
        } else {
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
                            color = themeAccent,
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
                            tint = themeAccent
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        triggerHapticFeedback("heavy")
                        ConsoleLogManager.clearLogs(server.id)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Logs",
                            tint = Color.Red
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.background
                )
            )
        },
        containerColor = themeColors.background
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
                    .background(themeColors.cardBackground)
                    .border(1.dp, themeColors.border)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (isAuthenticated) themeAccent else Color.Red
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
                    color = themeColors.subText,
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
                    .background(if (themeColors.background == Color(0xFFF5F5F5)) Color(0xFFE5E5E5) else Color(0xFF050505))
                    .border(1.dp, themeColors.border)
            ) {
                itemsIndexed(logStream) { _, log ->
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = getLogColor(log, themeAccent, themeColors.text)
                    )
                }
            }

            // Preset Command Row Keypad
            if (server.sharedRole != "Viewer") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.cardBackground)
                        .border(1.dp, themeColors.border)
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
                                .background(themeAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(1.dp, themeAccent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .clickable {
                                    commandInput = preset
                                    executeCommand()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                color = themeAccent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Filtered Suggestions Row
            val filteredSuggestions = remember(commandInput) {
                com.raul1x9.mineconsole.network.CommandAutocomplete.getSuggestions(commandInput)
            }

            if (filteredSuggestions.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColors.cardBackground)
                        .border(1.dp, themeColors.border)
                ) {
                    items(filteredSuggestions.size) { index ->
                        val suggestion = filteredSuggestions[index]
                        Box(
                            modifier = Modifier
                                .background(themeAccent, RoundedCornerShape(4.dp))
                                .clickable {
                                    val parts = commandInput.split(" ").toMutableList()
                                    if (parts.isNotEmpty()) {
                                        parts[parts.size - 1] = suggestion
                                        commandInput = parts.joinToString(" ") + " "
                                    } else {
                                        commandInput = "$suggestion "
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = suggestion,
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Bottom Input Console Pane
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.cardBackground)
                    .border(1.dp, themeColors.border)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (server.sharedRole == "Viewer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColors.border)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = themeColors.text.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CONSOLE IS VIEW-ONLY (RESTRICTED)",
                            color = themeColors.text.copy(alpha = 0.4f),
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
                            tint = themeAccent
                        )
                    }

                    TextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = { Text(if (server.connectionType == "PAPER_MSMP") "TYPE MSMP COMMAND (/list, /whitelist, /op...)" else "TYPE COMMAND...", color = themeColors.subText, fontSize = 11.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = themeColors.background,
                            unfocusedContainerColor = themeColors.background,
                            focusedTextColor = themeColors.text,
                            unfocusedTextColor = themeColors.text
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { executeCommand() }),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, themeColors.border, RoundedCornerShape(6.dp))
                    )

                    IconButton(
                        onClick = { executeCommand() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(themeAccent, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (viewModel.appAccentColor.value == "Green" || viewModel.appAccentColor.value == "Orange") Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (showingHistory) {
            CommandHistoryDialog(
                history = commandHistory,
                themeColors = themeColors,
                themeAccent = themeAccent,
                onDismiss = { showingHistory = false },
                onSelectCommand = {
                    commandInput = it
                    showingHistory = false
                }
            )
        }
    }
}

private fun getLogColor(line: String, themeAccent: Color, textCol: Color): Color {
    return when {
        line.startsWith(">") -> themeAccent
        line.contains("[Error]") || line.contains("[Security Alert]") -> Color.Red
        line.contains("[System]") -> Color(0xFFFF9900) // Clear gold color for dynamic mode readability
        line.contains("joined the game") || line.contains("left the game") -> Color(0xFF0066CC) // Deep visible blue
        else -> textCol
    }
}

@Composable
fun CommandHistoryDialog(
    history: List<String>,
    themeColors: ThemeColors,
    themeAccent: Color,
    onDismiss: () -> Unit,
    onSelectCommand: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "COMMAND_HISTORY",
                color = themeAccent,
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
                        color = themeColors.subText,
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
                                .background(themeColors.background, RoundedCornerShape(4.dp))
                                .border(1.dp, themeColors.border, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null,
                                tint = themeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cmd,
                                color = themeColors.text,
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
                Text("CLOSE", color = themeAccent, fontSize = 12.sp)
            }
        },
        containerColor = themeColors.cardBackground
    )
}
