@file:OptIn(ExperimentalMaterial3Api::class)

package com.rms.customs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rms.customs.ui.dashboard.DashboardScreen
import com.rms.customs.ui.theme.CustomsTheme
import com.rms.customs.ui.transaction.PhaseTrackingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CustomsTheme { CustomsApp() } }
    }
}

@Composable
fun CustomsApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val screenTitle = when (currentRoute) {
        "phase_detail" -> "RMS-2026-0031 — المرحلة 4"
        else           -> "متتبع التخليص الجمركي"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    if (currentRoute != "dashboard") {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor        = MaterialTheme.colorScheme.primary,
                    titleContentColor     = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = "dashboard",
            modifier         = Modifier.padding(padding).fillMaxSize()
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onTransactionClick = { ref ->
                        // Only RMS-2026-0031 has Phase 4 detail in this demo
                        if (ref == "RMS-2026-0031") navController.navigate("phase_detail")
                    }
                )
            }
            composable("phase_detail") {
                PhaseTrackingScreen()
            }
        }
    }
}
