package com.edistrive.aura.ui.screens.digital

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.edistrive.aura.data.model.ChatMessage
import com.edistrive.aura.data.model.MessageRole
import com.edistrive.aura.ui.theme.AuraTokens

// iOS matching colors
private val UserBubbleStart = Color(0xFF2BB0A8)
private val UserBubbleEnd = Color(0xFF1A8080)
private val AssistantBg = Color.White.copy(alpha = 0.92f)
private val AssistantBorder = Color.White.copy(alpha = 0.6f)
private val ReasoningBg = Color(0xFFF5F7FA)
private val ReasoningText = Color(0xFF999999)
private val ReasoningContent = Color(0xFF666666)
private val TextDark = Color(0xFF1A1A1A)

@Composable
private fun BlinkingCursor(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(UserBubbleStart.copy(alpha = cursorAlpha))
    )
}

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotAlphas = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, delayMillis = index * 150),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotAlpha$index"
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        dotAlphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF999999).copy(alpha = alpha.value))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    onLongPress: (ChatMessage) -> Unit,
    onRegenerate: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    when (message.role) {
        MessageRole.USER -> UserBubble(message, onLongPress, modifier)
        MessageRole.ASSISTANT -> AssistantBubble(message, onLongPress, onRegenerate, modifier)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserBubble(
    message: ChatMessage,
    onLongPress: (ChatMessage) -> Unit,
    modifier: Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f), label = "pressScale"
    )

    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .fillMaxWidth(0.64f)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                    onLongClick = { onLongPress(message) }
                )
        ) {
            if (message.images.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(message.images.size) { index ->
                        AsyncImage(
                            model = message.images[index],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = AuraTokens.Primary.copy(alpha = 0.32f), spotColor = AuraTokens.Primary.copy(alpha = 0.32f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(UserBubbleStart, UserBubbleEnd),
                            start = Offset.Zero, end = Offset(0f, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssistantBubble(
    message: ChatMessage,
    onLongPress: (ChatMessage) -> Unit,
    onRegenerate: (ChatMessage) -> Unit,
    modifier: Modifier
) {
    var isReasoningExpanded by remember { mutableStateOf(message.isStreaming) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f), label = "pressScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClick = { onLongPress(message) }
            )
    ) {
        // Reasoning section
        if (message.isReasoning || message.reasoningDuration != null) {
            val elapsedSeconds = if (message.reasoningEndTime != null) {
                message.reasoningDuration?.toLong() ?: 0
            } else {
                ((System.currentTimeMillis() - (message.reasoningStartTime?.time ?: System.currentTimeMillis())) / 1000)
            }

            Column(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReasoningBg)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isReasoningExpanded = !isReasoningExpanded }
                ) {
                    Icon(
                        if (isReasoningExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ReasoningText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (message.isStreaming) "思考中" else "已思考",
                        fontSize = 14.sp,
                        color = ReasoningText
                    )
                    if (!message.isStreaming && elapsedSeconds > 0) {
                        Text(
                            " (用时 ${elapsedSeconds}s)",
                            fontSize = 14.sp,
                            color = ReasoningText
                        )
                    }
                }

                AnimatedVisibility(visible = isReasoningExpanded) {
                    Text(
                        text = message.content.ifBlank { "..." },
                        fontSize = 14.sp,
                        color = ReasoningContent,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Answer bubble (non-reasoning messages or reasoning content shown expanded above)
        if (!message.isReasoning) {
            Box(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.06f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(AssistantBg)
                    .border(1.dp, AssistantBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (message.isStreaming && message.content.isBlank()) {
                    TypingIndicator(modifier = Modifier.padding(vertical = 4.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MarkdownText(
                            content = message.content,
                            isStreaming = message.isStreaming,
                            fontSize = 16,
                            modifier = Modifier.weight(1f)
                        )
                        if (message.isStreaming && message.content.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            BlinkingCursor()
                        }
                    }
                }

                // Continue button
                if (message.isTruncated && !message.isStreaming) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.linearGradient(
                                    listOf(AuraTokens.Primary, AuraTokens.Primary2),
                                    start = Offset.Zero, end = Offset.Infinite
                                )
                            )
                            .shadow(8.dp, RoundedCornerShape(50), ambientColor = AuraTokens.Primary.copy(alpha = 0.3f))
                            .clickable { onRegenerate(message) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("继续生成", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
