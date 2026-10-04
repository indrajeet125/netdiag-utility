package com.netdiag.tool.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.netdiag.tool.data.repository.AppPreferencesRepository
import com.netdiag.tool.ui.navigation.AppNavigation
import com.netdiag.tool.ui.theme.NetDiagTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: AppPreferencesRepository

    private val targetRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            val route by targetRoute.collectAsState()
            NetDiagTheme {
                AppNavigation(
                    preferencesRepository = preferencesRepository,
                    initialTargetRoute = route,
                    onRouteHandled = { targetRoute.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val nav = intent?.getStringExtra("navigate_to")
        if (!nav.isNullOrBlank()) {
            targetRoute.value = nav
        }
    }
}
