package com.edistrive.aura.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.edistrive.aura.data.model.NotificationItem
import com.edistrive.aura.ui.navigation.Routes
import com.edistrive.aura.ui.state.InboxViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.ui.components.StyledConfirmDialog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

enum class MessageType(val displayName: String, val icon: ImageVector, val color: Color) {
    SYSTEM("系统消息", Icons.Filled.Notifications, Color(0xFF1A8080)),
    MEDICATION("用药提醒", Icons.Filled.Medication, Color(0xFFFF9500)),
    APPOINTMENT("预约提醒", Icons.Filled.CalendarMonth, Color(0xFF007AFF)),
    FAMILY("家人消息", Icons.Filled.People, Color(0xFF34C759)),
    HEALTH("健康提示", Icons.Filled.Favorite, Color(0xFFFF3B30));

    companion object {
        fun fromString(type: String?): MessageType = when (type) {
            "medication" -> MEDICATION
            "appointment" -> APPOINTMENT
            "family" -> FAMILY
            "health" -> HEALTH
            else -> SYSTEM
        }
    }
}

private fun formatDetailTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "-"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoString.substringBefore(".").substringBefore("Z"))
        val out = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
        out.format(date!!)
    } catch (_: Exception) {
        isoString
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf<MessageType?>(null) }
    var searchText by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedMessages by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var detailMessage by remember { mutableStateOf<NotificationItem?>(null) }

    LaunchedEffect(Unit) { viewModel.loadMessages() }

    val messages = uiState.messages

    val filteredMessages = remember(messages, selectedFilter, searchText) {
        var result = messages
        selectedFilter?.let { filter ->
            result = result.filter { MessageType.fromString(it.message_type) == filter }
        }
        if (searchText.isNotEmpty()) {
            result = result.filter { msg ->
                (msg.title ?: "").contains(searchText, ignoreCase = true) ||
                (msg.content ?: "").contains(searchText, ignoreCase = true)
            }
        }
        // Sort: unread first, then by id descending (newest first)
        result.sortedWith(compareBy({ it.is_read == true }, { -(it.id ?: 0) }))
    }

    // Toast auto-dismiss
    LaunchedEffect(uiState.toast) {
        if (uiState.toast != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.consumeToast()
        }
    }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "收件箱",
                onBack = onBack,
                background = Color(0xFFF5F7FA),
                trailing = {
                    if (!isEditMode) {
                        TextButton(
                            onClick = { viewModel.markAllAsRead() },
                            enabled = uiState.unreadCount > 0
                        ) {
                            Text("全部已读", color = AuraTokens.Primary)
                        }
                    }
                },
                leading = {
                    if (!isEditMode) {
                        TextButton(
                            onClick = { isEditMode = true },
                            enabled = messages.isNotEmpty()
                        ) {
                            Text("编辑", color = AuraTokens.Primary)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                isEditMode = false
                                selectedMessages = emptySet()
                            }
                        ) {
                            Text("取消", color = AuraTokens.Primary)
                        }
                    }
                }
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AuraTokens.Primary)
                    }
                }
                uiState.errorMessage != null && messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(uiState.errorMessage ?: "", color = Color(0xFFE5484D), fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.loadMessages() }) {
                            Text("重试", color = AuraTokens.Primary)
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchBar(
                            searchText = searchText,
                            onSearchTextChange = { searchText = it }
                        )

                        FilterChipRow(
                            messages = messages,
                            selectedFilter = selectedFilter,
                            onFilterChange = { selectedFilter = it }
                        )

                        if (isEditMode) {
                            EditToolbar(
                                selectedCount = selectedMessages.size,
                                filteredCount = filteredMessages.size,
                                onSelectAll = {
                                    selectedMessages = if (selectedMessages.size == filteredMessages.size) {
                                        emptySet()
                                    } else {
                                        filteredMessages.mapNotNull { it.id }.toSet()
                                    }
                                },
                                onDelete = {
                                    if (selectedMessages.isNotEmpty()) showDeleteDialog = true
                                }
                            )
                        }

                        val pullRefreshState = rememberPullRefreshState(
                            refreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refreshMessages() }
                        )

                        if (filteredMessages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .pullRefresh(pullRefreshState)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                                        EmptyView(selectedFilter = selectedFilter)
                                    }
                                }
                                PullRefreshIndicator(
                                    refreshing = uiState.isRefreshing,
                                    state = pullRefreshState,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    backgroundColor = Color.White,
                                    contentColor = AuraTokens.Primary
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .pullRefresh(pullRefreshState)
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    MessageList(
                                        messages = filteredMessages,
                                        isEditMode = isEditMode,
                                        selectedMessages = selectedMessages,
                                        onToggleSelect = { id ->
                                            selectedMessages = if (selectedMessages.contains(id)) {
                                                selectedMessages - id
                                            } else {
                                                selectedMessages + id
                                            }
                                        },
                                        onMessageClick = { msg ->
                                            if (isEditMode) {
                                                val id = msg.id ?: return@MessageList
                                                selectedMessages = if (selectedMessages.contains(id)) {
                                                    selectedMessages - id
                                                } else {
                                                    selectedMessages + id
                                                }
                                            } else {
                                                msg.id?.let { viewModel.markAsRead(it) }
                                                detailMessage = msg
                                            }
                                        }
                                    )
                                }
                                PullRefreshIndicator(
                                    refreshing = uiState.isRefreshing,
                                    state = pullRefreshState,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    backgroundColor = Color.White,
                                    contentColor = AuraTokens.Primary
                                )
                            }
                        }
                    }

                    // Toast overlay
                    AnimatedVisibility(
                        visible = uiState.toast != null,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.toastIsError) Color(0xFFE5484D) else Color(0xFF2BB0A8))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (uiState.toastIsError) Icons.Default.Cancel else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(uiState.toast.orEmpty(), color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        StyledConfirmDialog(
            icon = Icons.Filled.Delete,
            title = "删除消息",
            message = "确定要删除选中的 ${selectedMessages.size} 条消息吗？",
            confirmLabel = "删除",
            onConfirm = {
                viewModel.deleteMessages(selectedMessages.toList())
                selectedMessages = emptySet()
                showDeleteDialog = false
                isEditMode = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Message detail dialog
    detailMessage?.let { msg ->
        MessageDetailDialog(
            message = msg,
            navController = navController,
            onDismiss = { detailMessage = null },
            onDelete = {
                msg.id?.let { viewModel.deleteMessages(listOf(it)) }
                detailMessage = null
            }
        )
    }
}

// ---------- Search bar ----------

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF5F7FA)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    placeholder = { Text("搜索消息", fontSize = 15.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (searchText.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchTextChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF999999), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ---------- Filter chip row ----------

@Composable
private fun FilterChipRow(
    messages: List<NotificationItem>,
    selectedFilter: MessageType?,
    onFilterChange: (MessageType?) -> Unit
) {
    Surface(color = Color.White) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            item {
                FilterChip(
                    label = "全部",
                    count = messages.size,
                    color = Color(0xFF1A8080),
                    isSelected = selectedFilter == null,
                    onClick = { onFilterChange(null) }
                )
            }

            items(MessageType.values().toList()) { type ->
                val count = messages.count { MessageType.fromString(it.message_type) == type }
                if (count > 0) {
                    FilterChip(
                        label = type.displayName,
                        count = count,
                        color = type.color,
                        isSelected = selectedFilter == type,
                        onClick = { onFilterChange(type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (isSelected) color else color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else color
            )
            Text(
                "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else color,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White.copy(alpha = 0.3f) else color.copy(alpha = 0.1f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

// ---------- Edit toolbar ----------

@Composable
private fun EditToolbar(
    selectedCount: Int,
    filteredCount: Int,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F7FA))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSelectAll) {
            Icon(
                if (selectedCount == filteredCount) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = AuraTokens.Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (selectedCount == filteredCount) "取消全选" else "全选",
                color = AuraTokens.Primary
            )
        }

        TextButton(onClick = onDelete, enabled = selectedCount > 0) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = if (selectedCount > 0) Color(0xFFF56C6C) else Color(0xFFCCCCCC),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "删除($selectedCount)",
                color = if (selectedCount > 0) Color(0xFFF56C6C) else Color(0xFFCCCCCC)
            )
        }
    }
}

// ---------- Message list ----------

@Composable
private fun MessageList(
    messages: List<NotificationItem>,
    isEditMode: Boolean,
    selectedMessages: Set<Int>,
    onToggleSelect: (Int) -> Unit,
    onMessageClick: (NotificationItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        messages.forEachIndexed { index, message ->
            MessageRow(
                message = message,
                isEditMode = isEditMode,
                isSelected = selectedMessages.contains(message.id),
                onToggleSelect = { message.id?.let { onToggleSelect(it) } },
                onClick = { onMessageClick(message) }
            )
            if (index < messages.size - 1) {
                Divider(
                    color = Color(0xFFEDEDED),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = if (isEditMode) 88.dp else 72.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageRow(
    message: NotificationItem,
    isEditMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    val isRead = message.is_read == true
    val bg = if (isRead) Color.White else Color(0xFFF5F7FA)
    val msgType = MessageType.fromString(message.message_type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isEditMode) 8.dp else 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditMode) {
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) AuraTokens.Primary else Color(0xFFCCCCCC),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onToggleSelect)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Type icon circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(msgType.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                msgType.icon,
                contentDescription = null,
                tint = msgType.color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message.title.orEmpty(),
                    fontSize = 16.sp,
                    fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (!isEditMode) {
                    Text(
                        message.time_ago ?: "",
                        fontSize = 13.sp,
                        color = Color(0xFF999999)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                message.content.orEmpty(),
                fontSize = 14.sp,
                color = Color(0xFF666666),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isRead && !isEditMode) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
            )
        }
    }
}

// ---------- Empty view ----------

@Composable
private fun EmptyView(selectedFilter: MessageType?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Inbox,
            contentDescription = null,
            tint = AuraTokens.Primary.copy(alpha = 0.3f),
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            selectedFilter?.let { "暂无${it.displayName}" } ?: "暂无消息",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF909399)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "消息会在这里显示",
            fontSize = 14.sp,
            color = Color(0xFFC0C4CC)
        )
    }
}

// ---------- Message detail dialog ----------

@Composable
private fun MessageDetailDialog(
    message: NotificationItem,
    navController: NavHostController?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteAlert by remember { mutableStateOf(false) }
    val msgType = MessageType.fromString(message.message_type)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF5F7FA)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Surface(shadowElevation = 1.dp, color = Color.White) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showDeleteAlert = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = Color(0xFFF56C6C),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            "消息详情",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A),
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(onClick = onDismiss) {
                            Text("完成", color = AuraTokens.Primary, fontSize = 16.sp)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(msgType.color.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                msgType.icon,
                                contentDescription = null,
                                tint = msgType.color,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                msgType.displayName,
                                fontSize = 14.sp,
                                color = Color(0xFF999999)
                            )
                            Text(
                                message.title.orEmpty(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }

                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF999999),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            formatDetailTime(message.created_at),
                            fontSize = 14.sp,
                            color = Color(0xFF999999)
                        )
                    }

                    Divider(color = Color(0xFFEDEDED))

                    // Content
                    Text(
                        message.content.orEmpty(),
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        lineHeight = 26.sp
                    )

                    // Quick actions
                    if (msgType != MessageType.SYSTEM) {
                        Divider(color = Color(0xFFEDEDED))

                        Text(
                            "快捷操作",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                        ) {
                            val actions = getQuickActions(msgType, navController, onDismiss)
                            actions.forEachIndexed { index, action ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = action.second)
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        action.first,
                                        contentDescription = null,
                                        tint = msgType.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        action.third,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1A1A1A),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFFCCCCCC),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (index < actions.size - 1) {
                                    Divider(
                                        color = Color(0xFFEDEDED),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 46.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            if (showDeleteAlert) {
                StyledConfirmDialog(
                    icon = Icons.Filled.Delete,
                    title = "删除消息",
                    message = "确定要删除这条消息吗？",
                    confirmLabel = "删除",
                    onConfirm = {
                        showDeleteAlert = false
                        onDelete()
                    },
                    onDismiss = { showDeleteAlert = false }
                )
            }
        }
    }
}

