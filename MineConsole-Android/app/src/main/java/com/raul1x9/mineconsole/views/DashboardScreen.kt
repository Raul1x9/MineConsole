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
                    onSave = { name, ip, port, pass ->
                        viewModel.addServer(name, ip, port, pass)
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
            Text(
                text = "${server.ip}:${server.rconPort}",
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
    onSave: (String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("25575") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "NEW CONNECTION PROFILE",
                color = themeAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
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
                    label = { Text("Server IP", fontSize = 12.sp) },
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
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("RCON Port", fontSize = 12.sp) },
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
                    label = { Text("RCON Password", fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = themeColors.background,
                        unfocusedContainerColor = themeColors.background,
                        focusedTextColor = themeColors.text,
                        unfocusedTextColor = themeColors.text
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

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
                        onSave(name, ip, portInt, password)
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
