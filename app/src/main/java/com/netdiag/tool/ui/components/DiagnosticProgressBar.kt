package com.netdiag.tool.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.ui.theme.*

/**
 * Diagnostic Progress Bar & Telemetry Line Tracker.
 * Displays "Running in 10 • Current Status: 20%" with high-tech glowing gradient animation.
 */
@Composable
fun DiagnosticProgressBar(
    currentStep: Int,
    totalSteps: Int,
    targetHost: String = "",
    lastRttMs: Long? = null,
    isRunning: Boolean = true,
    modifier: Modifier = Modifier
) {
    val progressFraction = remember(currentStep, totalSteps) {
        if (totalSteps > 0) (currentStep.toFloat() / totalSteps).coerceIn(0f, 1f) else 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "diagnostic_progress"
    )

    val percent = (animatedProgress * 100).toInt()

    // Pulsing alpha for live radar indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Live Indicator & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha) else StatusGreen,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "RUNNING IN $totalSteps PROBES" else "DIAGNOSTIC FINISHED ($totalSteps/$totalSteps)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) MaterialTheme.colorScheme.primary else StatusGreen,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "STATUS: $percent%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Glowing High-Precision Animated Progress Bar
            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceBg = MaterialTheme.colorScheme.surface

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            ) {
                val w = size.width
                val h = size.height
                val corner = CornerRadius(h / 2f, h / 2f)

                // 1. Background Track
                drawRoundRect(
                    color = surfaceBg,
                    size = size,
                    cornerRadius = corner
                )

                // 2. Active Progress Fill
                val fillWidth = w * animatedProgress
                if (fillWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.7f),
                                primaryColor,
                                StatusGreen
                            )
                        ),
                        size = Size(fillWidth, h),
                        cornerRadius = corner
                    )

                    // 3. Glowing Lead Dot
                    if (isRunning && animatedProgress < 0.99f) {
                        drawCircle(
                            color = Color.White,
                            radius = (h / 2f) - 1.5f,
                            center = Offset(fillWidth - (h / 2f), h / 2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Status Footnote: Probe step, Remaining, Last RTT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Probe $currentStep of $totalSteps" + if (targetHost.isNotBlank()) " • $targetHost" else "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                )

                if (lastRttMs != null && lastRttMs > 0) {
                    Text(
                        text = "Last RTT: $lastRttMs ms",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                } else {
                    Text(
                        text = "${(totalSteps - currentStep).coerceAtLeast(0)} remaining",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
