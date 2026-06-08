package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.CreateScheduleRequest
import com.edistrive.aura.data.model.ScheduleResponse
import com.edistrive.aura.data.model.ScheduleStatsResponse
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val schedules: List<ScheduleResponse> = emptyList(),
    val stats: ScheduleStatsResponse? = null,
    val filterType: String? = null,
    val viewMode: ViewMode = ViewMode.LIST,
    val selectedDate: String? = null,
    val toast: String? = null
)

enum class ViewMode { LIST, CALENDAR }

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    fun loadSchedules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.getSchedules()
                if (response.isSuccessful) {
                    val body = response.body()
                    val schedules = body?.results ?: body?.schedules ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        schedules = schedules
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, toast = "加载失败: ${e.message}")
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val response = apiService.getScheduleStats()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(stats = response.body())
                }
            } catch (_: Exception) {}
        }
    }

    fun createSchedule(request: CreateScheduleRequest) {
        viewModelScope.launch {
            try {
                val response = apiService.createSchedule(request)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(toast = "日程创建成功")
                    loadSchedules()
                    loadStats()
                } else {
                    _uiState.value = _uiState.value.copy(toast = "创建失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toast = "创建失败: ${e.message}")
            }
        }
    }

    fun updateSchedule(id: Int, request: CreateScheduleRequest) {
        viewModelScope.launch {
            try {
                val response = apiService.updateSchedule(id, request)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(toast = "日程更新成功")
                    loadSchedules()
                } else {
                    _uiState.value = _uiState.value.copy(toast = "更新失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(toast = "更新失败: ${e.message}")
            }
        }
    }

    fun deleteSchedule(id: Int) {
        viewModelScope.launch {
            // 乐观更新
            val current = _uiState.value.schedules.toMutableList()
            current.removeAll { it.id == id }
            _uiState.value = _uiState.value.copy(schedules = current)
            try {
                val response = apiService.deleteSchedule(id)
                if (!response.isSuccessful) {
                    loadSchedules()
                } else {
                    loadStats()
                }
            } catch (_: Exception) {
                loadSchedules()
            }
        }
    }

    fun toggleComplete(id: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.toggleScheduleComplete(id)
                if (response.isSuccessful) {
                    val updated = response.body()?.schedule
                    val schedules = _uiState.value.schedules.map {
                        if (it.id == id) it.copy(is_completed = updated?.is_completed) else it
                    }
                    _uiState.value = _uiState.value.copy(schedules = schedules)
                    loadStats()
                }
            } catch (_: Exception) {}
        }
    }

    fun setFilterType(type: String?) {
        _uiState.value = _uiState.value.copy(filterType = type)
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun setSelectedDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toast = null)
    }

    fun filteredSchedules(): List<ScheduleResponse> {
        val filters = _uiState.value.filterType
        val all = _uiState.value.schedules
        return if (filters == null) all else all.filter { it.schedule_type == filters }
    }
}
