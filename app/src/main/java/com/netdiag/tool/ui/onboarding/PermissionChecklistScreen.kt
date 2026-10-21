package com.netdiag.tool.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.netdiag.tool.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionChecklistScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onSkip: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissions by viewModel.permissions.collectAsState()
    val selectedDetail by viewModel.selectedPermissionDetail.collectAsState()

    // Re-check permissions automatically on every ON_RESUME
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshState()
        viewModel.selectPermissionDetail(null)
    }

    val totalCount = permissions.size
    val grantedCount = permissions.count { it.status == PermissionStatus.GRANTED }
    val requiredTotal = permissions.count { it.isRequired }
    val requiredGranted = permissions.count { it.isRequired && it.status == PermissionStatus.GRANTED }
    val optionalTotal = permissions.count { !it.isRequired }
    val optionalGranted = permissions.count { !it.isRequired && it.status == PermissionStatus.GRANTED }
    val canProceed = requiredGranted == requiredTotal

    Scaffold(
        containerColor = CanvasNavy,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(
                        onClick = onSkip,
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasNavy)
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceBase,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Required: $requiredGranted / $requiredTotal",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (canProceed) StatusGreen else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Optional: $optionalGranted / $optionalTotal",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canProceed) AccentCyan else SurfaceElevated,
                            contentColor = if (canProceed) CanvasNavy else TextSecondary
                        )
                    ) {
                        Text(
                            text = if (canProceed) "Continue" else "Complete setup",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Set up Network Tools",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A few permissions are required for network and cellular diagnostics.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Indicator (e.g. 3 of 5 complete)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Setup Progress",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                            )
                            Text(
                                text = "$grantedCount of $totalCount complete",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (canProceed) StatusGreen else AccentCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { if (totalCount > 0) grantedCount.toFloat() / totalCount else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (canProceed) StatusGreen else AccentCyan,
                            trackColor = SurfaceHighlight,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PERMISSIONS CHECKLIST",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextTertiary,
                        letterSpacing = 1.2.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            items(permissions, key = { it.id }) { item ->
                PermissionRowItem(
                    item = item,
                    onClick = {
                        if (item.status != PermissionStatus.GRANTED) {
                            viewModel.selectPermissionDetail(item)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Permission Detail Bottom Sheet
    selectedDetail?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectPermissionDetail(null) },
            containerColor = SurfaceElevated,
            dragHandle = { BottomSheetDefaults.DragHandle(color = BorderSubtle) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = detail.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Why is this needed?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = detail.whyNeeded,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "What we use:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = StatusGreen
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                detail.whatWeUse.forEach { useCase ->
                    Text(
                        text = "• $useCase",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "What we do NOT do:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = StatusRed
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                detail.whatWeDoNotDo.forEach { restriction ->
                    Text(
                        text = "• $restriction",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (detail.androidPermission != null) {
                            permissionLauncher.launch(detail.androidPermission)
                        } else {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = CanvasNavy
                    )
                ) {
                    Text(
                        text = "Grant permission",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRowItem(
    item: PermissionItem,
    onClick: () -> Unit
) {
    val isGranted = item.status == PermissionStatus.GRANTED

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(
            1.dp,
            if (isGranted) BorderSubtle else AccentCyanBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // State Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = when (item.status) {
                            PermissionStatus.GRANTED -> StatusGreenBg
                            PermissionStatus.ACTION_REQUIRED -> AccentCyanBg
                            PermissionStatus.SPECIAL_ACCESS -> StatusAmberBg
                            PermissionStatus.OPTIONAL -> SurfaceHighlight
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (item.status) {
                    PermissionStatus.GRANTED -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Granted",
                            tint = StatusGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    PermissionStatus.ACTION_REQUIRED -> {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Action Required",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    PermissionStatus.SPECIAL_ACCESS -> {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Special Access",
                            tint = StatusAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    PermissionStatus.OPTIONAL -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(TextTertiary, CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (item.isRequired) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SurfaceHighlight
                        ) {
                            Text(
                                text = "REQUIRED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isGranted) StatusGreen else TextSecondary
                    )
                )
            }

            if (!isGranted) {
                Text(
                    text = "Allow",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
