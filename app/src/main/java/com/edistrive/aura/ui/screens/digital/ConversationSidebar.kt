package com.edistrive.aura.ui.screens.digital

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.edistrive.aura.data.model.Conversation
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.ui.components.StyledConfirmDialog
import com.edistrive.aura.ui.components.StyledInputDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationSidebarOverlay(
    show: Boolean,
    conversations: List<Conversation>,
    currentConversation: Conversation?,
    favoriteMessageIds: Set<Int>,
    isCreatingConversation: Boolean,
    currentUsername: String = "",
    currentAvatarUrl: String? = null,
    onDismiss: () -> Unit,
    onSelect: (Conversation) -> Unit,
    onDelete: (Int) -> Unit,
    onRename: (Int, String) -> Unit,
    onCreate: () -> Unit,
    onShowFavorites: () -> Unit
) {
    // Full-screen overlay covering system bars
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = show,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f)),
            exit = fadeOut(animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f))
        ) {
            // Dimmer — fills entire screen including system bars
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = show,
            enter = slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                initialOffsetX = { -it }
            ),
            exit = slideOutHorizontally(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                targetOffsetX = { -it }
            )
        ) {
            ConversationListView(
                conversations = conversations,
                currentConversation = currentConversation,
                favoriteMessageIds = favoriteMessageIds,
                isCreatingConversation = isCreatingConversation,
                currentUsername = currentUsername,
                currentAvatarUrl = currentAvatarUrl,
                onSelect = onSelect,
                onDelete = onDelete,
                onRename = onRename,
                onCreate = onCreate,
                onShowFavorites = onShowFavorites
            )
        }
    }
}

@Composable
private fun ConversationListView(
    conversations: List<Conversation>,
    currentConversation: Conversation?,
    favoriteMessageIds: Set<Int>,
    isCreatingConversation: Boolean,
    currentUsername: String = "",
    currentAvatarUrl: String? = null,
    onSelect: (Conversation) -> Unit,
    onDelete: (Int) -> Unit,
    onRename: (Int, String) -> Unit,
    onCreate: () -> Unit,
    onShowFavorites: () -> Unit
) {
    var renameDialogId by remember { mutableStateOf<Int?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteConfirmId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .shadow(16.dp, ambientColor = Color.Black.copy(alpha = 0.1f))
            .background(Color(0xFFF8F9FA))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AuraTokens.Primary, AuraTokens.Primary2),
                            Offset.Zero, Offset.Infinite
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "医小智",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = AuraTokens.Primary
            )
        }

        // New conversation button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable(enabled = !isCreatingConversation, onClick = onCreate)
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Icon(
                Icons.Default.ChatBubble,
                contentDescription = null,
                tint = AuraTokens.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "新建对话",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Favorites entry
        if (favoriteMessageIds.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(onClick = onShowFavorites)
                    .padding(14.dp)
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = AuraTokens.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "我的收藏",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFBBBBBB),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section title
        Text(
            "对话历史",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF888888),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Conversation list
        val grouped = groupConversationsByDate(conversations)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f)
        ) {
            grouped.forEach { (label, convs) ->
                item {
                    Text(
                        label,
                        fontSize = 12.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
                items(convs, key = { it.id }) { conv ->
                    val isActive = conv.id == currentConversation?.id
                    ConversationCard(
                        conversation = conv,
                        isActive = isActive,
                        onClick = { onSelect(conv) },
                        onRenameClick = {
                            renameText = conv.title
                            renameDialogId = conv.id
                        },
                        onDeleteClick = { deleteConfirmId = conv.id }
                    )
                }
            }
        }

        // Bottom user row
        val avatarUrl = currentAvatarUrl?.let {
            if (it.startsWith("http")) it else "https://zgjcyl.com$it"
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    val ctx = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(avatarUrl)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                currentUsername.ifBlank { "用户" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
        }
    }

    // Rename dialog
    renameDialogId?.let { id ->
        StyledInputDialog(
            icon = Icons.Filled.Edit,
            title = "重命名对话",
            value = renameText,
            onValueChange = { renameText = it },
            placeholder = "请输入新名称",
            onConfirm = { onRename(id, renameText); renameDialogId = null },
            onDismiss = { renameDialogId = null }
        )
    }

    // Delete confirmation
    deleteConfirmId?.let { id ->
        StyledConfirmDialog(
            icon = Icons.Filled.Delete,
            title = "删除对话",
            message = "确定要删除这个对话吗？删除后无法恢复。",
            confirmLabel = "删除",
            onConfirm = { onDelete(id); deleteConfirmId = null },
            onDismiss = { deleteConfirmId = null }
        )
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    isActive: Boolean,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) AuraTokens.Primary.copy(alpha = 0.08f) else Color.White)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .then(
                    if (isActive) Modifier.background(
                        Brush.linearGradient(
                            listOf(AuraTokens.Primary, AuraTokens.Primary2),
                            Offset.Zero, Offset.Infinite
                        )
                    ) else Modifier.background(Color(0xFFF0F0F0))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ChatBubble,
                contentDescription = null,
                tint = if (isActive) Color.White else Color(0xFF999999),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    conversation.title,
                    fontSize = 15.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatSidebarTime(conversation.updatedAt ?: conversation.createdAt),
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "${conversation.messageCount} 条消息",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("重命名") },
                onClick = {
                    showMenu = false
                    onRenameClick()
                }
            )
            DropdownMenuItem(
                text = { Text("删除对话", color = Color(0xFFF56C6C)) },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                }
            )
        }
    }
}

private fun groupConversationsByDate(conversations: List<Conversation>): LinkedHashMap<String, List<Conversation>> {
    val result = LinkedHashMap<String, MutableList<Conversation>>()
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val weekStart = Calendar.getInstance().apply {
        val dow = get(Calendar.DAY_OF_WEEK)
        add(Calendar.DAY_OF_YEAR, -(dow - Calendar.MONDAY))
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }

    for (conv in conversations) {
        val dateStr = conv.updatedAt ?: conv.createdAt ?: continue
        val date = runCatching { sdf.parse(dateStr.substringBefore(".")) }.getOrNull() ?: continue
        val cal = Calendar.getInstance().apply { time = date }

        val label = when {
            cal >= today -> "今天"
            cal >= yesterday -> "昨天"
            cal >= weekStart -> "本周"
            cal >= monthStart -> "本月"
            else -> {
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                "${y}年${m}月"
            }
        }
        result.getOrPut(label) { mutableListOf() }.add(conv)
    }
    return LinkedHashMap(result)
}

private fun formatSidebarTime(iso: String?): String {
    if (iso == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val date = runCatching { sdf.parse(iso.substringBefore(".")) }.getOrNull() ?: return ""
    val now = System.currentTimeMillis()
    val diff = now - date.time
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        diff < 604_800_000 -> "${diff / 86_400_000}天前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
    }
}
