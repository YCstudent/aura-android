package com.edistrive.aura.ui.state

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.AiAnalyzeRequest
import com.edistrive.aura.data.model.CreateMedicalRecordRequest
import com.edistrive.aura.data.model.MedicalRecord
import com.edistrive.aura.data.model.OcrRecognizeResponse
import com.edistrive.aura.data.model.StructuredMedicalInfo
import com.edistrive.aura.data.model.UpdateMedicalRecordRequest
import com.edistrive.aura.data.network.ApiService
import com.edistrive.aura.util.MultipartBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicalRecordsUiState(
    val isLoading: Boolean = false,
    val records: List<MedicalRecord> = emptyList(),
    val errorMessage: String? = null,
    val toast: String? = null,
    val filterType: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val searchText: String = "",
    val isWorking: Boolean = false,
    val ocrResults: List<OcrItemState> = emptyList(),
    val structured: StructuredMedicalInfo? = null,
    val aiAnalyzing: Boolean = false
)

data class OcrItemState(
    val uri: Uri,
    val displayName: String,
    val text: String = "",
    val isRecognizing: Boolean = false,
    val failed: Boolean = false
)

val MedicalRecordTypes = listOf(
    "" to "全部",
    "门诊" to "门诊",
    "急诊" to "急诊",
    "住院" to "住院",
    "体检" to "体检"
)

