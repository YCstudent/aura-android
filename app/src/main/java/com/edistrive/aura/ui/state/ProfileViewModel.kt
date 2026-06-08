package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.ChangeEmailRequest
import com.edistrive.aura.data.model.ChangePhoneRequest
import com.edistrive.aura.data.model.SendCodeRequest
import com.edistrive.aura.data.model.UpdateProfileRequest
import com.edistrive.aura.data.model.User
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val user: User? = null,
    val avatarVersion: Int = 0,
    val height: String = "",
    val weight: String = "",
    val bloodType: String = "",
    val medicalHistory: String = "",
    val allergyHistory: String = "",
    val chronicDiseases: String = "",
    val surgeryHistory: String = "",
    val medicationHistory: String = "",
    val notes: String = "",
    val toast: String? = null,
    val toastIsError: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching { apiService.getCurrentUser() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val user = resp.body()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = user,
                            height = user?.height?.let { if (it == 0.0) "" else it.toString() } ?: "",
                            weight = user?.weight?.let { if (it == 0.0) "" else it.toString() } ?: "",
                            bloodType = user?.blood_type ?: "",
                            medicalHistory = user?.medical_history ?: "",
                            allergyHistory = user?.allergy_history ?: "",
                            chronicDiseases = user?.chronic_diseases ?: "",
                            surgeryHistory = user?.surgery_history ?: "",
                            medicationHistory = user?.medication_history ?: "",
                            notes = user?.notes ?: ""
                        )
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, toast = "加载失败", toastIsError = true)
                }
        }
    }

    fun updateBasicProfile(username: String? = null, signature: String? = null, gender: String? = null, birthDate: String? = null) {
        viewModelScope.launch {
            runCatching {
                apiService.updateProfile(UpdateProfileRequest(
                    username = username,
                    signature = signature,
                    gender = gender,
                    birth_date = birthDate
                ))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val user = resp.body()
                    _uiState.value = _uiState.value.copy(user = user, toast = "保存成功")
                    // Refresh HomeViewModel data via loadData won't work cross-VM
                    // Rely on re-fetch when navigating back
                } else {
                    _uiState.value = _uiState.value.copy(toast = "保存失败", toastIsError = true)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(toast = "网络异常", toastIsError = true)
            }
        }
    }

    fun saveHealthProfile() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            runCatching {
                apiService.updateProfile(UpdateProfileRequest(
                    height = state.height.toDoubleOrNull(),
                    weight = state.weight.toDoubleOrNull(),
                    blood_type = state.bloodType.ifBlank { null },
                    medical_history = state.medicalHistory,
                    allergy_history = state.allergyHistory,
                    chronic_diseases = state.chronicDiseases,
                    surgery_history = state.surgeryHistory,
                    medication_history = state.medicationHistory,
                    notes = state.notes,
                    profile_completed = true
                ))
            }.onSuccess { resp ->
                _uiState.value = _uiState.value.copy(isSaving = false)
                if (resp.isSuccessful) {
                    val user = resp.body()
                    _uiState.value = _uiState.value.copy(user = user, toast = "健康档案保存成功")
                } else {
                    _uiState.value = _uiState.value.copy(toast = "保存失败", toastIsError = true)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, toast = "网络异常", toastIsError = true)
            }
        }
    }

    fun uploadAvatar(file: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("avatar", file.name, requestBody)
            runCatching { apiService.uploadAvatar(part) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        // 上传成功后重新拉取完整用户数据，避免用上传接口返回的局部对象覆盖个人信息
                        refreshUserProfile()
                        _uiState.value = _uiState.value.copy(isSaving = false, toast = "头像上传成功")
                    } else {
                        _uiState.value = _uiState.value.copy(isSaving = false, toast = "头像上传失败", toastIsError = true)
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSaving = false, toast = "网络异常", toastIsError = true)
                }
        }
    }

    private suspend fun refreshUserProfile() {
        runCatching { apiService.getCurrentUser() }
            .onSuccess { resp ->
                if (resp.isSuccessful) {
                    val user = resp.body()
                    val current = _uiState.value
                    _uiState.value = current.copy(
                        user = user,
                        avatarVersion = current.avatarVersion + 1
                    )
                }
            }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            runCatching { apiService.deleteAccount() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, toast = "账号已注销")
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSaving = false, toast = "注销失败", toastIsError = true)
                }
        }
    }

    fun updateHeight(value: String) { _uiState.value = _uiState.value.copy(height = value) }
    fun updateWeight(value: String) { _uiState.value = _uiState.value.copy(weight = value) }
    fun updateBloodType(value: String) { _uiState.value = _uiState.value.copy(bloodType = value) }
    fun updateMedicalHistory(value: String) { _uiState.value = _uiState.value.copy(medicalHistory = value) }
    fun updateAllergyHistory(value: String) { _uiState.value = _uiState.value.copy(allergyHistory = value) }
    fun updateChronicDiseases(value: String) { _uiState.value = _uiState.value.copy(chronicDiseases = value) }
    fun updateSurgeryHistory(value: String) { _uiState.value = _uiState.value.copy(surgeryHistory = value) }
    fun updateMedicationHistory(value: String) { _uiState.value = _uiState.value.copy(medicationHistory = value) }
    fun updateNotes(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun consumeToast() { _uiState.value = _uiState.value.copy(toast = null, toastIsError = false) }

    fun sendChangeEmailCode(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                apiService.sendCode(SendCodeRequest(email = email, purpose = "change_email"))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val msg = resp.body()
                    if (msg?.success == false) {
                        onError(msg.message ?: "发送失败")
                    } else {
                        onSuccess(msg?.message ?: "验证码已发送")
                    }
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "发送失败")
                }
            }.onFailure {
                onError(it.message ?: "网络错误")
            }
        }
    }

    fun changeEmail(
        newEmail: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                apiService.changeEmail(ChangeEmailRequest(new_email = newEmail, code = code))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val msg = resp.body()
                    if (msg?.success == false) {
                        onError(msg.message ?: "绑定失败")
                    } else {
                        onSuccess()
                    }
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "绑定失败")
                }
            }.onFailure {
                onError(it.message ?: "网络错误")
            }
        }
    }

    fun sendChangePhoneCode(
        phone: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                apiService.sendCode(SendCodeRequest(phone = phone, purpose = "change_phone"))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val msg = resp.body()
                    if (msg?.success == false) {
                        onError(msg.message ?: "发送失败")
                    } else {
                        onSuccess(msg?.message ?: "验证码已发送")
                    }
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "发送失败")
                }
            }.onFailure {
                onError(it.message ?: "网络错误")
            }
        }
    }

    fun changePhone(
        newPhone: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                apiService.changePhone(ChangePhoneRequest(new_phone = newPhone, code = code))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val msg = resp.body()
                    if (msg?.success == false) {
                        onError(msg.message ?: "绑定失败")
                    } else {
                        onSuccess()
                    }
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "绑定失败")
                }
            }.onFailure {
                onError(it.message ?: "网络错误")
            }
        }
    }

    private fun parseError(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = com.google.gson.JsonParser.parseString(body)
            if (!json.isJsonObject) return null
            val obj = json.asJsonObject
            obj["detail"]?.takeIf { it.isJsonPrimitive }?.asString
                ?: obj["message"]?.takeIf { it.isJsonPrimitive }?.asString
                ?: obj["error"]?.takeIf { it.isJsonPrimitive }?.asString
        } catch (_: Exception) { null }
    }
}
