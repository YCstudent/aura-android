package com.edistrive.aura.ui.screens.digital

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edistrive.aura.data.model.ChatMessage
import com.edistrive.aura.data.model.MessageRole
import com.edistrive.aura.ui.theme.AuraTokens

@Composable
fun ActionSheetOverlay(
    show: Boolean,
    message: ChatMessage?,
    onDismiss: () -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onSelectText: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    onFavorite: (ChatMessage) -> Unit,
    onShare: (ChatMessage) -> Unit,
    onFeedback: (ChatMessage, Int) -> Unit
) {
    // Dimmer backdrop with spring animation
    AnimatedVisibility(
        visible = show && message != null,
        enter = fadeIn(animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f)),
        exit = fadeOut(animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            message?.let { msg ->
                val cardShape = RoundedCornerShape(22.dp)

                Column(
                    modifier = Modifier
                        .width(292.dp)
                        // Shadow BEFORE clip so it draws around the rounded shape
                        .shadow(
                            elevation = 18.dp,
                            shape = cardShape,
                            ambientColor = Color.Black.copy(alpha = 0.14f),
                            spotColor = Color.Black.copy(alpha = 0.06f)
                        )
                        .clip(cardShape)
                        .background(Color.White.copy(alpha = 0.96f))
                        // Subtle hairline border like iOS
                        .border(0.5.dp, Color.Black.copy(alpha = 0.06f), cardShape),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Feedback section — two equal buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        // Like
                        FeedbackButton(
                            icon = if (msg.feedback == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            label = "点赞",
                            isActive = msg.feedback == 1,
                            onClick = { onFeedback(msg, if (msg.feedback == 1) 0 else 1) },
                            modifier = Modifier.weight(1f)
                        )
                        // Vertical separator
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(Color(0xFFEDEDED))
                        )
                        // Dislike
                        FeedbackButton(
                            icon = if (msg.feedback == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                            label = "点踩",
                            isActive = msg.feedback == -1,
                            onClick = { onFeedback(msg, if (msg.feedback == -1) 0 else -1) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Full-width separator
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEDEDED)))

                    // Action list
                    ActionSheetRow(
                        icon = Icons.Default.ContentCopy,
                        title = "复制",
                        onClick = { onCopy(msg) }
                    )
                    ActionSheetDivider()
                    ActionSheetRow(
                        icon = Icons.Default.TextFields,
                        title = "选取文字",
                        onClick = { onSelectText(msg) }
                    )
                    ActionSheetDivider()
                    ActionSheetRow(
                        icon = Icons.Default.Delete,
                        title = "删除",
                        color = Color(0xFFF56C6C),
                        onClick = { onDelete(msg) }
                    )
                    ActionSheetDivider()
                    val isFav = msg.feedback == 2
                    ActionSheetRow(
                        icon = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        title = if (isFav) "已收藏" else "收藏",
                        color = if (isFav) AuraTokens.Primary else Color(0xFF333333),
                        onClick = { onFavorite(msg) }
                    )
                    ActionSheetDivider()
                    ActionSheetRow(
                        icon = Icons.Outlined.Share,
                        title = "分享",
                        onClick = { onShare(msg) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isActive) AuraTokens.Primary else Color(0xFF333333),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) AuraTokens.Primary else Color(0xFF333333)
        )
    }
}

@Composable
private fun ActionSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color = Color(0xFF333333),
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = color)
    }
}

@Composable
private fun ActionSheetDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(0.5.dp)
            .background(Color(0xFFEDEDED))
    )
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}
