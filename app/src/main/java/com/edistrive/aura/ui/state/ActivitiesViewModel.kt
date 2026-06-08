package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.UserActivity
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivitiesUiState(
    val isLoading: Boolean = false,
    val activities: List<UserActivity> = emptyList(),
    val filteredActivities: List<UserActivity> = emptyList(),
    val selectedFilter: String = "all",
    val error: String? = null
)

data class ActivityFilter(
    val key: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivitiesUiState())
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()

    fun loadActivities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                apiService.getRecentActivities(limit = 50)
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val activities = if (body?.success == true) body.activities else emptyList()
                    _uiState.value = _uiState.value.copy(isLoading = false, activities = activities)
                    applyFilter()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "加载失败")
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "网络连接失败")
            }
        }
    }

    fun setFilter(key: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = key)
        applyFilter()
    }

    private fun applyFilter() {
        val state = _uiState.value
        val filtered = when (state.selectedFilter) {
            "all" -> state.activities
            "account" -> state.activities.filter {
                it.type in listOf("phone_change", "email_change", "password_change", "profile_update")
            }
            else -> state.activities.filter { it.type == state.selectedFilter }
        }
        _uiState.value = state.copy(filteredActivities = filtered)
    }

    companion object {
        val filters = listOf(
            "all" to "全部",
            "medical_record" to "病历",
            "ai_chat" to "AI对话",
            "profile_update" to "个人信息",
            "phone_change" to "手机号",
            "email_change" to "邮箱",
            "password_change" to "密码",
            "login" to "登录",
            "health_profile" to "健康档案",
            "medication_reminder" to "用药提醒",
            "account" to "账号相关"
        )
    }
}
