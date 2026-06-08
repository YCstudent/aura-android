package com.edistrive.aura.ui.state

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edistrive.aura.data.local.AuthPreferences
import com.edistrive.aura.data.model.*
import com.edistrive.aura.data.network.ApiService
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import javax.inject.Inject

data class DigitalHumanUiState(
    val conversations: List<Conversation> = emptyList(),
    val currentConversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isLoadingConversations: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isCreatingConversation: Boolean = false,
    val selectedModel: AIModel = AIModel.CHAT,
    val selectedImages: List<Uri> = emptyList(),
    val ocrResults: List<String> = emptyList(),
    val isUploadingImage: Boolean = false,
    val favoriteMessageIds: Set<Int> = emptySet(),
    val emptyConversationIds: Set<Int> = emptySet(),
    val showSidebar: Boolean = false,
    val showActionSheet: Boolean = false,
    val actionSheetMessage: ChatMessage? = null,
    val showDataNotice: Boolean = false,
    val toast: String? = null,
    val currentUsername: String = "",
    val currentAvatarUrl: String? = null
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val apiService: ApiService,
    private val authPreferences: AuthPreferences,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalHumanUiState())
    val uiState: StateFlow<DigitalHumanUiState> = _uiState.asStateFlow()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private var streamJob: Job? = null
    private var lastCreateConversationTime: Long = 0L
    private val cleanupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Streaming buffers
    private var reasoningBuffer = ""
    private var answerBuffer = ""
    private var pendingAnswerBuffer = ""
    private var isReasoningPhaseActive = false
    private var streamFinished = false
    private var flushJob: Job? = null

    init {
        val username = authPreferences.getUsername() ?: ""
        val avatarUrl = authPreferences.getAvatarUrl()
        _uiState.update { it.copy(currentUsername = username, currentAvatarUrl = avatarUrl) }
    }

    override fun onCleared() {
        super.onCleared()
        // Fire-and-forget: delete empty conversations when leaving the screen
        val emptyIds = _uiState.value.emptyConversationIds
        emptyIds.forEach { id ->
            cleanupScope.launch {
                runCatching { apiService.deleteConversation(id) }
            }
        }
    }

    // =========================================================================
    // Conversation CRUD
    // =========================================================================

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConversations = true) }
            runCatching { apiService.getConversations() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val convs = resp.body()?.results ?: emptyList()
                        val nonEmptyConvs = convs.filter { it.messageCount > 0 }
                        _uiState.update { it.copy(conversations = convs, isLoadingConversations = false) }

                        // Clean up empty conversations that exist on server
                        // (created but never sent a message, from any session)
                        val emptyOnServer = convs.filter { it.messageCount == 0 }
                        emptyOnServer.forEach { emptyConv ->
                            viewModelScope.launch {
                                runCatching { apiService.deleteConversation(emptyConv.id) }
                            }
                        }

                        // Auto-select first non-empty conversation if none selected (matching iOS)
                        if (_uiState.value.currentConversation == null && nonEmptyConvs.isNotEmpty()) {
                            selectConversation(nonEmptyConvs.first())
                        }
                    } else {
                        _uiState.update { it.copy(isLoadingConversations = false) }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingConversations = false) }
                }
        }
    }

    fun loadMessages(conversationId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMessages = true) }
            runCatching { apiService.getConversation(conversationId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val conv = resp.body()
                        if (conv != null) {
                            val chatMessages = conv.messages?.map { msg ->
                                ChatMessage(
                                    role = if (msg.role == "user") MessageRole.USER else MessageRole.ASSISTANT,
                                    content = msg.content,
                                    backendId = msg.id,
                                    feedback = msg.feedback ?: 0,
                                    isReasoning = msg.messageType == "reasoning",
                                    reasoningDuration = msg.reasoningDuration?.toDouble(),
                                    timestamp = parseIsoDate(msg.createdAt)
                                )
                            } ?: emptyList()
                            _uiState.update {
                                it.copy(
                                    currentConversation = conv,
                                    messages = chatMessages,
                                    isLoadingMessages = false
                                )
                            }
                            loadFavoriteMessages(conversationId)
                        } else {
                            _uiState.update { it.copy(isLoadingMessages = false) }
                        }
                    } else {
                        _uiState.update { it.copy(isLoadingMessages = false) }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMessages = false) }
                }
        }
    }

    fun selectConversation(conv: Conversation) {
        _uiState.update { it.copy(showSidebar = false, messages = emptyList()) }
        loadMessages(conv.id)
    }

    fun createConversation(title: String = "新对话") {
        // Guard: prevent concurrent creates
        if (_uiState.value.isCreatingConversation) {
            _uiState.update { it.copy(toast = "已在创建新对话，请稍候") }
            return
        }
        // Throttle: 2s between creates (matching iOS)
        val now = System.currentTimeMillis()
        if (now - lastCreateConversationTime < 2000L) {
            _uiState.update { it.copy(toast = "已创建新对话") }
            return
        }
        lastCreateConversationTime = now

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingConversation = true) }
            runCatching { apiService.createConversation(CreateConversationRequest(title)) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val conv = resp.body()
                        if (conv != null) {
                            val conversations = _uiState.value.conversations.toMutableList()
                            conversations.add(0, conv)
                            // Track as empty conversation (no messages sent yet)
                            val emptyIds = _uiState.value.emptyConversationIds + conv.id
                            _uiState.update {
                                it.copy(
                                    conversations = conversations,
                                    currentConversation = conv,
                                    messages = emptyList(),
                                    emptyConversationIds = emptyIds,
                                    isCreatingConversation = false,
                                    showSidebar = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(isCreatingConversation = false, toast = "创建对话失败")
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(isCreatingConversation = false, toast = "创建对话失败")
                        }
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isCreatingConversation = false, toast = "创建对话失败，请稍后重试")
                    }
                }
        }
    }

    fun deleteConversation(conversationId: Int) {
        viewModelScope.launch {
            runCatching { apiService.deleteConversation(conversationId) }
                .onSuccess {
                    val convs = _uiState.value.conversations.filter { it.id != conversationId }
                    val wasCurrent = _uiState.value.currentConversation?.id == conversationId
                    // Auto-select first remaining conversation (matching iOS)
                    val newCurrent = if (wasCurrent) {
                        convs.firstOrNull()
                    } else {
                        _uiState.value.currentConversation
                    }
                    val emptyIds = _uiState.value.emptyConversationIds - conversationId
                    _uiState.update {
                        it.copy(
                            conversations = convs,
                            currentConversation = newCurrent,
                            emptyConversationIds = emptyIds,
                            messages = if (wasCurrent) emptyList() else it.messages
                        )
                    }
                    // Load messages for newly selected conversation
                    if (wasCurrent && newCurrent != null) {
                        loadMessages(newCurrent.id)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(toast = "删除对话失败") }
                }
        }
    }

    fun renameConversation(conversationId: Int, newTitle: String) {
        viewModelScope.launch {
            runCatching {
                apiService.updateConversationTitle(conversationId, UpdateConversationTitleRequest(newTitle))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val convs = _uiState.value.conversations.map {
                        if (it.id == conversationId) it.copy(title = newTitle) else it
                    }
                    val cur = _uiState.value.currentConversation
                    val newCur = if (cur?.id == conversationId) cur.copy(title = newTitle) else cur
                    _uiState.update { it.copy(conversations = convs, currentConversation = newCur) }
                }
            }
        }
    }

    // =========================================================================
    // Send Message (SSE Streaming)
    // =========================================================================

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isSending) return

        val ocrResults = _uiState.value.ocrResults
        val selectedImages = _uiState.value.selectedImages.toList()
        val model = _uiState.value.selectedModel

        // Build full message with OCR results (matching iOS)
        val fullMessage = if (ocrResults.isNotEmpty()) {
            val ocrText = ocrResults.mapIndexed { index, result ->
                if (ocrResults.size > 1) "【图片 ${index + 1}】\n$result" else result
            }.joinToString("\n\n")
            "$text\n\n---\n\n$ocrText"
        } else text

        // Clear input
        _uiState.update {
            it.copy(
                inputText = "",
                ocrResults = emptyList(),
                selectedImages = emptyList(),
                isUploadingImage = false,
                isSending = true
            )
        }

        val currentConv = _uiState.value.currentConversation
        if (currentConv == null) {
            // Create conversation first, then send
            createConversationAndSend(fullMessage, selectedImages, model)
        } else {
            sendToConversation(currentConv.id, fullMessage, selectedImages, model)
        }
    }

    private fun createConversationAndSend(message: String, images: List<Uri>, model: AIModel) {
        viewModelScope.launch {
            if (_uiState.value.isCreatingConversation) {
                _uiState.update { it.copy(isSending = false, toast = "正在创建对话，请稍候") }
                return@launch
            }
            _uiState.update { it.copy(isCreatingConversation = true) }

            runCatching { apiService.createConversation(CreateConversationRequest("新对话")) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val conv = resp.body()
                        if (conv != null) {
                            val conversations = _uiState.value.conversations.toMutableList()
                            conversations.add(0, conv)
                            // NOT marked as empty — message is about to be sent immediately
                            _uiState.update {
                                it.copy(
                                    conversations = conversations,
                                    currentConversation = conv,
                                    messages = emptyList(),
                                    isCreatingConversation = false
                                )
                            }
                            sendToConversation(conv.id, message, images, model)
                        } else {
                            _uiState.update {
                                it.copy(isCreatingConversation = false, isSending = false, toast = "创建对话失败")
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(isCreatingConversation = false, isSending = false, toast = "创建对话失败")
                        }
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isCreatingConversation = false, isSending = false, toast = "创建对话失败，请稍后重试")
                    }
                }
        }
    }

    private fun sendToConversation(convId: Int, message: String, images: List<Uri>, model: AIModel) {
        // Remove from empty set — conversation now has content
        _uiState.update { it.copy(emptyConversationIds = it.emptyConversationIds - convId) }

        // Append user message
        val userMsg = ChatMessage(
            role = MessageRole.USER,
            content = message,
            images = images,
            timestamp = Date()
        )
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(userMsg)

        // Append AI placeholder(s)
        val isReasoning = model == AIModel.REASONER
        val reasoningStartTime = Date()
        var reasoningMsg: ChatMessage? = null
        var answerMsg: ChatMessage

        if (isReasoning) {
            reasoningMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "",
                isStreaming = true,
                isReasoning = true,
                reasoningStartTime = reasoningStartTime
            )
            currentMessages.add(reasoningMsg)
        }
        answerMsg = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true,
            isReasoning = false
        )
        currentMessages.add(answerMsg)

        _uiState.update { it.copy(messages = currentMessages) }

        // Reset streaming state
        reasoningBuffer = ""
        answerBuffer = ""
        pendingAnswerBuffer = ""
        isReasoningPhaseActive = isReasoning
        streamFinished = false

        // Start SSE streaming
        streamJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                startStreaming(convId, message, model.apiName, reasoningMsg?.id, answerMsg.id)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onStreamError(e.message ?: "连接失败")
                }
            }
        }
    }

    private suspend fun startStreaming(
        convId: Int, message: String, modelName: String,
        reasoningMsgId: UUID?, answerMsgId: UUID
    ) {
        val baseUrl = "https://zgjcyl.com/api/medical-records/conversations/$convId/send-message-stream/"
        val thinkingEnabled = _uiState.value.selectedModel.thinkingEnabled
        val body = gson.toJson(mapOf(
            "message" to message,
            "model" to modelName,
            "thinking_enabled" to thinkingEnabled
        ))
        val requestBody = body.toRequestBody(jsonMediaType)

        // Use a streaming client with no read timeout for SSE
        val streamingClient = okHttpClient.newBuilder()
            .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        // Authorization header is added by OkHttp interceptor — don't duplicate
        val request = Request.Builder()
            .url(baseUrl)
            .post(requestBody)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        val response: Response = streamingClient.newCall(request).execute()
        if (!response.isSuccessful) {
            withContext(Dispatchers.Main) { onStreamError("请求失败: ${response.code}") }
            return
        }

        val bodyStream = response.body?.byteStream()
        if (bodyStream == null) {
            withContext(Dispatchers.Main) { onStreamError("服务器无响应") }
            return
        }

        val reader = BufferedReader(InputStreamReader(bodyStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line ?: continue
            if (!l.startsWith("data: ")) continue
            val jsonStr = l.removePrefix("data: ").trim()
            if (jsonStr.isEmpty()) continue

            runCatching {
                val chunk = gson.fromJson(jsonStr, StreamChunk::class.java)
                withContext(Dispatchers.Main) { handleChunk(chunk, reasoningMsgId, answerMsgId) }
            }
        }

        // SSE stream ended — ensure we clean up streaming state
        if (!streamFinished) {
            withContext(Dispatchers.Main) {
                onStreamError("连接已断开")
            }
        }
    }

    private fun handleChunk(chunk: StreamChunk, reasoningMsgId: UUID?, answerMsgId: UUID) {
        when (chunk.type) {
            "reasoning" -> {
                reasoningBuffer += chunk.content ?: ""
                scheduleFlush(reasoningMsgId, answerMsgId)
            }
            "content" -> {
                if (isReasoningPhaseActive) {
                    // 推理内容已全部展示 → 立即过渡到回答阶段，不等 done 事件
                    // 避免答案内容被缓存在 pendingAnswerBuffer 而不渲染
                    if (reasoningBuffer.isEmpty()) {
                        isReasoningPhaseActive = false
                        answerBuffer = pendingAnswerBuffer + (chunk.content ?: "")
                        pendingAnswerBuffer = ""
                    } else {
                        pendingAnswerBuffer += chunk.content ?: ""
                    }
                } else {
                    answerBuffer += chunk.content ?: ""
                }
                scheduleFlush(reasoningMsgId, answerMsgId)
            }
            "hint" -> {
                val hintText = chunk.message ?: chunk.content ?: ""
                _uiState.update { it.copy(toast = hintText) }
            }
            "truncated" -> {
                val msgs = _uiState.value.messages.toMutableList()
                val idx = msgs.indexOfLast { it.id == answerMsgId }
                if (idx >= 0) msgs[idx] = msgs[idx].copy(isTruncated = true)
                _uiState.update { it.copy(messages = msgs) }
            }
            "done" -> {
                streamFinished = true
                // Mark reasoning end time
                if (reasoningMsgId != null) {
                    val msgs = _uiState.value.messages.toMutableList()
                    val ridx = msgs.indexOfFirst { it.id == reasoningMsgId }
                    if (ridx >= 0) {
                        msgs[ridx] = msgs[ridx].copy(
                            reasoningEndTime = Date(),
                            reasoningDuration =
                                ((Date().time - (msgs[ridx].reasoningStartTime?.time ?: Date().time)) / 1000.0)
                        )
                        _uiState.update { it.copy(messages = msgs) }
                    }
                }
                // Set backend IDs
                chunk.messageId?.let { backendId ->
                    val msgs = _uiState.value.messages.toMutableList()
                    val aidx = msgs.indexOfLast { it.id == answerMsgId }
                    if (aidx >= 0) msgs[aidx] = msgs[aidx].copy(backendId = backendId)
                    _uiState.update { it.copy(messages = msgs) }
                }
                scheduleFlush(reasoningMsgId, answerMsgId)

                // Generate title for first user message
                val conv = _uiState.value.currentConversation
                if (conv != null && (conv.messageCount == 0 || conv.title == "新对话")) {
                    val userText = _uiState.value.messages
                        .firstOrNull { it.role == MessageRole.USER }?.content ?: ""
                    if (userText.isNotEmpty()) {
                        viewModelScope.launch { generateTitle(conv.id, userText) }
                    }
                }

                // Update message count
                val newCount = (conv?.messageCount ?: 0) + 2
                val convs = _uiState.value.conversations.map {
                    if (it.id == conv?.id) it.copy(messageCount = newCount) else it
                }
                _uiState.update {
                    it.copy(
                        conversations = convs,
                        currentConversation = it.currentConversation?.copy(messageCount = newCount),
                        isSending = false
                    )
                }
            }
            "error" -> {
                onStreamError(chunk.message ?: "未知错误")
            }
        }
    }

    private fun scheduleFlush(reasoningMsgId: UUID?, answerMsgId: UUID) {
        flushJob?.cancel()
        flushJob = viewModelScope.launch {
            delay(30) // 30ms debounce
            flushBuffers(reasoningMsgId, answerMsgId)
        }
    }

    private fun flushBuffers(reasoningMsgId: UUID?, answerMsgId: UUID) {
        val stepSize = 15
        val msgs = _uiState.value.messages.toMutableList()

        // Flush reasoning buffer
        val ridx = if (reasoningMsgId != null) msgs.indexOfFirst { it.id == reasoningMsgId } else -1
        if (ridx >= 0 && reasoningBuffer.isNotEmpty()) {
            val (chunk, rest) = reasoningBuffer.splitSafeChunk(stepSize)
            reasoningBuffer = rest
            msgs[ridx] = msgs[ridx].copy(
                content = msgs[ridx].content + chunk,
                isStreaming = reasoningBuffer.isNotEmpty() || !streamFinished
            )
        } else if (ridx >= 0 && reasoningBuffer.isEmpty() && !streamFinished) {
            msgs[ridx] = msgs[ridx].copy(isStreaming = true)
        } else if (ridx >= 0 && reasoningBuffer.isEmpty() && streamFinished) {
            msgs[ridx] = msgs[ridx].copy(isStreaming = false)
            // Transfer pending answer buffer to answer buffer
            isReasoningPhaseActive = false
            if (pendingAnswerBuffer.isNotEmpty()) {
                answerBuffer += pendingAnswerBuffer
                pendingAnswerBuffer = ""
            }
        }

        // Flush answer buffer
        val aidx = msgs.indexOfLast { it.id == answerMsgId }
        if (aidx >= 0 && answerBuffer.isNotEmpty()) {
            val (chunk, rest) = answerBuffer.splitSafeChunk(stepSize)
            answerBuffer = rest
            msgs[aidx] = msgs[aidx].copy(
                content = msgs[aidx].content + chunk,
                isStreaming = answerBuffer.isNotEmpty() || !streamFinished || isReasoningPhaseActive
            )
        } else if (aidx >= 0 && answerBuffer.isEmpty() && streamFinished) {
            msgs[aidx] = msgs[aidx].copy(isStreaming = false)
        }

        _uiState.update { it.copy(messages = msgs) }

        // Continue flushing if there's more content（与 iOS 一致，包含 pendingAnswerBuffer）
        if (reasoningBuffer.isNotEmpty() || answerBuffer.isNotEmpty() || pendingAnswerBuffer.isNotEmpty()) {
            flushJob = viewModelScope.launch {
                delay(30)
                flushBuffers(reasoningMsgId, answerMsgId)
            }
        }
    }

    private fun onStreamError(error: String) {
        streamFinished = true
        isReasoningPhaseActive = false
        // Merge any pending answer content into answer buffer before cleanup
        if (pendingAnswerBuffer.isNotEmpty()) {
            answerBuffer += pendingAnswerBuffer
            pendingAnswerBuffer = ""
        }
        val msgs = _uiState.value.messages.toMutableList()
        // Remove all empty streaming messages (there may be 2 in reasoning mode)
        msgs.removeAll { it.role == MessageRole.ASSISTANT && it.isStreaming && it.content.isEmpty() }
        // Mark any remaining streaming messages (with partial content) as done
        for (i in msgs.indices) {
            if (msgs[i].role == MessageRole.ASSISTANT && msgs[i].isStreaming) {
                msgs[i] = msgs[i].copy(isStreaming = false)
            }
        }
        _uiState.update { it.copy(messages = msgs, isSending = false, toast = error) }
    }

    /**
     * 安全拆分字符串，确保不会在 surrogate pair 中间截断（emoji 等双码元字符）。
     * 返回 (head, tail)，head 长度不超过 chunkSize 个码元。
     */
    private fun String.splitSafeChunk(chunkSize: Int): Pair<String, String> {
        if (length <= chunkSize) return Pair(this, "")
        var point = chunkSize
        // 不在高/低代理对之间截断
        if (point < length && Character.isLowSurrogate(this[point])) {
            point--
        }
        if (point == 0) point = minOf(chunkSize + 1, length) // 保证至少推进一个字符
        return Pair(substring(0, point), substring(point))
    }

    fun stopStreaming() {
        streamJob?.cancel()
        flushJob?.cancel()
        streamFinished = true
        isReasoningPhaseActive = false
        val msgs = _uiState.value.messages.toMutableList()
        msgs.removeAll { it.isStreaming && it.content.isEmpty() }
        for (i in msgs.indices) {
            if (msgs[i].isStreaming) {
                msgs[i] = msgs[i].copy(isStreaming = false)
            }
        }
        _uiState.update { it.copy(messages = msgs, isSending = false) }
    }

    // =========================================================================
    // OCR Image Recognition
    // =========================================================================

    fun handleImageSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isUploadingImage = true, toast = "正在识别图片...") }

            runCatching {
                val imageBytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("无法读取图片")
                val requestBody = MultipartBody.Part.createFormData(
                    "image", "image.jpg",
                    imageBytes.toRequestBody("image/*".toMediaType())
                )
                apiService.ocrRecognize(requestBody)
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val ocrText = body?.ocr_text ?: ""
                    if (ocrText.isNotBlank()) {
                        val result = "📋 图片识别内容：\n\n$ocrText"
                        val ocrList = _uiState.value.ocrResults.toMutableList()
                        ocrList.add(result)
                        _uiState.update {
                            it.copy(ocrResults = ocrList, isUploadingImage = false,
                                toast = "✓ 图片识别成功，可以继续提问")
                        }
                    } else {
                        _uiState.update { it.copy(isUploadingImage = false, toast = "未识别到文字") }
                    }
                } else {
                    _uiState.update { it.copy(isUploadingImage = false, toast = "图片识别失败，请重试") }
                }
            }.onFailure {
                _uiState.update { it.copy(isUploadingImage = false, toast = "图片识别失败，请重试") }
            }
        }
    }

    fun removeImage(index: Int) {
        val images = _uiState.value.selectedImages.toMutableList()
        val ocr = _uiState.value.ocrResults.toMutableList()
        if (index < images.size) images.removeAt(index)
        if (index < ocr.size) ocr.removeAt(index)
        if (images.isEmpty()) {
            _uiState.update { it.copy(selectedImages = images, ocrResults = ocr, isUploadingImage = false) }
        } else {
            _uiState.update { it.copy(selectedImages = images, ocrResults = ocr) }
        }
    }

    // =========================================================================
    // Feedback & Favorites
    // =========================================================================

    fun setFeedback(message: ChatMessage, feedback: Int) {
        val convId = _uiState.value.currentConversation?.id ?: return
        val msgId = message.backendId ?: return
        viewModelScope.launch {
            // Optimistic update — messages list
            val msgs = _uiState.value.messages.toMutableList()
            val idx = msgs.indexOfFirst { it.id == message.id }
            if (idx >= 0) msgs[idx] = msgs[idx].copy(feedback = feedback)
            // Also update actionSheetMessage so the sheet reflects new state
            val updatedSheet = _uiState.value.actionSheetMessage?.let {
                if (it.id == message.id) it.copy(feedback = feedback) else it
            }
            _uiState.update { it.copy(messages = msgs, actionSheetMessage = updatedSheet ?: it.actionSheetMessage) }

            runCatching {
                apiService.setConversationFeedback(convId, FeedbackRequest(msgId, feedback))
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val newFeedback = resp.body()?.feedback ?: feedback
                    val msgs2 = _uiState.value.messages.toMutableList()
                    val idx2 = msgs2.indexOfFirst { it.id == message.id }
                    if (idx2 >= 0) msgs2[idx2] = msgs2[idx2].copy(feedback = newFeedback)
                    val updatedSheet2 = _uiState.value.actionSheetMessage?.let {
                        if (it.id == message.id) it.copy(feedback = newFeedback) else it
                    }
                    _uiState.update { it.copy(messages = msgs2, actionSheetMessage = updatedSheet2 ?: it.actionSheetMessage) }
                }
            }.onFailure {
                // Rollback
                val msgs2 = _uiState.value.messages.toMutableList()
                val idx2 = msgs2.indexOfFirst { it.id == message.id }
                if (idx2 >= 0) msgs2[idx2] = msgs2[idx2].copy(feedback = 0)
                val rolledBackSheet = _uiState.value.actionSheetMessage?.let {
                    if (it.id == message.id) it.copy(feedback = 0) else it
                }
                _uiState.update { it.copy(messages = msgs2, actionSheetMessage = rolledBackSheet ?: it.actionSheetMessage) }
            }
        }
    }

    fun toggleFavorite(message: ChatMessage) {
        val convId = _uiState.value.currentConversation?.id ?: return
        val backendId = message.backendId ?: return
        val isFav = _uiState.value.favoriteMessageIds.contains(backendId)

        viewModelScope.launch {
            // Optimistic update
            val newFavs = if (isFav) {
                _uiState.value.favoriteMessageIds - backendId
            } else {
                _uiState.value.favoriteMessageIds + backendId
            }
            _uiState.update { it.copy(favoriteMessageIds = newFavs) }

            val result = if (isFav) {
                runCatching { apiService.unfavoriteMessage(convId, backendId) }
            } else {
                runCatching { apiService.favoriteMessage(convId, FavoriteMessageRequest(backendId)) }
            }

            result.onSuccess {
                _uiState.update { it.copy(toast = if (isFav) "已取消收藏" else "已收藏") }
            }.onFailure {
                // Rollback
                _uiState.update {
                    it.copy(
                        favoriteMessageIds = if (isFav) it.favoriteMessageIds + backendId
                        else it.favoriteMessageIds - backendId,
                        toast = "操作失败"
                    )
                }
            }
        }
    }

    private fun loadFavoriteMessages(conversationId: Int) {
        viewModelScope.launch {
            runCatching { apiService.getFavoriteMessages(conversationId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        val ids = resp.body()?.ids ?: emptyList()
                        _uiState.update { it.copy(favoriteMessageIds = ids.toSet()) }
                    }
                }
        }
    }

    // =========================================================================
    // Delete Message (paired deletion)
    // =========================================================================

    fun deleteMessage(message: ChatMessage) {
        // Streaming guard — same as iOS
        if (message.isStreaming) {
            _uiState.update { it.copy(toast = "正在生成中，稍后再删除") }
            return
        }

        val convId = _uiState.value.currentConversation?.id ?: return
        val messages = _uiState.value.messages

        // Find paired messages for deletion (user + assistant round)
        val targetIdx = messages.indexOfFirst { it.id == message.id }
        if (targetIdx < 0) return

        val indicesToRemove = mutableListOf<Int>()
        if (messages[targetIdx].role == MessageRole.USER) {
            indicesToRemove.add(targetIdx)
            // Delete this user + all following assistants until next user
            for (i in (targetIdx + 1) until messages.size) {
                if (messages[i].role == MessageRole.ASSISTANT) indicesToRemove.add(i)
                else break
            }
        } else {
            // Find preceding user
            for (i in (targetIdx - 1) downTo 0) {
                if (messages[i].role == MessageRole.USER) {
                    indicesToRemove.add(i)
                    break
                }
            }
            // All assistants in this round from user onward
            val userIdx = indicesToRemove.firstOrNull() ?: targetIdx
            for (i in (userIdx + 1) until messages.size) {
                if (messages[i].role == MessageRole.ASSISTANT) indicesToRemove.add(i)
                else if (messages[i].role == MessageRole.USER) break
            }
        }

        val sortedIndices = indicesToRemove.sortedDescending()
        val msgsToDelete = sortedIndices.map { messages[it] }

        // Optimistic removal
        val newMsgs = messages.toMutableList()
        sortedIndices.forEach { newMsgs.removeAt(it) }
        _uiState.update { it.copy(messages = newMsgs) }

        // Delete on server
        viewModelScope.launch {
            var anyDeleted = false
            msgsToDelete.forEach { msg ->
                msg.backendId?.let { bid ->
                    runCatching { apiService.deleteConversationMessage(convId, bid) }
                    anyDeleted = true
                }
            }
            if (anyDeleted) {
                _uiState.update { it.copy(toast = "已删除") }
            }
        }
    }

    // =========================================================================
    // Generate Title
    // =========================================================================

    private suspend fun generateTitle(convId: Int, userMessage: String) {
        runCatching {
            apiService.generateConversationTitle(convId, GenerateTitleRequest(userMessage))
        }.onSuccess { resp ->
            if (resp.isSuccessful) {
                val newTitle = resp.body()?.title ?: return@onSuccess
                val convs = _uiState.value.conversations.map {
                    if (it.id == convId) it.copy(title = newTitle) else it
                }
                val cur = _uiState.value.currentConversation
                val newCur = if (cur?.id == convId) cur.copy(title = newTitle) else cur
                _uiState.update { it.copy(conversations = convs, currentConversation = newCur) }
            }
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setSelectedModel(model: AIModel) {
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun setShowSidebar(show: Boolean) {
        _uiState.update { it.copy(showSidebar = show) }
        // When sidebar closes, cleanup empty conversations (matching iOS)
        if (!show) {
            cleanupEmptyConversations()
        }
    }

    /**
     * Delete empty conversations that were created but never had
     * a user message sent to them (matching iOS behavior).
     * Skips the currently selected conversation.
     */
    fun cleanupEmptyConversations() {
        val emptyIds = _uiState.value.emptyConversationIds
        if (emptyIds.isEmpty()) return
        val currentId = _uiState.value.currentConversation?.id
        val conversations = _uiState.value.conversations

        emptyIds.forEach { emptyId ->
            // Skip current conversation (user may be viewing it)
            if (emptyId == currentId) return@forEach
            // Only clean up if the conversation still exists locally
            if (conversations.any { it.id == emptyId }) {
                // Fire-and-forget: delete from server, remove from local state
                viewModelScope.launch {
                    runCatching { apiService.deleteConversation(emptyId) }
                        .onSuccess {
                            val convs = _uiState.value.conversations.filter { it.id != emptyId }
                            val newEmptyIds = _uiState.value.emptyConversationIds - emptyId
                            _uiState.update {
                                it.copy(
                                    conversations = convs,
                                    emptyConversationIds = newEmptyIds
                                )
                            }
                        }
                }
            } else {
                // Conversation not in list anymore, just remove ID
                _uiState.update { it.copy(emptyConversationIds = it.emptyConversationIds - emptyId) }
            }
        }
    }

    fun setShowActionSheet(show: Boolean, message: ChatMessage? = null) {
        _uiState.update { it.copy(showActionSheet = show, actionSheetMessage = message) }
    }

    fun setShowDataNotice(show: Boolean) {
        _uiState.update { it.copy(showDataNotice = show) }
    }

    fun addSelectedImage(uri: Uri) {
        val images = _uiState.value.selectedImages.toMutableList()
        images.add(uri)
        _uiState.update { it.copy(selectedImages = images) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }

    private fun parseIsoDate(iso: String?): Date {
        if (iso == null) return Date()
        return runCatching {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.parse(iso) ?: Date()
        }.getOrDefault(Date())
    }
}
