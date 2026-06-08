package com.edistrive.aura.ui.state

import com.edistrive.aura.data.model.User

data class AppUiState(
    val isChecking: Boolean = true,
    val isAuthenticated: Boolean = false,
    val requiresProfileCompletion: Boolean = false,
    val currentUser: User? = null,
    val hasAcceptedDisclaimer: Boolean = false,
    val selectedTab: Int = 0
)
