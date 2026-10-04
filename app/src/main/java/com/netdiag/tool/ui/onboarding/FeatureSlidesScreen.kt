package com.netdiag.tool.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingSlideData(
    val icon: ImageVector,
    val title: String,
    val description: String
)

val onboardingSlides = listOf(
    OnboardingSlideData(
        icon = Icons.Outlined.CellTower,
        title = "Understand your network",
        description = "See your current connection, cellular technology, signal strength, IP information, and network state."
    ),
    OnboardingSlideData(
        icon = Icons.Outlined.Speed,
        title = "Test your connection",
        description = "Run HTTP, TCP, and other supported network tests and understand where connectivity problems occur."
    ),
    OnboardingSlideData(
        icon = Icons.Outlined.SimCard,
        title = "Inspect your mobile network",
        description = "View SIM 1 and SIM 2 information, LTE and 5G cell details, signal metrics, and network transitions."
    )
)

@Composable
fun FeatureSlidesScreen(
    onFinished: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })
    val coroutineScope = rememberCoroutineScope()

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
                        text = "Skip",
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val slide = onboardingSlides[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .background(SurfaceElevated, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = slide.icon,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = slide.description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextSecondary,
                            lineHeight = 22.sp,
                            fontSize = 14.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Dot Indicator
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(onboardingSlides.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .background(
                                color = if (isSelected) AccentCyan else BorderSubtle,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Bottom CTA (Next / Get started)
            val isLastPage = pagerState.currentPage == onboardingSlides.size - 1
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
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
                    text = if (isLastPage) "Get started" else "Next",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
