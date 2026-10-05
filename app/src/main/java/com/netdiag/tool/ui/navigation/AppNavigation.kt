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
import kotlinx.coroutines.launch

sealed class BottomDestination(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomDestination("home", "Home", Icons.Outlined.Home)
    object Tests : BottomDestination("tests", "Tests", Icons.Outlined.Speed)
    object Network : BottomDestination("network", "Network", Icons.Outlined.CellTower)
    object Tools : BottomDestination("tools", "Tools", Icons.Outlined.Build)
}

val primaryBottomItems = listOf(
    BottomDestination.Home,
    BottomDestination.Tests,
    BottomDestination.Network,
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val onboardingViewModel: OnboardingViewModel = hiltViewModel()

    // Show bottom bar only on the 4 primary destinations
    val showBottomBar = currentRoute in listOf("home", "tests", "network", "tools")

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showBottomBar,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Drawer Header
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = com.netdiag.tool.R.drawable.app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "NETWORK TOOLS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "Diagnostics & Cellular Suite",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // HOME
                DrawerSection(title = "HOME")
                DrawerItem(
                    label = "Dashboard",
                    icon = Icons.Outlined.Home,
                    isSelected = currentRoute == "home",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("home") { launchSingleTop = true }
                    }
                )

                // TESTING
                DrawerSection(title = "TESTING")
                DrawerItem(
                    label = "Speed Test",
                    icon = Icons.Outlined.Speed,
                    isSelected = currentRoute == "speed_test",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("speed_test") { launchSingleTop = true }
                    }
                )
                DrawerItem(
                    label = "Ping & Latency",
                    icon = Icons.Outlined.NetworkCheck,
                    isSelected = currentRoute == "tests",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("tests") { launchSingleTop = true }
                    }
                )

                // CELLULAR
                DrawerSection(title = "CELLULAR")
                DrawerItem(
                    label = "Network & SIM Info",
                    icon = Icons.Outlined.CellTower,
                    isSelected = currentRoute == "network",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("network") { launchSingleTop = true }
                    }
                )
                DrawerItem(
                    label = "SIM Security & IMSI",
                    icon = Icons.Outlined.Badge,
                    isSelected = currentRoute == "sim_identity",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("sim_identity") { launchSingleTop = true }
                    }
                )
                DrawerItem(
                    label = "Network Mode Policy",
                    icon = Icons.Outlined.SettingsInputAntenna,
                    isSelected = currentRoute == "network_mode",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("network_mode") { launchSingleTop = true }
                    }
                )

                // UTILITIES
                DrawerSection(title = "UTILITIES")
                DrawerItem(
                    label = "Device & Hardware Telemetry",
                    icon = Icons.Outlined.PhoneAndroid,
                    isSelected = currentRoute == "device_info",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("device_info") { launchSingleTop = true }
                    }
                )
                DrawerItem(
                    label = "Tools Hub",
                    icon = Icons.Outlined.Build,
                    isSelected = currentRoute == "tools",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("tools") { launchSingleTop = true }
                    }
                )
                DrawerItem(
                    label = "OneTap Floating Setup",
                    icon = Icons.Outlined.TouchApp,
                    isSelected = currentRoute == "onetap_setup",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("onetap_setup") { launchSingleTop = true }
                    }
                )
                DrawerItem(
                    label = "Cache Cleaner",
                    icon = Icons.Outlined.CleaningServices,
                    isSelected = currentRoute == "cache_cleaner",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("cache_cleaner") { launchSingleTop = true }
                    }
                )
                DrawerItem(
                    label = "Settings",
                    icon = Icons.Outlined.Settings,
                    isSelected = currentRoute == "settings",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("settings") { launchSingleTop = true }
                    }
                )
            }
        }
    ) {
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
                            val isSelected = currentRoute == destination.route

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
                    SplashScreen(
                        isFirstLaunch = !isOnboardingDone,
                        onNavigateNext = { isFirst ->
                            if (isFirst) {
                                navController.navigate("welcome") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            } else {
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
                        onSkip = { navController.navigate("permission_checklist") }
                    )
                }

                composable("onboarding_slides") {
                    FeatureSlidesScreen(
                        onFinished = { navController.navigate("permission_checklist") },
                        onSkip = { navController.navigate("permission_checklist") }
                    )
                }

                composable("permission_checklist") {
                    PermissionChecklistScreen(
                        viewModel = onboardingViewModel,
                        onContinue = { navController.navigate("onetap_setup") }
                    )
                }

                composable("onetap_setup") {
                    OneTapSetupScreen(
                        viewModel = onboardingViewModel,
                        onContinue = { navController.navigate("setup_complete") },
                        onSkip = { navController.navigate("setup_complete") }
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
                        onNavigateToRoute = { route -> navController.navigate(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable("tests") {
                    TestsScreen(
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
                        onNavigateBack = { navController.popBackStack() }
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

                composable("network_mode") {
                    NetworkModeScreen()
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

@Composable
fun DrawerSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        ),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        selected = isSelected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}
