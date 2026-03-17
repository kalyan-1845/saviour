@file:OptIn(ExperimentalMaterial3Api::class)
package com.sarathi.emergency.ui.navigation

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.repository.SarathiRepository
import com.sarathi.emergency.ui.screens.*
import com.sarathi.emergency.ui.viewmodel.*
import com.sarathi.emergency.util.LocationHelper
import androidx.compose.material3.ExperimentalMaterial3Api

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

/**
 * Safe navigation helper that prevents crashes from:
 * - Double-navigation (clicking twice quickly)
 * - Navigating after the composable is destroyed
 * - Invalid backstack states
 */
private fun NavHostController.safeNavigate(
    route: String,
    popUpToRoute: String? = null,
    inclusive: Boolean = true
) {
    try {
        val currentRoute = currentBackStackEntry?.destination?.route
        if (currentRoute == route) return // Already here, skip

        navigate(route) {
            popUpToRoute?.let {
                popUpTo(it) { this.inclusive = inclusive }
            }
        }
    } catch (e: Exception) {
        Log.e("NavGraph", "Navigation failed to $route", e)
    }
}

/**
 * Safe navigation that clears the entire backstack.
 */
private fun NavHostController.safeNavigateClearAll(route: String) {
    try {
        navigate(route) {
            popUpTo(graph.startDestinationId) { inclusive = true }
        }
    } catch (e: Exception) {
        Log.e("NavGraph", "Navigation (clear all) failed to $route", e)
    }
}

