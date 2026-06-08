package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.ChangePasswordRequest
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun changePassword(request: ChangePasswordRequest) {
        viewModelScope.launch {
            _uiState.value = ChangePasswordUiState(isLoading = true)
            try {
                val response = apiService.changePassword(request)
                if (response.isSuccessful) {
                    _uiState.value = ChangePasswordUiState(success = true)
                } else {
                    _uiState.value = ChangePasswordUiState(error = response.errorBody()?.string() ?: "修改失败，请重试")
                }
            } catch (e: Exception) {
                _uiState.value = ChangePasswordUiState(error = e.message ?: "网络错误")
            }
        }
    }
}
