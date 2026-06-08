package com.edistrive.aura.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.model.BatchDeleteRequest
import com.edistrive.aura.data.model.NotificationItem
import com.edistrive.aura.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val isLoading: Boolean = false,
    val messages: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null,
    val toast: String? = null,
    val toastIsError: Boolean = false,
    val isWorking: Boolean = false,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching {
                apiService.getNotifications()
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val messages = resp.body()?.results.orEmpty()
                    val unread = messages.count { it.is_read == false }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages,
                        unreadCount = unread,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "加载消息失败"
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

    fun markAsRead(messageId: Int) {
        viewModelScope.launch {
            runCatching {
                apiService.markNotificationRead(messageId)
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val updated = _uiState.value.messages.map {
                        if (it.id == messageId) it.copy(is_read = true) else it
                    }
                    val unread = updated.count { it.is_read == false }
                    _uiState.value = _uiState.value.copy(messages = updated, unreadCount = unread)
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching {
                apiService.markAllNotificationsRead()
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val updated = _uiState.value.messages.map { it.copy(is_read = true) }
                    _uiState.value = _uiState.value.copy(
                        messages = updated,
                        unreadCount = 0,
                        isWorking = false,
                        toast = "已全部标记为已读"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isWorking = false)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isWorking = false)
            }
        }
    }

    fun deleteMessages(ids: List<Int>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            runCatching {
                apiService.batchDeleteNotifications(BatchDeleteRequest(ids))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val idsSet = ids.toSet()
                    val updated = _uiState.value.messages.filter { it.id !in idsSet }
                    val unread = updated.count { it.is_read == false }
                    _uiState.value = _uiState.value.copy(
                        messages = updated,
                        unreadCount = unread,
                        isWorking = false,
                        toast = "已删除"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        toast = "删除失败",
                        toastIsError = true
                    )
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    toast = "删除失败",
                    toastIsError = true
                )
            }
        }
    }

    fun refreshMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            runCatching {
                apiService.getNotifications()
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val messages = resp.body()?.results.orEmpty()
                    val unread = messages.count { it.is_read == false }
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        unreadCount = unread,
                        isRefreshing = false,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toast = null, toastIsError = false)
    }
}