@Composable
fun NavGraph(api: SarathiApi, sessionManager: SessionManager) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Shared state for police/hospital dashboards
    var selectedStationName by remember { mutableStateOf("") }
    var selectedStationArea by remember { mutableStateOf("") }
    var selectedStationId by remember { mutableStateOf("") }
    var selectedHospName by remember { mutableStateOf("") }
    var selectedHospArea by remember { mutableStateOf("") }
    var selectedHospId by remember { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                isLoggedIn = sessionManager.isLoggedIn(),
                isPoliceLoggedIn = sessionManager.getPoliceStationId().isNotEmpty(),
                isHospitalLoggedIn = sessionManager.getHospitalId().isNotEmpty(),
                onDriverLogin = {
                    navController.safeNavigate(Routes.DRIVER_LOGIN, Routes.SPLASH)
                },
                onPublicSOS = {
                    navController.safeNavigate(Routes.SOS, Routes.SPLASH)
                },
                onGoToDashboard = {
                    navController.safeNavigate(Routes.DASHBOARD, Routes.SPLASH)
                },
                onPoliceDashboard = {
                    navController.safeNavigate(Routes.POLICE_DASHBOARD, Routes.SPLASH)
                },
                onHospitalDashboard = {
                    navController.safeNavigate(Routes.HOSPITAL_DASHBOARD, Routes.SPLASH)
                },
                onPoliceLogin = {
                    navController.safeNavigate(Routes.POLICE_LOGIN, Routes.SPLASH)
                },
                onHospitalLogin = {
                    navController.safeNavigate(Routes.HOSPITAL_LOGIN, Routes.SPLASH)
                }
            )
        }

        composable(Routes.DRIVER_LOGIN) {
            val repository = remember { SarathiRepository(api) }
            val viewModel: DriverViewModel = viewModel(
                factory = DriverViewModelFactory(repository, sessionManager)
            )
            DriverLoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.safeNavigate(Routes.DASHBOARD, Routes.DRIVER_LOGIN)
                },
                onRegister = {
                    navController.safeNavigate(Routes.DRIVER_REGISTER)
                },
                onBack = {
                    navController.safeNavigate(Routes.SPLASH, Routes.DRIVER_LOGIN)
                }
            )
        }

        composable(Routes.DRIVER_REGISTER) {
            val repository = remember { SarathiRepository(api) }
            val viewModel: DriverViewModel = viewModel(
                factory = DriverViewModelFactory(repository, sessionManager)
            )
            DriverRegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.safeNavigate(Routes.DASHBOARD, Routes.DRIVER_REGISTER)
                },
                onBack = {
                    try { navController.popBackStack() } catch (_: Exception) {}
                }
            )
        }

        composable(Routes.SOS) {
            val repository = remember { SarathiRepository(api) }
            val locationHelper = remember { LocationHelper(context) }
            val viewModel: SOSViewModel = viewModel(
                factory = SOSViewModelFactory(repository, locationHelper)
            )
            SOSScreen(
                viewModel = viewModel,
                sessionManager = sessionManager,
                onBack = {
                    navController.safeNavigate(Routes.SPLASH, Routes.SOS)
                }
            )
        }

        composable(Routes.DASHBOARD) {
            val repository = remember { SarathiRepository(api) }
            val viewModel: DriverViewModel = viewModel(
                factory = DriverViewModelFactory(repository, sessionManager)
            )
            DriverDashboardScreen(
                viewModel = viewModel,
                onNavigateToHospitalSelection = {
                    navController.safeNavigate(Routes.HOSPITAL_SELECTION)
                },
                onNavigateToActiveRoute = {
                    navController.safeNavigate(Routes.ACTIVE_ROUTE)
                },
                onLogout = {
                    navController.safeNavigateClearAll(Routes.SPLASH)
                }
            )
        }

        composable(Routes.HOSPITAL_SELECTION) {
            val repository = remember { SarathiRepository(api) }
            val viewModel: HospitalSelectionViewModel = viewModel(
                factory = HospitalSelectionViewModelFactory(repository)
            )
            HospitalSelectionScreen(
                viewModel = viewModel,
                onStartNavigation = {
                    navController.safeNavigate(Routes.ACTIVE_ROUTE, Routes.HOSPITAL_SELECTION)
                },
                onBack = {
                    try { navController.popBackStack() } catch (_: Exception) {}
                }
            )
        }

        composable(Routes.ACTIVE_ROUTE) {
            val repository = remember { SarathiRepository(api) }
            val viewModel: ActiveRouteViewModel = viewModel(
                factory = ActiveRouteViewModelFactory(repository, sessionManager)
            )
            ActiveRouteScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.safeNavigate(Routes.DASHBOARD, Routes.ACTIVE_ROUTE)
                },
                onBack = {
                    try { navController.popBackStack() } catch (_: Exception) {}
                }
            )
        }

        composable(Routes.POLICE_LOGIN) {
            PoliceLoginScreen(
                onLoginSuccess = { station ->
                    selectedStationId = station.id
                    selectedStationName = station.name
                    selectedStationArea = station.area
                    sessionManager.savePoliceStationId(station.id)
                    navController.safeNavigate(Routes.POLICE_DASHBOARD, Routes.POLICE_LOGIN)
                },
                onBack = {
                    navController.safeNavigate(Routes.SPLASH, Routes.POLICE_LOGIN)
                }
            )
        }

        composable(Routes.POLICE_DASHBOARD) {
            val repository = remember { SarathiRepository(api) }
            val viewModel: PoliceDashboardViewModel = viewModel(
                factory = PoliceDashboardViewModelFactory(repository)
            )
            PoliceDashboardScreen(
                stationId = selectedStationId.ifEmpty { sessionManager.getPoliceStationId() },
                stationName = selectedStationName.ifEmpty { "Hyderabad Central" },
                stationArea = selectedStationArea.ifEmpty { "Abids" },
                viewModel = viewModel,
                sessionManager = sessionManager,
                onLogout = {
                    navController.safeNavigateClearAll(Routes.SPLASH)
                }
            )
        }

        composable(Routes.HOSPITAL_LOGIN) {
            HospitalLoginScreen(
                onLoginSuccess = { hospital ->
                    selectedHospId = hospital.id
                    selectedHospName = hospital.name
                    selectedHospArea = hospital.area
                    sessionManager.saveHospitalId(hospital.id)
                    navController.safeNavigate(Routes.HOSPITAL_DASHBOARD, Routes.HOSPITAL_LOGIN)
                },
                onBack = {
                    navController.safeNavigate(Routes.SPLASH, Routes.HOSPITAL_LOGIN)
                }
            )
        }

        composable(Routes.HOSPITAL_DASHBOARD) {
            val repository = remember { SarathiRepository(api) }
            val viewModel: HospitalDashboardViewModel = viewModel(
                factory = HospitalDashboardViewModelFactory(repository)
            )
            HospitalDashboardScreen(
                hospitalId = selectedHospId.ifEmpty { sessionManager.getHospitalId() },
                hospitalName = selectedHospName.ifEmpty { "Apollo" },
                hospitalArea = selectedHospArea.ifEmpty { "Jubilee Hills" },
                viewModel = viewModel,
                sessionManager = sessionManager,
                onLogout = {
                    navController.safeNavigateClearAll(Routes.SPLASH)
                }
            )
        }
    }
}
