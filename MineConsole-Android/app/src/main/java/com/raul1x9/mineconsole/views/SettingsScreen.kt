package com.raul1x9.mineconsole.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raul1x9.mineconsole.viewmodels.MainViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf("Viewer") }
    var generatedToken by remember { mutableStateOf("") }
    var isCopied by remember { mutableStateOf(false) }

    val biometricsEnabled = viewModel.biometricsEnabled.value
    val tailscaleConnected = viewModel.tailscaleConnected.value

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val themeColors = remember(viewModel.appTheme.value, systemDark) {
        ThemeManager.getThemeColors(viewModel.appTheme.value, systemDark)
    }
    val themeAccent = remember(viewModel.appAccentColor.value) {
        ThemeManager.getAccentColor(viewModel.appAccentColor.value)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SETTINGS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = themeAccent,
                            letterSpacing = 2.sp
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // General Settings Card
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SYSTEM CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall.copy(color = themeAccent)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Secure Biometrics", color = themeColors.text)
                            Text(
                                "Require Face/Fingerprint unlock",
                                color = themeColors.subText,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = biometricsEnabled,
                            onCheckedChange = { viewModel.setBiometricsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = themeColors.background,
                                checkedTrackColor = themeAccent
                            )
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = themeColors.border)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Tailscale Link", color = themeColors.text)
                            Text(
                                "Simulate active WireGuard tunneling",
                                color = themeColors.subText,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = tailscaleConnected,
                            onCheckedChange = { viewModel.setTailscaleConnected(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = themeColors.background,
                                checkedTrackColor = themeAccent
                            )
                        )
                    }
                }
            }

            // Theme Personalization Card
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "THEME PERSONALIZATION",
                        style = MaterialTheme.typography.labelSmall.copy(color = themeAccent)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "APPEARANCE MODE",
                        color = themeColors.subText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themes = listOf("System", "Light", "Dark")
                        themes.forEach { theme ->
                            val active = viewModel.appTheme.value == theme
                            Button(
                                onClick = { viewModel.setAppTheme(theme) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) themeAccent.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (active) themeAccent else themeColors.text.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (active) themeAccent else themeColors.border,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Text(theme, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ACCENT COLOR",
                        color = themeColors.subText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colors = listOf("Green", "Blue", "Red", "Purple", "Orange")
                        colors.forEach { colorName ->
                            val colorValue = ThemeManager.getAccentColor(colorName)
                            val active = viewModel.appAccentColor.value == colorName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .background(colorValue, RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (active) 3.dp else 1.dp,
                                        color = if (active) themeColors.text else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.setAppAccentColor(colorName) }
                            )
                        }
                    }
                }
            }

            // Share Settings Card
            Card(
                colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ROLE-BASED SHARING LAB",
                        style = MaterialTheme.typography.labelSmall.copy(color = themeAccent)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Generate a restricted access token link for guest moderators.",
                        color = themeColors.subText,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf("Viewer", "Moderator", "Admin")
                        roles.forEach { role ->
                            val active = selectedRole == role
                            Button(
                                onClick = {
                                    selectedRole = role
                                    generatedToken = ""
                                    isCopied = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) themeAccent.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (active) themeAccent else themeColors.text.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (active) themeAccent else themeColors.border,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Text(role, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            generatedToken = "mineconsole://guest?role=${selectedRole.lowercase()}&token=${UUID.randomUUID().toString().take(8)}"
                            isCopied = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeAccent,
                            contentColor = if (viewModel.appAccentColor.value == "Green" || viewModel.appAccentColor.value == "Orange") Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GENERATE GUEST ACCESS LINK", style = MaterialTheme.typography.labelMedium)
                    }

                    if (generatedToken.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .border(1.dp, themeColors.border, RoundedCornerShape(6.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = generatedToken,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("MineConsole Token", generatedToken)
                                    clipboard.setPrimaryClip(clip)
                                    isCopied = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = if (isCopied) themeAccent else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
