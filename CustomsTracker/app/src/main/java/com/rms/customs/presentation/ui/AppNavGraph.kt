package com.rms.customs.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rms.customs.presentation.ui.admin.SettingsScreen
import com.rms.customs.presentation.ui.admin.SlaAdminScreen
import com.rms.customs.presentation.ui.admin.UserManagementScreen
import com.rms.customs.presentation.ui.auth.AdminSetupScreen
import com.rms.customs.presentation.ui.auth.AuthState
import com.rms.customs.presentation.ui.auth.LoginScreen
import com.rms.customs.presentation.ui.notification.NotificationCenterScreen
import com.rms.customs.presentation.ui.transaction.CreateTransactionScreen
import com.rms.customs.presentation.ui.transaction.TransactionDetailScreen
import com.rms.customs.presentation.viewmodel.AuthViewModel

@Composable
fun AppNavGraph(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.authState) {
        when (uiState.authState) {
            is AuthState.LoggedIn   -> navController.navigate(Dest.MAIN)  { popUpTo(0) { inclusive = true } }
            is AuthState.LoggedOut  -> navController.navigate(Dest.LOGIN)  { popUpTo(0) { inclusive = true } }
            is AuthState.NeedsSetup -> navController.navigate(Dest.SETUP)  { popUpTo(0) { inclusive = true } }
            AuthState.Loading       -> Unit
        }
    }

    NavHost(navController = navController, startDestination = Dest.LOADING) {

        composable(Dest.LOADING) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable(Dest.LOGIN) {
            LoginScreen(
                uiState      = uiState,
                onLogin      = authViewModel::login,
                onClearError = authViewModel::clearError,
            )
        }

        composable(Dest.SETUP) {
            AdminSetupScreen(
                uiState   = uiState,
                onSetup   = authViewModel::setupAdmin,
            )
        }

        composable(Dest.MAIN) {
            val session = (uiState.authState as? AuthState.LoggedIn)?.session
            if (session != null) {
                CompositionLocalProvider(LocalUserSession provides session) {
                    MainScreen(
                        navController = navController,
                        onLogout      = authViewModel::logout,
                    )
                }
            }
        }

        composable(
            route     = Dest.TRANSACTION_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) {
            val session = (uiState.authState as? AuthState.LoggedIn)?.session
            if (session != null) {
                CompositionLocalProvider(LocalUserSession provides session) {
                    TransactionDetailScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        composable(Dest.CREATE) {
            val session = (uiState.authState as? AuthState.LoggedIn)?.session
            if (session != null) {
                CompositionLocalProvider(LocalUserSession provides session) {
                    CreateTransactionScreen(
                        onBack    = { navController.popBackStack() },
                        onCreated = { navController.popBackStack() },
                    )
                }
            }
        }

        composable(Dest.NOTIFICATIONS) {
            val session = (uiState.authState as? AuthState.LoggedIn)?.session
            if (session != null) {
                CompositionLocalProvider(LocalUserSession provides session) {
                    NotificationCenterScreen(
                        onTransactionClick = { txId ->
                            navController.navigate(Dest.transactionDetail(txId))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        composable(Dest.SLA_ADMIN) {
            val session = (uiState.authState as? AuthState.LoggedIn)?.session
            if (session != null) {
                CompositionLocalProvider(LocalUserSession provides session) {
                    SlaAdminScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        composable(Dest.SETTINGS) {
            val session = (uiState.authState as? AuthState.LoggedIn)?.session
            if (session != null) {
                CompositionLocalProvider(LocalUserSession provides session) {
                    SettingsScreen(
                        onBack                     = { navController.popBackStack() },
                        onNavigateToSlaAdmin       = { navController.navigate(Dest.SLA_ADMIN) },
                        onNavigateToUserManagement = { navController.navigate(Dest.USER_MANAGEMENT) },
                    )
                }
            }
        }

        composable(Dest.USER_MANAGEMENT) {
            val session = (uiState.authState as? AuthState.LoggedIn)?.session
            if (session != null) {
                CompositionLocalProvider(LocalUserSession provides session) {
                    UserManagementScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