private fun getQuickActions(
    type: MessageType,
    navController: NavHostController?,
    onDismiss: () -> Unit
): List<Triple<ImageVector, () -> Unit, String>> {
    val dismissAndNavigate: (String) -> Unit = { route ->
        onDismiss()
        navController?.navigate(route)
    }
    return when (type) {
        MessageType.MEDICATION -> listOf(
            Triple(Icons.Default.Medication, { dismissAndNavigate(Routes.MEDICATIONS) }, "查看用药计划"),
            Triple(Icons.Default.CheckCircle, { dismissAndNavigate(Routes.MEDICATIONS) }, "标记已服药")
        )
        MessageType.APPOINTMENT -> listOf(
            Triple(Icons.Default.CalendarMonth, { dismissAndNavigate(Routes.APPOINTMENTS) }, "查看预约详情"),
            Triple(Icons.Default.LocationOn, { dismissAndNavigate(Routes.HOSPITALS_MAP) }, "查看医院位置")
        )
        MessageType.HEALTH -> listOf(
            Triple(Icons.Default.Favorite, { dismissAndNavigate(Routes.MEDICAL_RECORDS) }, "查看健康数据")
        )
        MessageType.FAMILY -> listOf(
            Triple(Icons.Default.People, { dismissAndNavigate(Routes.FAMILY_MANAGEMENT) }, "查看家庭成员")
        )
        else -> emptyList()
    }
}
