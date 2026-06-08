package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.AcceptInvitationResponse
import com.edistrive.aura.data.model.CreateFamilyMemberRequest
import com.edistrive.aura.data.model.FamilyMember
import com.edistrive.aura.data.model.InvitationCodeBody
import com.edistrive.aura.data.model.PreviewInvitationResponse
import com.edistrive.aura.data.model.UpdateDisplayNameRequest
import com.edistrive.aura.data.model.UpdateFamilyMemberRequest
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val isLoading: Boolean = false,
    val activeMembers: List<FamilyMember> = emptyList(),
    val pendingMembers: List<FamilyMember> = emptyList(),
    val errorMessage: String? = null,
    val toast: String? = null,
    val generatedInviteCode: String? = null,
    val isWorking: Boolean = false
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    fun loadAll() {
        loadActive()
        loadPending()
    }

    fun loadActive() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching {
                apiService.getFamilyMembers(listType = "active")
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeMembers = resp.body()?.results.orEmpty(),
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "加载家庭成员失败：${resp.code()}"
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

    fun loadPending() {
        viewModelScope.launch {
            runCatching {
                apiService.getFamilyMembers(listType = "pending")
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        pendingMembers = resp.body()?.results.orEmpty()
                    )
                }
            }
        }
    }

    fun createMember(request: CreateFamilyMemberRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching { apiService.createFamilyMember(request) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        loadActive()
                        _uiState.value = _uiState.value.copy(isWorking = false, toast = "成员已添加")
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

    fun updateMember(
        memberId: Int,
        request: UpdateFamilyMemberRequest,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching { apiService.updateFamilyMember(memberId, request) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        loadAll()
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

    fun updateDisplayName(memberId: Int, displayName: String) {
        viewModelScope.launch {
            runCatching {
                apiService.patchFamilyMember(memberId, UpdateDisplayNameRequest(displayName))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    loadActive()
                    _uiState.value = _uiState.value.copy(toast = "名称已更新")
                }
            }
        }
    }

    fun activate(memberId: Int) {
        viewModelScope.launch {
            runCatching { apiService.activateFamilyMember(memberId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        loadAll()
                        _uiState.value = _uiState.value.copy(toast = "成员已激活")
                    }
                }
        }
    }

    fun unlink(memberId: Int) {
        viewModelScope.launch {
            runCatching { apiService.unlinkFamilyMember(memberId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        loadAll()
                        _uiState.value = _uiState.value.copy(toast = "已解除关联")
                    }
                }
        }
    }

    fun delete(memberId: Int) {
        viewModelScope.launch {
            runCatching { apiService.deleteFamilyMember(memberId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        loadAll()
                        _uiState.value = _uiState.value.copy(toast = "已删除")
                    }
                }
        }
    }

    fun generateInviteCode(onResult: (String?, String?) -> Unit) {
        viewModelScope.launch {
            runCatching { apiService.generateInviteCode() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val code = resp.body()?.invitation_code
                        if (code != null) {
                            _uiState.value = _uiState.value.copy(generatedInviteCode = code)
                            onResult(code, null)
                        } else {
                            onResult(null, "生成失败，请重试")
                        }
                    } else {
                        val errMsg = try {
                            resp.errorBody()?.string()?.let { err ->
                                com.google.gson.Gson().fromJson(err, Map::class.java)?.get("detail")?.toString()
                            }
                        } catch (_: Exception) { null }
                        onResult(null, errMsg ?: "生成失败，请重试")
                    }
                }
                .onFailure {
                    onResult(null, it.message ?: "网络异常，请重试")
                }
        }
    }

    fun clearGeneratedCode() {
        _uiState.value = _uiState.value.copy(generatedInviteCode = null)
    }

    fun previewInvitation(code: String, onResult: (PreviewInvitationResponse?, String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                apiService.previewInvitation(InvitationCodeBody(code))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    onResult(resp.body(), null)
                } else {
                    val errMsg = try {
                        resp.errorBody()?.string()?.let { err ->
                            com.google.gson.Gson().fromJson(err, Map::class.java)?.get("detail")?.toString()
                        }
                    } catch (_: Exception) { null }
                    onResult(null, errMsg ?: "邀请码无效或已过期")
                }
            }.onFailure {
                onResult(null, it.message ?: "网络异常，请重试")
            }
        }
    }

    fun acceptInvitation(code: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                apiService.acceptInvitation(InvitationCodeBody(code))
            }.onSuccess { resp ->
                val body = resp.body()
                if (resp.isSuccessful && body?.success == true) {
                    loadAll()
                    _uiState.value = _uiState.value.copy(toast = body.message ?: "关联成功")
                    onResult(true, body.message)
                } else {
                    val errMsg = try {
                        resp.errorBody()?.string()?.let { err ->
                            com.google.gson.Gson().fromJson(err, Map::class.java)?.get("detail")?.toString()
                        }
                    } catch (_: Exception) { null }
                    onResult(false, errMsg ?: body?.message ?: "关联失败")
                }
            }.onFailure {
                onResult(false, it.message ?: "网络异常，请重试")
            }
        }
    }

    fun setToast(message: String) {
        _uiState.value = _uiState.value.copy(toast = message)
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toast = null)
    }
}