@HiltViewModel
class MedicalRecordsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalRecordsUiState())
    val uiState: StateFlow<MedicalRecordsUiState> = _uiState.asStateFlow()

    fun load(memberId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                apiService.getMedicalRecords(memberId = memberId)
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        records = resp.body()?.results.orEmpty()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "加载失败：${resp.code()}"
                    )
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = it.message ?: "网络异常"
                )
            }
        }
    }

    fun setSearch(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
    }

    fun setFilterType(type: String) {
        _uiState.value = _uiState.value.copy(filterType = type)
    }

    fun setDateRange(start: String, end: String) {
        _uiState.value = _uiState.value.copy(startDate = start, endDate = end)
    }

    fun create(
        request: CreateMedicalRecordRequest,
        imageUris: List<Uri>,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching { apiService.createMedicalRecord(request) }
                .onSuccess { resp ->
                    val recordId = resp.body()?.id
                    if (resp.isSuccessful && recordId != null) {
                        uploadImagesIfNeeded(recordId, imageUris)
                        load()
                        _uiState.value = _uiState.value.copy(isWorking = false, toast = "已保存")
                        onResult(true, null)
                    } else {
                        _uiState.value = _uiState.value.copy(isWorking = false)
                        onResult(false, "保存失败：${resp.code()}")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isWorking = false)
                    onResult(false, it.message ?: "保存失败")
                }
        }
    }

    fun update(
        recordId: Int,
        request: UpdateMedicalRecordRequest,
        imageUris: List<Uri>,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching { apiService.updateMedicalRecord(recordId, request) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        uploadImagesIfNeeded(recordId, imageUris)
                        load()
                        _uiState.value = _uiState.value.copy(isWorking = false, toast = "已保存")
                        onResult(true, null)
                    } else {
                        _uiState.value = _uiState.value.copy(isWorking = false)
                        onResult(false, "保存失败：${resp.code()}")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isWorking = false)
                    onResult(false, it.message ?: "保存失败")
                }
        }
    }

    fun delete(recordId: Int) {
        viewModelScope.launch {
            runCatching { apiService.deleteMedicalRecord(recordId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        load()
                        _uiState.value = _uiState.value.copy(toast = "已删除")
                    }
                }
        }
    }

    private suspend fun uploadImagesIfNeeded(recordId: Int, imageUris: List<Uri>) {
        if (imageUris.isEmpty()) return
        val parts = imageUris.mapNotNull { uri ->
            MultipartBuilder.imagePartFromUri(context, uri, fieldName = "images")
        }
        if (parts.isEmpty()) return
        runCatching { apiService.uploadMedicalRecordImages(recordId, parts) }
    }

    // ----- OCR & AI -----

    fun addOcrImages(uris: List<Uri>, displayNames: List<String>) {
        val newItems = uris.zip(displayNames).map { (uri, name) ->
            OcrItemState(uri = uri, displayName = name, isRecognizing = true)
        }
        _uiState.value = _uiState.value.copy(ocrResults = _uiState.value.ocrResults + newItems)
        newItems.forEach { triggerRecognize(it.uri) }
    }

    fun removeOcrItem(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            ocrResults = _uiState.value.ocrResults.filterNot { it.uri == uri }
        )
    }

    private fun triggerRecognize(uri: Uri) {
        viewModelScope.launch {
            val part = MultipartBuilder.imagePartFromUri(context, uri, fieldName = "image") ?: run {
                updateOcrItem(uri) { it.copy(isRecognizing = false, failed = true) }
                return@launch
            }
            runCatching { apiService.ocrRecognize(part) }
                .onSuccess { resp ->
                    val body = resp.body()
                    if (resp.isSuccessful && body?.success == true) {
                        updateOcrItem(uri) {
                            it.copy(isRecognizing = false, text = body.ocr_text.orEmpty())
                        }
                        if (body.structured_data != null && _uiState.value.structured == null) {
                            _uiState.value = _uiState.value.copy(structured = body.structured_data)
                        }
                    } else {
                        updateOcrItem(uri) { it.copy(isRecognizing = false, failed = true) }
                    }
                }
                .onFailure {
                    updateOcrItem(uri) { it.copy(isRecognizing = false, failed = true) }
                }
        }
    }

    fun aiAnalyze(symptoms: String, onResult: (StructuredMedicalInfo?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aiAnalyzing = true)
            val ocrText = _uiState.value.ocrResults.joinToString("\n") { it.text }
            runCatching {
                apiService.aiAnalyze(AiAnalyzeRequest(symptoms = symptoms, ocr_text = ocrText.ifBlank { null }))
            }.onSuccess { resp ->
                _uiState.value = _uiState.value.copy(aiAnalyzing = false)
                val body = resp.body()
                if (resp.isSuccessful && body?.success == true) {
                    val merged = body.structured_data ?: StructuredMedicalInfo(
                        possible_diseases = body.possible_diseases,
                        suggestion_desc = body.suggestion_desc,
                        suggestion_tests = body.suggestion_tests,
                        treatment_rx = body.treatment_rx,
                        treatment_otc = body.treatment_otc
                    )
                    _uiState.value = _uiState.value.copy(structured = merged)
                    onResult(merged)
                } else {
                    onResult(null)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(aiAnalyzing = false)
                onResult(null)
            }
        }
    }

    fun clearOcrAndStructured() {
        _uiState.value = _uiState.value.copy(ocrResults = emptyList(), structured = null)
    }

    fun filteredRecords(): List<MedicalRecord> {
        val s = _uiState.value
        var result = s.records

        if (s.searchText.isNotBlank()) {
            val keyword = s.searchText.lowercase()
            result = result.filter { rec ->
                rec.title?.lowercase()?.contains(keyword) == true ||
                rec.symptoms?.lowercase()?.contains(keyword) == true ||
                rec.possible_diseases?.lowercase()?.contains(keyword) == true
            }
        }

        if (s.filterType.isNotBlank()) {
            result = result.filter { it.effectiveType == s.filterType }
        }

        val start = s.startDate
        val end = s.endDate
        if (start.isNotBlank() || end.isNotBlank()) {
            result = result.filter { rec ->
                val date = rec.effectiveDate ?: return@filter false
                if (start.isNotBlank() && date < start) return@filter false
                if (end.isNotBlank() && date > end) return@filter false
                true
            }
        }

        return result
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toast = null)
    }

    private fun updateOcrItem(uri: Uri, transform: (OcrItemState) -> OcrItemState) {
        _uiState.value = _uiState.value.copy(
            ocrResults = _uiState.value.ocrResults.map { if (it.uri == uri) transform(it) else it }
        )
    }
}

@HiltViewModel
class MedicalRecordDetailViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    data class DetailState(
        val isLoading: Boolean = false,
        val record: MedicalRecord? = null,
        val errorMessage: String? = null
    )

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { apiService.getMedicalRecord(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.value = DetailState(record = resp.body())
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
