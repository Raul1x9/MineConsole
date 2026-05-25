package com.raul1x9.mineconsole.views

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BiometricLockScreen(onAuthenticateClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "BiometricSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C0C0C), Color(0xFF1E1E1E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MINE_CONSOLE",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color(0xFF00FF66),
                    letterSpacing = 4.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "SECURE PROTOCOL ACCESS REQUIRED",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 60.dp)
            )

            // Animated concentric scanning rings
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Outer Ring
                    drawCircle(
                        color = Color(0xFF00FF66).copy(alpha = 0.15f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Animated sweeping arc
                    drawArc(
                        color = Color(0xFF00FF66),
                        startAngle = sweepAngle,
                        sweepAngle = 45f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // Central Fingerprint Icon with breathing scale pulse
                IconButton(
                    onClick = onAuthenticateClick,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(scalePulse)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(100.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Authenticate",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = onAuthenticateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF66),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            ) {
                Text(
                    text = "AUTHENTICATE CONNECTION",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
