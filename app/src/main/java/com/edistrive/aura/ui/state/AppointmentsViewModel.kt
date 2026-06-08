package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.Appointment
import com.edistrive.aura.data.model.AppointmentStatisticsResponse
import com.edistrive.aura.data.model.CreateAppointmentRequest
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppointmentsUiState(
    val isLoading: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val statistics: AppointmentStatisticsResponse = AppointmentStatisticsResponse(),
    val selectedTab: Int = 0, // 0=pending, 1=completed, 2=cancelled
    val error: String? = null,
    val toast: String? = null,
    val toastIsError: Boolean = false
)

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    fun loadAppointments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { apiService.getAppointments() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val results = resp.body()?.results.orEmpty()
                        _uiState.value = _uiState.value.copy(isLoading = false, appointments = results)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "加载预约失败")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "网络连接失败")
                }
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            runCatching { apiService.getAppointmentStatistics() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        resp.body()?.let { stats ->
                            _uiState.value = _uiState.value.copy(statistics = stats)
                        }
                    }
                }
        }
    }

    fun setSelectedTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun completeAppointment(appointment: Appointment) {
        val id = appointment.id ?: return
        viewModelScope.launch {
            runCatching { apiService.completeAppointment(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.value = _uiState.value.copy(toast = "已完成预约")
                        loadAppointments()
                        loadStatistics()
                    } else {
                        _uiState.value = _uiState.value.copy(toast = "操作失败", toastIsError = true)
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(toast = "网络异常", toastIsError = true)
                }
        }
    }

    fun cancelAppointment(appointment: Appointment) {
        val id = appointment.id ?: return
        viewModelScope.launch {
            runCatching { apiService.cancelAppointment(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.value = _uiState.value.copy(toast = "已取消预约")
                        loadAppointments()
                        loadStatistics()
                    } else {
                        _uiState.value = _uiState.value.copy(toast = "操作失败", toastIsError = true)
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(toast = "网络异常", toastIsError = true)
                }
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        val id = appointment.id ?: return
        viewModelScope.launch {
            runCatching { apiService.deleteAppointment(id) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(toast = "已删除预约")
                    loadAppointments()
                    loadStatistics()
                }.onFailure {
                    _uiState.value = _uiState.value.copy(toast = "网络异常", toastIsError = true)
                }
        }
    }

    fun createAppointment(request: CreateAppointmentRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching { apiService.createAppointment(request) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.value = _uiState.value.copy(isLoading = false, toast = "预约创建成功")
                        loadAppointments()
                        loadStatistics()
                        onResult(true, null)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onResult(false, "创建失败")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onResult(false, "网络异常")
                }
        }
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toast = null, toastIsError = false)
    }
}
