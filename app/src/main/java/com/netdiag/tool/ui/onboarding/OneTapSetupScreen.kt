package com.netdiag.tool.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.netdiag.tool.ui.theme.*

@Composable
fun OneTapSetupScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val canDrawOverlays by viewModel.canDrawOverlays.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = CanvasNavy,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text(
                        text = "Skip for now",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // OneTap Visual Header
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(SurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.TouchApp,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to OneTap",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Access your most useful network actions from anywhere using a movable floating shortcut.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    lineHeight = 20.sp
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Function Guide Step Graphic
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceBase,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "HOW IT WORKS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextTertiary,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GuideStep(label = "Floating button", step = "1")
                        Text("→", color = TextTertiary, fontSize = 18.sp)
                        GuideStep(label = "Single Tap", step = "2")
                        Text("→", color = TextTertiary, fontSize = 18.sp)
                        GuideStep(label = "Quick Actions", step = "3")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Setup Checklist
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ONETAP ACCESS STATUS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextTertiary,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OneTapStatusRow(
                        title = "Overlay Permission",
                        subtitle = if (canDrawOverlays) "Granted" else "Required to draw above apps",
                        isComplete = canDrawOverlays,
                        onAction = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OneTapStatusRow(
                        title = "Accessibility Access",
                        subtitle = if (isAccessibilityActive) "Active & configured" else "Tap to enable in Settings",
                        isComplete = isAccessibilityActive,
                        onAction = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = CanvasNavy
                )
            ) {
                Text(
                    text = if (isAccessibilityActive && canDrawOverlays) "Continue" else "Continue to App",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GuideStep(label: String, step: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(SurfaceHighlight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun OneTapStatusRow(
    title: String,
    subtitle: String,
    isComplete: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (isComplete) StatusGreenBg else SurfaceHighlight,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = StatusGreen,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(StatusAmber, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isComplete) StatusGreen else TextSecondary
                )
            )
        }

        if (!isComplete) {
            TextButton(onClick = onAction) {
                Text(
                    text = "Configure",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
