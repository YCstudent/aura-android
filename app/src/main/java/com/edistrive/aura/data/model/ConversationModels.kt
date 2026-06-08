package com.edistrive.aura.data.model

import android.net.Uri
import com.google.gson.annotations.SerializedName
import java.util.Date
import java.util.UUID

// ============================================================================
// API Models — Conversation
// ============================================================================

data class Conversation(
    val id: Int = 0,
    var title: String = "新对话",
    @SerializedName("family_member") val familyMember: Int? = null,
    @SerializedName("family_member_name") val familyMemberName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") var updatedAt: String? = null,
    val messages: List<ConversationMessage>? = null,
    @SerializedName("message_count") var messageCount: Int = 0
)

data class ConversationMessage(
    val id: Int = 0,
    val role: String = "",              // "user" | "assistant"
    @SerializedName("message_type") val messageType: String? = null, // "content" | "reasoning"
    val content: String = "",
    @SerializedName("created_at") val createdAt: String? = null,
    val feedback: Int? = null,          // -1 dislike, 0 none, 1 like
    @SerializedName("reasoning_duration") val reasoningDuration: Int? = null
)

// Paginated response — DRF returns {count, next, previous, results}
data class ConversationsListResponse(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<Conversation>? = null
)

// ============================================================================
// API Models — Requests
// ============================================================================

data class CreateConversationRequest(
    val title: String = "新对话"
)

data class UpdateConversationTitleRequest(
    val title: String
)

data class SendMessageRequest(
    val message: String,
    val model: String = "deepseek-v4-pro",
    @SerializedName("thinking_enabled") val thinkingEnabled: Boolean = false
)

data class FeedbackRequest(
    @SerializedName("message_id") val messageId: Int,
    val feedback: Int                 // -1, 0, 1
)

data class FavoriteMessageRequest(
    @SerializedName("message_id") val messageId: Int
)

data class GenerateTitleRequest(
    @SerializedName("user_message") val userMessage: String
)

// ============================================================================
// API Models — Responses
// ============================================================================

data class FeedbackResponse(
    val success: Boolean = false,
    val message: String? = null,
    val feedback: Int? = null
)

data class FavoriteResponse(
    val success: Boolean = false,
    val created: Boolean? = null
)

data class FavoriteListResponse(
    val success: Boolean = false,
    val ids: List<Int>? = null
)

data class GenerateTitleResponse(
    val success: Boolean = false,
    val title: String? = null
)

data class DeleteMessageResponse(
    val success: Boolean = false
)

// ============================================================================
// SSE Stream chunk
// ============================================================================

data class StreamChunk(
    val type: String = "",      // "content" | "reasoning" | "done" | "error" | "hint" | "truncated" | "user_message"
    val content: String? = null,
    @SerializedName("message_id") val messageId: Int? = null,
    val timestamp: String? = null,
    val message: String? = null // error message
)

// ============================================================================
// UI Models
// ============================================================================

enum class MessageRole {
    USER, ASSISTANT
}

enum class AIModel(val apiName: String, val displayName: String, val thinkingEnabled: Boolean = false) {
    CHAT("deepseek-v4-pro", "Chat", thinkingEnabled = false),
    REASONER("deepseek-v4-pro", "深度思考", thinkingEnabled = true)
}

data class ChatMessage(
    val id: UUID = UUID.randomUUID(),
    val role: MessageRole,
    var content: String,
    val timestamp: Date = Date(),
    var isStreaming: Boolean = false,
    var isReasoning: Boolean = false,
    var backendId: Int? = null,
    var feedback: Int = 0,
    var reasoningStartTime: Date? = null,
    var reasoningEndTime: Date? = null,
    var reasoningDuration: Double? = null,
    var isTruncated: Boolean = false,
    var images: List<Uri> = emptyList()
)
