package com.sarathi.emergency.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.repository.SarathiRepository
import com.sarathi.emergency.ui.screens.*
import com.sarathi.emergency.ui.viewmodel.*
import com.sarathi.emergency.util.LocationHelper

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
                onPoliceDashboard = {
                    navController.navigate(Routes.POLICE_DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onHospitalDashboard = {
                    navController.navigate(Routes.HOSPITAL_DASHBOARD) {
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
            val repository = remember { SarathiRepository(api) }
            val viewModel: DriverViewModel = viewModel(
                factory = DriverViewModelFactory(repository, sessionManager)
            )
            DriverLoginScreen(
                viewModel = viewModel,
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
            val repository = remember { SarathiRepository(api) }
            val viewModel: DriverViewModel = viewModel(
                factory = DriverViewModelFactory(repository, sessionManager)
            )
            DriverRegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DRIVER_REGISTER) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
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
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(Routes.SOS) { inclusive = true }
                    }
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
            val repository = remember { SarathiRepository(api) }
            val viewModel: HospitalSelectionViewModel = viewModel(
                factory = HospitalSelectionViewModelFactory(repository)
            )
            HospitalSelectionScreen(
                viewModel = viewModel,
                onStartNavigation = {
                    navController.navigate(Routes.ACTIVE_ROUTE) {
                        popUpTo(Routes.HOSPITAL_SELECTION) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
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
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ACTIVE_ROUTE) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.POLICE_LOGIN) {
            PoliceLoginScreen(
                onLoginSuccess = { station ->
                    selectedStationId = station.id
                    selectedStationName = station.name
                    selectedStationArea = station.area
                    sessionManager.savePoliceStationId(station.id)
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
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(0) { inclusive = true }
                    }
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
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
