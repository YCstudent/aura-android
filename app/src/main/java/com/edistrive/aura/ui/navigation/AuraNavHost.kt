package com.edistrive.aura.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.edistrive.aura.ui.screens.DisclaimerDialog
import com.edistrive.aura.ui.screens.EmailBindingScreen
import com.edistrive.aura.ui.screens.LoginScreen
import com.edistrive.aura.ui.screens.PhoneBindingScreen
import com.edistrive.aura.ui.screens.ProfileScreen
import com.edistrive.aura.ui.screens.ProfileCompletionScreen
import com.edistrive.aura.ui.screens.RegisterScreen
import com.edistrive.aura.ui.screens.ResetPasswordScreen
import com.edistrive.aura.ui.screens.SplashScreen
import com.edistrive.aura.ui.screens.family.AcceptInvitationScreen
import com.edistrive.aura.ui.screens.family.EditFamilyMemberScreen
import com.edistrive.aura.ui.screens.family.FamilyManagementScreen
import com.edistrive.aura.ui.screens.family.FamilyMemberDetailScreen
import com.edistrive.aura.ui.screens.health.HealthReportScreen
import com.edistrive.aura.ui.screens.medical.EditMedicalRecordScreen
import com.edistrive.aura.ui.screens.medical.MedicalRecordDetailScreen
import com.edistrive.aura.ui.screens.medical.MedicalRecordsScreen
import com.edistrive.aura.ui.screens.medical.MemberMedicalRecordsScreen
import com.edistrive.aura.ui.screens.medication.EditMedicationScreen
import com.edistrive.aura.ui.screens.medication.MedicationManagementScreen
import com.edistrive.aura.ui.screens.medication.MemberMedicationScreen
import com.edistrive.aura.ui.screens.medication.SendMedicationRequestScreen
import com.edistrive.aura.ui.screens.digital.DigitalHumanScreen
import com.edistrive.aura.ui.screens.AppointmentsScreen
import com.edistrive.aura.ui.screens.AddAppointmentScreen
import com.edistrive.aura.ui.screens.ChangePasswordScreen
import com.edistrive.aura.ui.screens.HospitalMapDetailScreen
import com.edistrive.aura.ui.screens.HospitalsMapScreen
import com.edistrive.aura.ui.screens.ActivitiesScreen
import com.edistrive.aura.ui.screens.PolicyViewScreen
import com.edistrive.aura.ui.screens.ScheduleScreen
import com.edistrive.aura.ui.screens.SettingsScreen
import com.edistrive.aura.ui.state.AppStateViewModel
import com.google.gson.Gson
import com.edistrive.aura.data.model.Hospital

