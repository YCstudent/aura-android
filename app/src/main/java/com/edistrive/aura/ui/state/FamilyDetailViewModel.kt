package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.FamilyMember
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyDetailUiState(
    val isLoading: Boolean = false,
    val member: FamilyMember? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class FamilyDetailViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyDetailUiState())
    val uiState: StateFlow<FamilyDetailUiState> = _uiState.asStateFlow()

    fun load(memberId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching { apiService.getFamilyMember(memberId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.value = FamilyDetailUiState(isLoading = false, member = resp.body())
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "加载失败：${resp.code()}"
                        )
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = it.message ?: "网络异常"
                    )
                }
        }
    }
}
