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
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // Navigation state
    private var currentScreen = mutableStateOf("dashboard")
    private var selectedServer: ServerProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()

        // Relaunch biometric prompt on startup if enabled
        if (viewModel.biometricsEnabled.value) {
            currentScreen.value = "biometric_lock"
            triggerBiometricAuthentication()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen.value) {
                        "biometric_lock" -> {
                            BiometricLockScreen(
                                onAuthenticateClick = { triggerBiometricAuthentication() }
                            )
                        }
                        "dashboard" -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onServerClick = { server ->
                                    selectedServer = server
                                    currentScreen.value = "console"
                                },
                                onSettingsClick = {
                                    currentScreen.value = "settings"
                                }
                            )
                        }
                        "console" -> {
                            selectedServer?.let { server ->
                                ConsoleScreen(
                                    server = server,
                                    onBackClick = {
                                        currentScreen.value = "dashboard"
                                        selectedServer = null
                                    }
                                )
                            } ?: run {
                                currentScreen.value = "dashboard"
                            }
                        }
                        "settings" -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    currentScreen.value = "dashboard"
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
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    currentScreen.value = "dashboard"
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
            currentScreen.value = "dashboard"
        }
    }
}
