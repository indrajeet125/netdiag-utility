package com.netdiag.tool.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.netdiag.tool.data.repository.AppPreferencesRepository
import com.netdiag.tool.ui.dashboard.DashboardScreen
import com.netdiag.tool.ui.network.NetworkModeScreen
import com.netdiag.tool.ui.network.NetworkScreen
import com.netdiag.tool.ui.onboarding.*
import com.netdiag.tool.ui.settings.SettingsScreen
import com.netdiag.tool.ui.tests.TestTab
import com.netdiag.tool.ui.tests.TestsScreen
import com.netdiag.tool.ui.tools.ToolsScreen

sealed class BottomDestination(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomDestination("home", "Home", Icons.Outlined.Home)
    object Tests : BottomDestination("tests", "Tests", Icons.Outlined.Speed)
    object DeviceInfo : BottomDestination("device_info", "My Phone", Icons.Outlined.PhoneAndroid)
    object Tools : BottomDestination("tools", "Tools", Icons.Outlined.Build)
}

val primaryBottomItems = listOf(
    BottomDestination.Home,
    BottomDestination.Tests,
    BottomDestination.DeviceInfo,
    BottomDestination.Tools
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    preferencesRepository: AppPreferencesRepository,
    initialTargetRoute: String? = null,
    onRouteHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(initialTargetRoute) {
        if (!initialTargetRoute.isNullOrBlank()) {
            navController.navigate(initialTargetRoute) {
                launchSingleTop = true
            }
            onRouteHandled()
        }
    }

    val isOnboardingDone by preferencesRepository.onboardingCompleted.collectAsState()

    val onboardingViewModel: OnboardingViewModel = hiltViewModel()

    // Show bottom bar only on the 4 primary destinations
    val showBottomBar = currentRoute in listOf("home", "tests", "device_info", "tools") || currentRoute?.startsWith("tests?") == true

    // One navigation system: the bottom bar. Everything else is reached from a bottom tab.
    run {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        tonalElevation = 0.dp
                    ) {
                        primaryBottomItems.forEach { destination ->
                            val isSelected = if (destination.route == "tests") {
                                currentRoute == "tests" || currentRoute?.startsWith("tests?") == true
                            } else {
                                currentRoute == destination.route
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(padding)
            ) {
                // ── Onboarding Journey ──
                composable("splash") {
                    val isFirst = !isOnboardingDone
                    android.util.Log.d("Onboarding", "[Onboarding] startup state = ${if (isFirst) "first_launch" else "completed"}")
                    SplashScreen(
                        isFirstLaunch = isFirst,
                        onNavigateNext = { firstLaunch ->
                            if (firstLaunch) {
                                android.util.Log.d("Onboarding", "[Onboarding] Showing Overview")
                                navController.navigate("welcome") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            } else {
                                android.util.Log.d("Onboarding", "[Onboarding] Overview hidden")
                                navController.navigate("home") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable("welcome") {
                    WelcomeScreen(
                        onGetStarted = { navController.navigate("onboarding_slides") },
                        onSkip = {
                            onboardingViewModel.completeOnboarding()
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    )
                }

                composable("onboarding_slides") {
                    FeatureSlidesScreen(
                        onFinished = { navController.navigate("permission_checklist") },
                        onSkip = {
                            onboardingViewModel.completeOnboarding()
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    )
                }

                composable("permission_checklist") {
                    PermissionChecklistScreen(
                        viewModel = onboardingViewModel,
                        onContinue = { navController.navigate("onetap_setup") },
                        onSkip = {
                            onboardingViewModel.completeOnboarding()
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    )
                }

                composable("onetap_setup") {
                    OneTapSetupScreen(
                        viewModel = onboardingViewModel,
                        onContinue = { navController.navigate("setup_complete") },
                        onSkip = {
                            onboardingViewModel.completeOnboarding()
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    )
                }

                composable("setup_complete") {
                    SetupCompleteScreen(
                        onOpenApp = {
                            onboardingViewModel.completeOnboarding()
                            navController.navigate("home") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        },
                        onReviewPermissions = {
                            navController.navigate("permission_checklist")
                        }
                    )
                }

                // ── 4 Primary Bottom Destinations ──
                composable("home") {
                    DashboardScreen(
                        onNavigateToPing = { navController.navigate("tests") },
                        onNavigateToNetwork = { navController.navigate("network") },
                        onNavigateToSim = { navController.navigate("network") },
                        onNavigateToNetworkMode = { navController.navigate("network_mode") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToOneTap = { navController.navigate("onetap_setup") },
                        onNavigateToDeviceInfo = { navController.navigate("device_info") },
                        onNavigateToSimIdentity = { navController.navigate("sim_identity") },
                        onNavigateToRoute = { route -> navController.navigate(route) }
                    )
                }

                // Optional ?tab= lets Home deep-link into Checkup / History; plain "tests" still matches
                composable(
                    route = "tests?tab={tab}",
                    arguments = listOf(navArgument("tab") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { entry ->
                    val tab = entry.arguments?.getString("tab")
                        ?.let { name -> TestTab.values().firstOrNull { it.name == name } }
                        ?: TestTab.PING
                    TestsScreen(
                        initialTab = tab,
                        onOpenSettings = { navController.navigate("settings") }
                    )
                }

                composable("network") {
                    NetworkScreen()
                }

                composable("tools") {
                    ToolsScreen(
                        onNavigateToOneTap = { navController.navigate("onetap_setup") },
                        onNavigateToNetworkMode = { navController.navigate("network_mode") },
                        onNavigateToCacheCleaner = { navController.navigate("cache_cleaner") },
                        onNavigateToIpInfo = { navController.navigate("ip_info") },
                        onNavigateToSubnetCalculator = { navController.navigate("subnet_calculator") },
                        onNavigateToSignal = { navController.navigate("network") },
                        onNavigateToSimIdentity = { navController.navigate("sim_identity") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        preferencesRepository = preferencesRepository
                    )
                }

                // ── Child Screens ──
                composable("speed_test") {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "Speed Test",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        }
                    ) { padding ->
                        Box(modifier = Modifier.padding(padding)) {
                            com.netdiag.tool.ui.speed.SpeedTestScreen()
                        }
                    }
                }

                composable("device_info") {
                    com.netdiag.tool.ui.device.DeviceInfoScreen(
                        onNavigateBack = null,
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }

                composable("sim_identity") {
                    com.netdiag.tool.ui.device.SimIdentityScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("cache_cleaner") {
                    com.netdiag.tool.ui.cache.CacheCleanerScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("ip_info") {
                    com.netdiag.tool.ui.tools.IpInfoScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("subnet_calculator") {
                    com.netdiag.tool.ui.tools.SubnetCalculatorScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("network_mode") {
                    NetworkModeScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        preferencesRepository = preferencesRepository,
                        onNavigateBack = { navController.popBackStack() },
                        onResetOnboarding = {
                            navController.navigate("welcome") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
