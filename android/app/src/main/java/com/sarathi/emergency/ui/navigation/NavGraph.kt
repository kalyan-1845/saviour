package com.sarathi.emergency.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.ui.screens.*

object Routes {
    const val SPLASH = "splash"
    const val DRIVER_LOGIN = "driver_login"
    const val DRIVER_REGISTER = "driver_register"
    const val SOS = "sos"
    const val DASHBOARD = "dashboard"
    const val HOSPITAL_SELECTION = "hospital_selection"
    const val ACTIVE_ROUTE = "active_route"
    const val POLICE_LOGIN = "police_login"
    const val POLICE_DASHBOARD = "police_dashboard"
    const val HOSPITAL_LOGIN = "hospital_login"
    const val HOSPITAL_DASHBOARD = "hospital_dashboard"
}

@Composable
fun NavGraph(api: SarathiApi, sessionManager: SessionManager) {
    val navController = rememberNavController()

    // Shared state for police/hospital dashboards
    var selectedStationName by remember { mutableStateOf("") }
    var selectedStationArea by remember { mutableStateOf("") }
    var selectedHospName by remember { mutableStateOf("") }
    var selectedHospArea by remember { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                isLoggedIn = sessionManager.isLoggedIn(),
                onDriverLogin = {
                    navController.navigate(Routes.DRIVER_LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onPublicSOS = {
                    navController.navigate(Routes.SOS) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onGoToDashboard = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onPoliceLogin = {
                    navController.navigate(Routes.POLICE_LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onHospitalLogin = {
                    navController.navigate(Routes.HOSPITAL_LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DRIVER_LOGIN) {
            DriverLoginScreen(
                api = api,
                sessionManager = sessionManager,
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DRIVER_LOGIN) { inclusive = true }
                    }
                },
                onRegister = { navController.navigate(Routes.DRIVER_REGISTER) },
                onBack = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(Routes.DRIVER_LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DRIVER_REGISTER) {
            DriverRegisterScreen(
                api = api,
                sessionManager = sessionManager,
                onRegisterSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DRIVER_REGISTER) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SOS) {
            SOSScreen(
                api = api,
                onBack = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(Routes.SOS) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DriverDashboardScreen(
                api = api,
                sessionManager = sessionManager,
                onNavigateToHospitalSelection = {
                    navController.navigate(Routes.HOSPITAL_SELECTION)
                },
                onNavigateToActiveRoute = {
                    navController.navigate(Routes.ACTIVE_ROUTE)
                },
                onLogout = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOSPITAL_SELECTION) {
            HospitalSelectionScreen(
                onStartNavigation = {
                    navController.navigate(Routes.ACTIVE_ROUTE) {
                        popUpTo(Routes.HOSPITAL_SELECTION) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ACTIVE_ROUTE) {
            ActiveRouteScreen(
                api = api,
                sessionManager = sessionManager,
                onComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Police Panel ──
        composable(Routes.POLICE_LOGIN) {
            PoliceLoginScreen(
                onLoginSuccess = { station ->
                    selectedStationName = station.name
                    selectedStationArea = station.area
                    navController.navigate(Routes.POLICE_DASHBOARD) {
                        popUpTo(Routes.POLICE_LOGIN) { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(Routes.POLICE_LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.POLICE_DASHBOARD) {
            PoliceDashboardScreen(
                stationName = selectedStationName,
                stationArea = selectedStationArea,
                onLogout = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Hospital Panel ──
        composable(Routes.HOSPITAL_LOGIN) {
            HospitalLoginScreen(
                onLoginSuccess = { hospital ->
                    selectedHospName = hospital.name
                    selectedHospArea = hospital.area
                    navController.navigate(Routes.HOSPITAL_DASHBOARD) {
                        popUpTo(Routes.HOSPITAL_LOGIN) { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(Routes.HOSPITAL_LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOSPITAL_DASHBOARD) {
            HospitalDashboardScreen(
                hospitalName = selectedHospName,
                hospitalArea = selectedHospArea,
                onLogout = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
