package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.local.AuthPreferences
import com.edistrive.aura.data.local.DisclaimerPreferences
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val apiService: ApiService,
    private val authPreferences: AuthPreferences,
    private val disclaimerPreferences: DisclaimerPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppUiState(
            isChecking = true,
            isAuthenticated = false,
            requiresProfileCompletion = false,
            currentUser = null,
            hasAcceptedDisclaimer = disclaimerPreferences.hasAcceptedDisclaimer()
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        checkAuth()
    }

    fun checkAuth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isChecking = true,
                hasAcceptedDisclaimer = disclaimerPreferences.hasAcceptedDisclaimer()
            )

            val token = authPreferences.getToken()
            if (token.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    isAuthenticated = false,
                    requiresProfileCompletion = false,
                    currentUser = null
                )
                return@launch
            }

            try {
                val resp = apiService.getCurrentUser()
                if (resp.isSuccessful) {
                    val user = resp.body()
                    val requiresProfileCompletion = !(user?.profile_completed ?: false)
                    if (user?.id != null) {
                        authPreferences.saveUserId(user.id)
                    }
                    if (!user?.username.isNullOrBlank()) {
                        authPreferences.saveUsername(user!!.username!!)
                    }

                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        isAuthenticated = true,
                        requiresProfileCompletion = requiresProfileCompletion,
                        currentUser = user
                    )
                } else {
                    authPreferences.clear()
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        isAuthenticated = false,
                        requiresProfileCompletion = false,
                        currentUser = null
                    )
                }
            } catch (_: Exception) {
                authPreferences.clear()
                _uiState.value = _uiState.value.copy(
                    isChecking = false,
                    isAuthenticated = false,
                    requiresProfileCompletion = false,
                    currentUser = null
                )
            }
        }
    }

    fun onDisclaimerAccepted() {
        disclaimerPreferences.setAcceptedDisclaimer(true)
        _uiState.value = _uiState.value.copy(hasAcceptedDisclaimer = true)
    }

    fun onLoginSuccess() {
        checkAuth()
    }

    fun logout() {
        viewModelScope.launch {
            try {
                apiService.logout()
            } catch (_: Exception) {
                // ignore
            }
            authPreferences.clear()
            _uiState.value = _uiState.value.copy(
                isAuthenticated = false,
                requiresProfileCompletion = false,
                currentUser = null
            )
        }
    }

    fun onProfileCompleted() {
        _uiState.value = _uiState.value.copy(requiresProfileCompletion = false)
    }

    fun setSelectedTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
}
