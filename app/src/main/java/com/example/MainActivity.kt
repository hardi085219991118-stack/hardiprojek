package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.FarmViewModel
import com.example.ui.screens.*
import com.example.ui.theme.SejahteraBersamaTheme
import com.example.util.UserSessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SejahteraBersamaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SejahteraBersamaApp()
                }
            }
        }
    }
}

@Composable
fun SejahteraBersamaApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModel: FarmViewModel = viewModel()

    val summary by viewModel.dashboardSummary.collectAsState()
    val cycles by viewModel.cycles.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    val isLoggedIn = UserSessionManager.isLoggedIn(context)
                    if (isLoggedIn) {
                        navController.navigate("dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("welcome_auth") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        // --- AUTHENTICATION & VERIFICATION ROUTES ---
        composable("welcome_auth") {
            WelcomeAuthScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVerify = { userId ->
                    navController.navigate("verify_account/$userId") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }

        composable(
            route = "verify_account/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: 1L
            VerifyScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onVerificationSuccess = {
                    viewModel.refreshUserScope()
                    navController.navigate("dashboard") {
                        popUpTo("welcome_auth") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToVerify = { userId -> navController.navigate("verify_account/$userId") },
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("welcome_auth") { inclusive = true }
                    }
                }
            )
        }

        // --- DASHBOARD & MAIN APP ---
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                summary = summary,
                cycles = cycles,
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        // --- PHOTO EVIDENCE WATERMARK & REPORT DISPATCH ---
        composable("photo_evidence") {
            PhotoEvidenceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("report_dispatch") {
            val activeCycleId = summary.activeCycle?.id ?: 1L
            ReportDispatchScreen(
                cycleId = activeCycleId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "report_dispatch/{cycleId}",
            arguments = listOf(navArgument("cycleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cycleId = backStackEntry.arguments?.getLong("cycleId") ?: (summary.activeCycle?.id ?: 1L)
            ReportDispatchScreen(
                cycleId = cycleId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- CORE OPERATIONAL MODULES ---
        composable("daily_log") {
            DailyLogScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("coops") {
            CoopScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("partners") {
            PartnerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("cycles") {
            CycleScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("feed") {
            FeedScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("weight") {
            WeightScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("mortality") {
            MortalityScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("medicine") {
            MedicineScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("expenses") {
            ExpenseScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("harvest") {
            HarvestScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("reports_pdf") {
            ReportsPdfScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable("farm_profile") {
            FarmProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    viewModel.refreshUserScope()
                    navController.navigate("welcome_auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("broiler_guide") {
            BroilerGuideScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable("backup") {
            BackupExportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("about") {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onOpenTutorial = { navController.navigate("tutorial") }
            )
        }

        composable("tutorial") {
            TutorialScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
