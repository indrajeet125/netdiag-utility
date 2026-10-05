package com.netdiag.tool.ui.speed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netdiag.tool.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel = hiltViewModel()
) {
    val phase by viewModel.phase.collectAsState()
    val currentSpeed by viewModel.currentSpeedMbps.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val result by viewModel.result.collectAsState()
    val speedHistory by viewModel.speedHistory.collectAsState()

    val isRunning = phase != SpeedTestPhase.IDLE && phase != SpeedTestPhase.COMPLETED && phase != SpeedTestPhase.ERROR
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Server Info Card ──
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Dns,
                        contentDescription = "Server",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.serverName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Global Edge CDN • Multi-thread HTTP/2",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (phase) {
                        SpeedTestPhase.IDLE -> MaterialTheme.colorScheme.surface
                        SpeedTestPhase.COMPLETED -> StatusGreenBg
                        SpeedTestPhase.ERROR -> StatusRedBg
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = when (phase) {
                            SpeedTestPhase.IDLE -> "READY"
                            SpeedTestPhase.PING -> "PINGING"
                            SpeedTestPhase.DOWNLOAD -> "DOWNLOADING"
                            SpeedTestPhase.UPLOAD -> "UPLOADING"
                            SpeedTestPhase.COMPLETED -> "FINISHED"
                            SpeedTestPhase.ERROR -> "ERROR"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (phase) {
                                SpeedTestPhase.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                                SpeedTestPhase.COMPLETED -> StatusGreen
                                SpeedTestPhase.ERROR -> StatusRed
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Circular Speedometer Gauge ──
        SpeedometerGauge(
            currentSpeed = currentSpeed,
            phase = phase,
            modifier = Modifier
                .size(280.dp)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Real-Time Throughput Waveform ──
        ThroughputWaveformCard(
            speedHistory = speedHistory,
            phase = phase,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Primary 4-Metric Grid ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "LATENCY",
                value = if (result.pingMs > 0) "${result.pingMs} ms" else "--",
                subValue = if (result.jitterMs > 0) "Jitter ${result.jitterMs}ms" else "--",
                icon = Icons.Outlined.Timer,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "DOWNLOAD",
                value = if (result.downloadMbps > 0) String.format(Locale.US, "%.1f", result.downloadMbps) else "--",
                unit = if (result.downloadMbps > 0) "Mbps" else "",
                subValue = if (result.downloadPeakMbps > 0) "Peak " + String.format(Locale.US, "%.1f", result.downloadPeakMbps) else "--",
                icon = Icons.Default.CloudDownload,
                iconColor = StatusGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "UPLOAD",
                value = if (result.uploadMbps > 0) String.format(Locale.US, "%.1f", result.uploadMbps) else "--",
                unit = if (result.uploadMbps > 0) "Mbps" else "",
                subValue = if (result.uploadPeakMbps > 0) "Peak " + String.format(Locale.US, "%.1f", result.uploadPeakMbps) else "--",
                icon = Icons.Default.CloudUpload,
                iconColor = StatusBlue,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "TEST QUALITY",
                value = when {
                    result.downloadMbps > 100 -> "Ultra 4K"
                    result.downloadMbps > 30 -> "HD Stream"
                    result.downloadMbps > 10 -> "Standard"
                    result.downloadMbps > 0 -> "Low Bandwidth"
                    else -> "--"
                },
                subValue = if (isRunning) "Running..." else "Ready",
                icon = Icons.Outlined.Speed,
                iconColor = StatusAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Overall Progress Bar (When Active) ──
        if (isRunning) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Testing ${phase.name}...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ── Big Control Action Button ──
        if (!isRunning) {
            Button(
                onClick = { viewModel.startTest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (phase == SpeedTestPhase.COMPLETED) Icons.Default.Refresh else Icons.Default.PlayArrow,
                    contentDescription = "Start"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (phase == SpeedTestPhase.COMPLETED) "RUN TEST AGAIN" else "START SPEED TEST",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        } else {
            Button(
                onClick = { viewModel.stopTest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusRed,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STOP SPEED TEST",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Modern High-Precision Speedometer Gauge
 * Arc sweeps 270° from 135° to 405°
 */
@Composable
fun SpeedometerGauge(
    currentSpeed: Float,
    phase: SpeedTestPhase,
    modifier: Modifier = Modifier
) {
    // Non-linear scale mapping: 0 to 500 Mbps -> 0.0 to 1.0 fraction
    val targetFraction = remember(currentSpeed) {
        val speed = currentSpeed.coerceAtLeast(0f)
        when {
            speed <= 10f -> (speed / 10f) * 0.25f
            speed <= 50f -> 0.25f + ((speed - 10f) / 40f) * 0.25f
            speed <= 150f -> 0.50f + ((speed - 50f) / 100f) * 0.25f
            else -> (0.75f + ((speed - 150f) / 350f) * 0.25f).coerceAtMost(1f)
        }
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "gauge_needle"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth - 10.dp.toPx()
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )

            val startAngle = 135f
            val totalSweep = 270f

            // 1. Draw Track Arc
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Draw Active Gradient Progress Arc
            val activeSweep = totalSweep * animatedFraction
            if (activeSweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.5f),
                            primaryColor,
                            StatusGreen
                        ),
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Draw Major Scale Tick Marks
            val scaleTicks = listOf(0, 10, 50, 100, 250, 500)
            val tickRadius = diameter / 2f - strokeWidth - 6.dp.toPx()

            scaleTicks.forEach { tickVal ->
                val fraction = when {
                    tickVal <= 10 -> (tickVal / 10f) * 0.25f
                    tickVal <= 50 -> 0.25f + ((tickVal - 10f) / 40f) * 0.25f
                    tickVal <= 150 -> 0.50f + ((tickVal - 50f) / 100f) * 0.25f
                    else -> (0.75f + ((tickVal - 150f) / 350f) * 0.25f).coerceAtMost(1f)
                }
                val angleDeg = startAngle + totalSweep * fraction
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val tickStart = Offset(
                    center.x + (tickRadius * cos(angleRad)).toFloat(),
                    center.y + (tickRadius * sin(angleRad)).toFloat()
                )
                val tickEnd = Offset(
                    center.x + ((tickRadius - 6.dp.toPx()) * cos(angleRad)).toFloat(),
                    center.y + ((tickRadius - 6.dp.toPx()) * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = onSurfaceVariant.copy(alpha = 0.4f),
                    start = tickStart,
                    end = tickEnd,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 4. Draw Needle Pointer
            val needleAngleDeg = startAngle + totalSweep * animatedFraction
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLength = diameter / 2f - 22.dp.toPx()

            val needleEnd = Offset(
                center.x + (needleLength * cos(needleAngleRad)).toFloat(),
                center.y + (needleLength * sin(needleAngleRad)).toFloat()
            )

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.8f)),
                    start = center,
                    end = needleEnd
                ),
                start = center,
                end = needleEnd,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 5. Draw Center Needle Hub
            drawCircle(
                color = primaryColor,
                radius = 8.dp.toPx(),
                center = center
            )
            drawCircle(
                color = onSurface,
                radius = 3.dp.toPx(),
                center = center
            )
        }

        // Center Digital Speed Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 70.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", currentSpeed),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1).sp,
                    color = onSurface
                )
            )
            Text(
                text = "Mbps",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (phase) {
                    SpeedTestPhase.IDLE -> "TAP TO TEST"
                    SpeedTestPhase.PING -> "CALCULATING PING"
                    SpeedTestPhase.DOWNLOAD -> "DOWNLINK STREAM"
                    SpeedTestPhase.UPLOAD -> "UPLINK STREAM"
                    SpeedTestPhase.COMPLETED -> "TEST COMPLETE"
                    SpeedTestPhase.ERROR -> "CONNECTION TIMEOUT"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
            )
        }
    }
}

/**
 * Real-Time Throughput Waveform Canvas Card
 */
@Composable
fun ThroughputWaveformCard(
    speedHistory: List<SpeedPoint>,
    phase: SpeedTestPhase,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE THROUGHPUT STREAM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                )
                Text(
                    text = if (phase == SpeedTestPhase.DOWNLOAD) "DOWNLOAD SPECTRUM"
                           else if (phase == SpeedTestPhase.UPLOAD) "UPLOAD SPECTRUM"
                           else "STABLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw horizontal gridlines (25%, 50%, 75%)
                    listOf(0.25f, 0.50f, 0.75f).forEach { fraction ->
                        val y = h * fraction
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (speedHistory.size >= 2) {
                        val maxSpeed = (speedHistory.maxOfOrNull { it.mbps } ?: 10f).coerceAtLeast(10f) * 1.15f

                        val points = speedHistory.mapIndexed { index, pt ->
                            val x = (index.toFloat() / (speedHistory.size - 1)) * w
                            val y = h - ((pt.mbps / maxSpeed) * h).coerceIn(0f, h)
                            Offset(x, y)
                        }

                        // Path for stroke
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val cur = points[i]
                                val cx = (prev.x + cur.x) / 2f
                                cubicTo(cx, prev.y, cx, cur.y, cur.x, cur.y)
                            }
                        }

                        // Path for gradient fill
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(points.last().x, h)
                            lineTo(points.first().x, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.0f)
                                ),
                                startY = 0f,
                                endY = h
                            )
                        )

                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    } else {
                        // Flat baseline if no points yet
                        drawLine(
                            color = primaryColor.copy(alpha = 0.3f),
                            start = Offset(0f, h - 2f),
                            end = Offset(w, h - 2f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Metric Card for Speed Test
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String = "",
    subValue: String = "",
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            if (subValue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