@Composable
fun AuraNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: AppStateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen()
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { viewModel.onLoginSuccess() },
                onGoRegister = { navController.navigate(Routes.REGISTER) },
                onResetPassword = { navController.navigate(Routes.RESET_PASSWORD) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackToLogin = { navController.popBackStack() },
                onPrivacyPolicy = { navController.navigate(Routes.PRIVACY_POLICY) },
                onUserAgreement = { navController.navigate(Routes.USER_AGREEMENT) }
            )
        }
        composable(Routes.RESET_PASSWORD) {
            ResetPasswordScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        composable(Routes.EMAIL_BINDING) {
            EmailBindingScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PHONE_BINDING) {
            PhoneBindingScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE_COMPLETION) {
            ProfileCompletionScreen(
                forceCompletion = true,
                onCompleted = { viewModel.onProfileCompleted() }
            )
        }
        composable(Routes.PROFILE_CENTER) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.MAIN_TABS) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    viewModel.logout()
                },
                onDeleteAccount = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    viewModel.logout()
                },
                onEmailBinding = { navController.navigate(Routes.EMAIL_BINDING) },
                onPhoneBinding = { navController.navigate(Routes.PHONE_BINDING) }
            )
        }
        composable(Routes.MAIN_TABS) {
            MainTabsScreen(
                navController = navController,
                onLogout = { viewModel.logout() }
            )
        }

        // --------- Family ---------
        composable(Routes.FAMILY_MANAGEMENT) {
            FamilyManagementScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { navController.navigate(Routes.familyDetail(it)) },
                onAddMember = { navController.navigate(Routes.FAMILY_ADD) },
                onEditMember = { navController.navigate(Routes.familyEdit(it)) }
            )
        }
        composable(Routes.FAMILY_ADD) {
            EditFamilyMemberScreen(
                memberId = null,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.FAMILY_EDIT,
            arguments = listOf(navArgument("memberId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("memberId") ?: 0
            EditFamilyMemberScreen(
                memberId = id.takeIf { it > 0 },
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.FAMILY_DETAIL,
            arguments = listOf(navArgument("memberId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("memberId") ?: 0
            FamilyMemberDetailScreen(
                memberId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.familyEdit(it)) },
                onOpenMedicalRecords = { navController.navigate(Routes.memberMedicalRecords(it)) },
                onOpenMedications = { navController.navigate(Routes.memberMedication(it)) },
                onOpenHealthReport = { navController.navigate(Routes.healthReport(it)) }
            )
        }
        composable(Routes.ACCEPT_INVITATION) {
            AcceptInvitationScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        // --------- Medical Records ---------
        composable(Routes.MEDICAL_RECORDS) {
            MedicalRecordsScreen(
                memberId = null,
                onBack = { navController.popBackStack() },
                onOpenDetail = { navController.navigate(Routes.medicalRecordDetail(it)) },
                onAdd = { navController.navigate(Routes.MEDICAL_RECORD_ADD) },
                onEdit = { navController.navigate(Routes.medicalRecordEdit(it)) }
            )
        }
        composable(Routes.MEDICAL_RECORD_ADD) {
            EditMedicalRecordScreen(
                recordId = null,
                memberId = null,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            Routes.MEDICAL_RECORD_EDIT,
            arguments = listOf(navArgument("recordId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("recordId") ?: 0
            EditMedicalRecordScreen(
                recordId = id.takeIf { it > 0 },
                memberId = null,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            Routes.MEDICAL_RECORD_DETAIL,
            arguments = listOf(navArgument("recordId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("recordId") ?: 0
            MedicalRecordDetailScreen(
                recordId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.medicalRecordEdit(it)) }
            )
        }
        composable(
            Routes.MEMBER_MEDICAL_RECORDS,
            arguments = listOf(navArgument("memberId") { type = NavType.IntType })
        ) { entry ->
            val mid = entry.arguments?.getInt("memberId") ?: 0
            MemberMedicalRecordsScreen(
                memberId = mid,
                onBack = { navController.popBackStack() },
                onOpenDetail = { navController.navigate(Routes.medicalRecordDetail(it)) }
            )
        }

        // --------- Medications ---------
        composable(Routes.MEDICATIONS) {
            MedicationManagementScreen(
                memberId = null,
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.MEDICATION_ADD) },
                onEdit = { navController.navigate(Routes.medicationEdit(it)) }
            )
        }
        composable(Routes.MEDICATION_ADD) {
            EditMedicationScreen(
                medicationId = null,
                memberId = null,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            Routes.MEDICATION_EDIT,
            arguments = listOf(navArgument("medicationId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("medicationId") ?: 0
            EditMedicationScreen(
                medicationId = id.takeIf { it > 0 },
                memberId = null,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            Routes.MEMBER_MEDICATION,
            arguments = listOf(navArgument("memberId") { type = NavType.IntType })
        ) { entry ->
            val mid = entry.arguments?.getInt("memberId") ?: 0
            MemberMedicationScreen(
                memberId = mid,
                onBack = { navController.popBackStack() },
                onSendRequest = { navController.navigate(Routes.sendMedicationRequest(mid)) }
            )
        }
        composable(
            Routes.SEND_MEDICATION_REQUEST,
            arguments = listOf(navArgument("memberId") { type = NavType.IntType })
        ) { entry ->
            val mid = entry.arguments?.getInt("memberId") ?: 0
            SendMedicationRequestScreen(
                memberId = mid,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }

        // --------- Health Report ---------
        composable(
            Routes.HEALTH_REPORT,
            arguments = listOf(navArgument("memberId") { type = NavType.IntType })
        ) { entry ->
            val mid = entry.arguments?.getInt("memberId") ?: 0
            HealthReportScreen(
                memberId = mid,
                onBack = { navController.popBackStack() }
            )
        }

        // --------- Hospitals ---------
        composable(Routes.HOSPITALS_MAP) {
            HospitalsMapScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { hospital ->
                    val json = Gson().toJson(hospital)
                    navController.navigate(Routes.hospitalDetail(json))
                }
            )
        }
        composable(
            Routes.HOSPITAL_DETAIL,
            arguments = listOf(navArgument("hospitalJson") { type = NavType.StringType })
        ) { entry ->
            val json = entry.arguments?.getString("hospitalJson") ?: ""
            val hospital = Gson().fromJson(json, Hospital::class.java)
            HospitalMapDetailScreen(
                hospital = hospital,
                onBack = { navController.popBackStack() }
            )
        }

        // --------- Appointments ---------
        composable(Routes.APPOINTMENTS) {
            AppointmentsScreen(
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.ADD_APPOINTMENT) }
            )
        }
        composable(Routes.ADD_APPOINTMENT) {
            AddAppointmentScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }

        // --------- Digital Human (医小智) ---------
        composable(Routes.DIGITAL_HUMAN) {
            DigitalHumanScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // --------- Schedule ---------
        composable(Routes.SCHEDULE) {
            ScheduleScreen(onBack = { navController.popBackStack() })
        }

        // --------- Activities ---------
        composable(Routes.RECENT_ACTIVITIES) {
            ActivitiesScreen(
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }

        // --------- Settings ---------
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                onPrivacyPolicy = { navController.navigate(Routes.PRIVACY_POLICY) },
                onUserAgreement = { navController.navigate(Routes.USER_AGREEMENT) },
                onProfile = { navController.navigate(Routes.PROFILE_CENTER) },
                onLogout = {
                    navController.navigate(Routes.MAIN_TABS) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    viewModel.logout()
                },
                onClearCache = {
                    navController.navigate(Routes.SPLASH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    viewModel.logout()
                }
            )
        }

        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY_POLICY) {
            PolicyViewScreen(title = "隐私政策", onBack = { navController.popBackStack() })
        }

        composable(Routes.USER_AGREEMENT) {
            PolicyViewScreen(title = "用户服务协议", onBack = { navController.popBackStack() })
        }
    }

    LaunchedEffect(
        uiState.isChecking,
        uiState.isAuthenticated,
        uiState.requiresProfileCompletion,
        uiState.hasAcceptedDisclaimer
    ) {
        if (!uiState.hasAcceptedDisclaimer) {
            return@LaunchedEffect
        }

        val target = when {
            uiState.isChecking -> Routes.SPLASH
            !uiState.isAuthenticated -> Routes.LOGIN
            uiState.requiresProfileCompletion -> Routes.PROFILE_COMPLETION
            else -> Routes.MAIN_TABS
        }

        // Only run the auto-navigation when the user is on the auth flow
        // (splash / login / register / profile completion). Once the user is
        // inside the app and pushing screens (family detail, medication edit,
        // etc.) we must not yank them back to MAIN_TABS just because the auth
        // state was re-emitted, otherwise we crash with "destination is not in
        // the back stack".
        val current = navController.currentDestination?.route
        val onAuthFlow = current == null ||
            current == Routes.SPLASH ||
            current == Routes.LOGIN ||
            current == Routes.REGISTER ||
            current == Routes.PROFILE_COMPLETION
        if (!onAuthFlow && target == Routes.MAIN_TABS) return@LaunchedEffect

        if (current != target) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (!uiState.hasAcceptedDisclaimer) {
        DisclaimerDialog(
            onAccept = { viewModel.onDisclaimerAccepted() }
        )
    }
}
