package com.raul1x9.mineconsole

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.raul1x9.mineconsole.models.ServerProfile
import com.raul1x9.mineconsole.viewmodels.MainViewModel
import com.raul1x9.mineconsole.views.BiometricLockScreen
import com.raul1x9.mineconsole.views.ConsoleScreen
import com.raul1x9.mineconsole.views.DashboardScreen
import com.raul1x9.mineconsole.views.SettingsScreen
import com.raul1x9.mineconsole.views.ThemeManager
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()

        // Relaunch biometric prompt on startup if enabled
        if (viewModel.biometricsEnabled.value) {
            viewModel.currentScreen.value = "biometric_lock"
            triggerBiometricAuthentication()
        }

        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val themeColors = remember(viewModel.appTheme.value, systemDark) {
                ThemeManager.getThemeColors(viewModel.appTheme.value, systemDark)
            }
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = themeColors.background
                ) {
                    when (viewModel.currentScreen.value) {
                        "biometric_lock" -> {
                            BiometricLockScreen(
                                viewModel = viewModel,
                                onAuthenticateClick = { triggerBiometricAuthentication() }
                            )
                        }
                        "dashboard" -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onServerClick = { server ->
                                    viewModel.selectedServer.value = server
                                    viewModel.currentScreen.value = "console"
                                },
                                onSettingsClick = {
                                    viewModel.currentScreen.value = "settings"
                                }
                            )
                        }
                        "console" -> {
                            viewModel.selectedServer.value?.let { server ->
                                ConsoleScreen(
                                    server = server,
                                    viewModel = viewModel,
                                    onBackClick = {
                                        viewModel.currentScreen.value = "dashboard"
                                        viewModel.selectedServer.value = null
                                    }
                                )
                            } ?: run {
                                viewModel.currentScreen.value = "dashboard"
                            }
                        }
                        "settings" -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    viewModel.currentScreen.value = "dashboard"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    
                    // Fallback for emulator testing or devices without biometrics
                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS || 
                        errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                        errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE) {
                        viewModel.currentScreen.value = "dashboard"
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    viewModel.currentScreen.value = "dashboard"
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for MineConsole")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun triggerBiometricAuthentication() {
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Toast.makeText(this, "Biometrics unavailable: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            // Fallback in case of device emulation where biometrics aren't configured
            viewModel.currentScreen.value = "dashboard"
        }
    }
}
