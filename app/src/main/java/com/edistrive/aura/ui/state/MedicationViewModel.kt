package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.CreateMedicationRequest
import com.edistrive.aura.data.model.Medication
import com.edistrive.aura.data.model.MedicationRequestModel
import com.edistrive.aura.data.model.SendMedicationRequestBody
import com.edistrive.aura.data.model.TakeMedicationBody
import com.edistrive.aura.data.model.UpdateMedicationRequest
import com.edistrive.aura.data.network.ApiService
import com.edistrive.aura.data.notification.MedicationReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MedicationsUiState(
    val isLoading: Boolean = false,
    val activeMedications: List<Medication> = emptyList(),
    val todayMedications: List<Medication> = emptyList(),
    val expiredMedications: List<Medication> = emptyList(),
    val requests: List<MedicationRequestModel> = emptyList(),
    val errorMessage: String? = null,
    val toast: String? = null,
    val isWorking: Boolean = false
)

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val apiService: ApiService,
    private val scheduler: MedicationReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicationsUiState())
    val uiState: StateFlow<MedicationsUiState> = _uiState.asStateFlow()

    fun loadAll(memberId: Int? = null) {
        loadMedications(memberId)
        loadTodayMedications(memberId)
        if (memberId == null) loadRequests()
    }

    fun loadMedications(memberId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching { apiService.getMedications(memberId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val list = resp.body()?.results.orEmpty()
                        val (active, expired) = list.partition { it.isActiveToday() }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            activeMedications = active,
                            expiredMedications = expired
                        )
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

    fun loadTodayMedications(memberId: Int? = null) {
        viewModelScope.launch {
            runCatching { apiService.getTodayMedications(memberId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.value = _uiState.value.copy(todayMedications = resp.body().orEmpty())
                    }
                }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            runCatching { apiService.getMedicationRequests() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.value = _uiState.value.copy(requests = resp.body()?.results.orEmpty())
                    }
                }
        }
    }

    fun create(request: CreateMedicationRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching { apiService.createMedication(request) }
                .onSuccess { resp ->
                    val saved = resp.body()
                    if (resp.isSuccessful && saved?.id != null) {
                        scheduleReminders(saved)
                        loadAll(request.family_member)
                        _uiState.value = _uiState.value.copy(isWorking = false, toast = "已保存")
                        onResult(true, null)
                    } else {
                        _uiState.value = _uiState.value.copy(isWorking = false)
                        onResult(false, extractError(resp.errorBody()?.string()) ?: "保存失败：${resp.code()}")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isWorking = false)
                    onResult(false, it.message ?: "保存失败")
                }
        }
    }

    fun update(
        medicationId: Int,
        request: UpdateMedicationRequest,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching { apiService.updateMedication(medicationId, request) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        resp.body()?.let { scheduleReminders(it) }
                        loadAll(request.family_member)
                        _uiState.value = _uiState.value.copy(isWorking = false, toast = "已保存")
                        onResult(true, null)
                    } else {
                        _uiState.value = _uiState.value.copy(isWorking = false)
                        onResult(false, extractError(resp.errorBody()?.string()) ?: "保存失败：${resp.code()}")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isWorking = false)
                    onResult(false, it.message ?: "保存失败")
                }
        }
    }

    fun delete(medicationId: Int) {
        viewModelScope.launch {
            runCatching { apiService.deleteMedication(medicationId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        scheduler.cancel(medicationId)
                        loadAll()
                        _uiState.value = _uiState.value.copy(toast = "已删除")
                    }
                }
        }
    }

    fun toggleActive(medicationId: Int) {
        viewModelScope.launch {
            runCatching { apiService.toggleMedication(medicationId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) loadAll()
                }
        }
    }

    fun take(medicationId: Int, time: String) {
        viewModelScope.launch {
            runCatching { apiService.takeMedication(TakeMedicationBody(medicationId, time)) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        loadAll()
                        _uiState.value = _uiState.value.copy(toast = "已记录")
                    }
                }
        }
    }

    fun cancelTake(medicationId: Int, time: String) {
        viewModelScope.launch {
            runCatching { apiService.cancelMedicationCheckIn(TakeMedicationBody(medicationId, time)) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        loadAll()
                        _uiState.value = _uiState.value.copy(toast = "已撤销")
                    }
                }
        }
    }

    fun acceptRequest(id: Int) {
        viewModelScope.launch {
            runCatching { apiService.acceptMedicationRequest(id) }
                .onSuccess { resp -> if (resp.isSuccessful) loadAll() }
        }
    }

    fun rejectRequest(id: Int) {
        viewModelScope.launch {
            runCatching { apiService.rejectMedicationRequest(id) }
                .onSuccess { resp -> if (resp.isSuccessful) loadRequests() }
        }
    }

    fun withdrawRequest(id: Int) {
        viewModelScope.launch {
            runCatching { apiService.withdrawMedicationRequest(id) }
                .onSuccess { resp -> if (resp.isSuccessful) loadRequests() }
        }
    }

    fun deleteRequest(id: Int) {
        viewModelScope.launch {
            runCatching { apiService.deleteMedicationRequest(id) }
                .onSuccess { resp -> if (resp.isSuccessful) loadRequests() }
        }
    }

    fun sendRequest(body: SendMedicationRequestBody, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching { apiService.sendMedicationRequest(body) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.value = _uiState.value.copy(isWorking = false, toast = "请求已发送")
                        onResult(true, null)
                    } else {
                        _uiState.value = _uiState.value.copy(isWorking = false)
                        onResult(false, extractError(resp.errorBody()?.string()) ?: "发送失败：${resp.code()}")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isWorking = false)
                    onResult(false, it.message ?: "发送失败")
                }
        }
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toast = null)
    }

    private fun extractError(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val gson = com.google.gson.Gson()
            val map = gson.fromJson(errorBody, Map::class.java) ?: return null
            // Prefer "detail" style (used by family endpoints)
            map["detail"]?.toString()?.let { return it }
            // Handle DRF field-level errors: {"name":["msg"],"dosage":["msg"]}
            @Suppress("UNCHECKED_CAST")
            val messages = map.entries.mapNotNull { (key, value) ->
                when (value) {
                    is List<*> -> value.firstOrNull()?.let { "$key: $it" }
                    is String -> if (value.isNotBlank()) "$key: $value" else null
                    else -> null
                }
            }
            messages.joinToString("；").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun scheduleReminders(med: Medication) {
        val id = med.id ?: return
        scheduler.schedule(
            medicationId = id,
            medicationName = med.name.orEmpty(),
            dosage = med.dosage,
            reminderTimes = med.reminder_times.orEmpty(),
            startIso = med.start_date,
            endIso = med.end_date
        )
    }
}

private fun Medication.isActiveToday(): Boolean {
    val today = LocalDate.now()
    if (is_active == false) return false
    val end = end_date?.let { runCatching { LocalDate.parse(it.substring(0, 10)) }.getOrNull() }
    if (end != null && end.isBefore(today)) return false
    return true
}

@HiltViewModel
class MedicationDetailViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {
    data class State(
        val isLoading: Boolean = false,
        val medication: Medication? = null,
        val errorMessage: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { apiService.getMedication(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = State(medication = resp.body())
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = "加载失败：${resp.code()}"
                        )
                    }
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = it.message ?: "网络异常"
                    )
                }
        }
    }
}
