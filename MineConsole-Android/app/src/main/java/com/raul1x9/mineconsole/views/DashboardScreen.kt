package com.raul1x9.mineconsole.views

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raul1x9.mineconsole.models.ServerProfile
import com.raul1x9.mineconsole.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onServerClick: (ServerProfile) -> Unit,
    onSettingsClick: () -> Unit
) {
    val servers by viewModel.servers.collectAsState()
    val tailscaleConnected = viewModel.tailscaleConnected.value

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val themeColors = remember(viewModel.appTheme.value, systemDark) {
        ThemeManager.getThemeColors(viewModel.appTheme.value, systemDark)
    }
    val themeAccent = remember(viewModel.appAccentColor.value) {
        ThemeManager.getAccentColor(viewModel.appAccentColor.value)
    }

    var showingAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MINE_CONSOLE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = themeAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = themeAccent
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showingAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Server",
                            tint = themeAccent
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tailscale Disconnected Warning Alert
                AnimatedVisibility(
                    visible = !tailscaleConnected,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color(0xFFE65C00).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE65C00).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = Color(0xFFE65C00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "TAILSCALE DISCONNECTED",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Verify secure WireGuard tunneling is enabled.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        TextButton(onClick = { viewModel.setTailscaleConnected(true) }) {
                            Text("DISMISS", color = Color(0xFFE65C00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (servers.isEmpty()) {
                    // Clean Empty Terminal state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "No Servers",
                            tint = themeAccent.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            "NO_SERVERS_DETECTED",
                            color = themeAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Link your first Minecraft RCON server below.",
                            color = themeColors.subText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showingAddDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeAccent,
                                contentColor = if (viewModel.appAccentColor.value == "Green" || viewModel.appAccentColor.value == "Orange") Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ADD SERVER PROFILE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    // Server Profiles LazyList
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(servers) { server ->
                            ServerRow(
                                server = server,
                                tailscaleConnected = tailscaleConnected,
                                themeColors = themeColors,
                                themeAccent = themeAccent,
                                onClick = { onServerClick(server) },
                                onDelete = { viewModel.deleteServer(server) }
                            )
                        }
                    }
                }
            }

            // Dialog for adding a server profile
            if (showingAddDialog) {
                AddServerDialog(
                    themeColors = themeColors,
                    themeAccent = themeAccent,
                    appAccentColor = viewModel.appAccentColor.value,
                    onDismiss = { showingAddDialog = false },
                    onSave = { name, ip, port, pass, connectionType, useTLS ->
                        viewModel.addServer(name, ip, port, pass, connectionType, useTLS)
                        showingAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun ServerRow(
    server: ServerProfile,
    tailscaleConnected: Boolean,
    themeColors: ThemeColors,
    themeAccent: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(themeColors.cardBackground)
            .border(1.dp, themeColors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glowing status indicator
        val indicatorColor = if (tailscaleConnected) themeAccent else Color.Red
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(indicatorColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name.uppercase(),
                color = themeColors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            val connTypeLabel = if (server.connectionType == "PAPER_MSMP") "MSMP" else "RCON"
            Text(
                text = "${server.ip}:${server.rconPort} ($connTypeLabel)",
                color = themeColors.subText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Role badge
        Box(
            modifier = Modifier
                .background(themeAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .border(1.dp, themeAccent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = server.sharedRole.uppercase(),
                color = themeAccent,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.Red.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AddServerDialog(
    themeColors: ThemeColors,
    themeAccent: Color,
    appAccentColor: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var connectionType by remember { mutableStateOf("RCON") } // "RCON" or "PAPER_MSMP"
    var port by remember { mutableStateOf("25575") }
    var password by remember { mutableStateOf("") }
    var useTLS by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    var showingHelp by remember { mutableStateOf(false) }

    if (showingHelp) {
        AlertDialog(
            onDismissRequest = { showingHelp = false },
            title = {
                Text(
                    "CONNECTION SETUP GUIDE",
                    color = themeAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = "1. RCON Protocol",
                        fontWeight = FontWeight.Bold,
                        color = themeColors.text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "The standard remote console shell protocol. Allows running any arbitrary command (/say, /gamemode, /tp, etc.). Configure in server.properties:\n" +
                               "enable-rcon=true\nrcon.port=25575\nrcon.password=your_secure_password",
                        color = themeColors.subText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "2. Paper MSMP (Modern)",
                        fontWeight = FontWeight.Bold,
                        color = themeColors.text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "A modern JSON-RPC protocol running over WebSockets, native to PaperMC and modern Minecraft. Setup in server.properties:\n" +
                               "management-server-enabled=true\n" +
                               "management-server-host=0.0.0.0\n" +
                               "management-server-port=25585\n" +
                               "management-server-secret=your_40_char_token\n" +
                               "management-server-tls-enabled=true",
                        color = themeColors.subText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "3. TLS (WSS) Encryption Options",
                        fontWeight = FontWeight.Bold,
                        color = themeColors.text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Scenario A (No TLS - Easiest for Local/VPN):\n" +
                               "Set 'management-server-tls-enabled=false' in server.properties and turn off the TLS switch in this app. Standard unencrypted WebSocket (ws://) will be used.\n\n" +
                               "• Scenario B (TLS Enabled - Most Secure):\n" +
                               "Set 'management-server-tls-enabled=true' in server.properties and turn on the TLS switch in this app. Fully encrypted WebSocket (wss://) will be used.",
                        color = themeColors.subText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showingHelp = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("OK, UNDERSTOOD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = themeColors.cardBackground
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "NEW CONNECTION PROFILE",
                    color = themeAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = { showingHelp = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Help Guide",
                        tint = themeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name", fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = themeColors.background,
                        unfocusedContainerColor = themeColors.background,
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("Server IP / Hostname", fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = themeColors.background,
                        unfocusedContainerColor = themeColors.background,
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Connection Type Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "CONNECTION TYPE",
                            color = themeAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Help Info",
                            tint = themeColors.subText,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { showingHelp = true }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("RCON", "PAPER_MSMP").forEach { type ->
                            val selected = connectionType == type
                            val label = if (type == "RCON") "RCON" else "PAPER MSMP"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) themeAccent.copy(alpha = 0.15f) else themeColors.background)
                                    .border(1.dp, if (selected) themeAccent else themeColors.border, RoundedCornerShape(6.dp))
                                    .clickable {
                                        connectionType = type
                                        port = if (type == "RCON") "25575" else "25585"
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) themeAccent else themeColors.text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                TextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text(if (connectionType == "RCON") "RCON Port" else "MSMP Port", fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = themeColors.background,
                        unfocusedContainerColor = themeColors.background,
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (connectionType == "RCON") "RCON Password" else "Management Secret Token", fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = themeColors.background,
                        unfocusedContainerColor = themeColors.background,
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Warning Banner for Paper MSMP Protocol Limitations
                if (connectionType == "PAPER_MSMP") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF332A00))
                            .border(1.dp, Color(0xFFFFCC00), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "⚠️ MSMP PROTOCOL LIMITATION NOTICE",
                                color = Color(0xFFFFCC00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Paper MSMP is a structured API that only supports specific management commands (/list, /whitelist, /op, /deop, /stop, /save-all). Arbitrary console commands (like /say, /gamemode, or /tp) are NOT supported. For full unrestricted console access, select RCON above.",
                                color = Color(0xFFFFEEAA),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // TLS Toggle (only show for Paper MSMP)
                if (connectionType == "PAPER_MSMP") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "ENABLE TLS (WSS)",
                                    color = themeColors.text,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "TLS Info",
                                    tint = themeColors.subText,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { showingHelp = true }
                                )
                            }
                            Text(
                                "Connect securely via wss://",
                                color = themeColors.subText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = useTLS,
                            onCheckedChange = { useTLS = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = themeAccent,
                                checkedTrackColor = themeAccent.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                if (showError) {
                    Text(
                        "Fill in all parameters correctly.",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val portInt = port.toIntOrNull()
                    if (name.isNotEmpty() && ip.isNotEmpty() && password.isNotEmpty() && portInt != null) {
                        onSave(name, ip, portInt, password, connectionType, useTLS)
                    } else {
                        showError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeAccent,
                    contentColor = if (appAccentColor == "Green" || appAccentColor == "Orange") Color.Black else Color.White
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("SAVE PROFILE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = themeAccent, fontSize = 12.sp)
            }
        },
        containerColor = themeColors.cardBackground
    )
}
