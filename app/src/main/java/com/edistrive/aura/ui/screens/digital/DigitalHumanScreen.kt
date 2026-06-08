package com.edistrive.aura.ui.screens.digital

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.edistrive.aura.data.model.AIModel
import com.edistrive.aura.ui.components.AuraLoadingView
import com.edistrive.aura.ui.components.StyledOptionsDialog
import com.edistrive.aura.ui.state.ConversationViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalHumanScreen(
    onBack: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var isVoiceMode by remember { mutableStateOf(false) }
    var pageEntered by remember { mutableStateOf(false) }
    var showSelectTextDialog by remember { mutableStateOf(false) }
    var selectTextContent by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { pageEntered = true }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { viewModel.addSelectedImage(it); viewModel.handleImageSelected(it) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraUri?.let {
            viewModel.addSelectedImage(it)
            viewModel.handleImageSelected(it)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadConversations() }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    if (showAttachmentDialog) {
        StyledOptionsDialog(
            icon = Icons.Default.Image,
            title = "选择图片",
            options = listOf(
                "拍照" to "camera",
                "从相册选择" to "gallery"
            ),
            iconColor = AuraTokens.Primary,
            onSelect = { value ->
                showAttachmentDialog = false
                when (value) {
                    "camera" -> {
                        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                        cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        cameraLauncher.launch(cameraUri!!)
                    }
                    "gallery" -> imagePicker.launch("image/*")
                }
            },
            onDismiss = { showAttachmentDialog = false }
        )
    }

    DataNoticeSheet(
        show = state.showDataNotice,
        onConfirm = { viewModel.setShowDataNotice(false) }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF2F8F8), Color(0xFFEAF4F6), Color(0xFFF6F0FA)),
                    start = Offset.Zero, end = Offset.Infinite
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AnimatedVisibility(
                visible = pageEntered,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 350f),
                    initialOffsetY = { -it }
                ) + fadeIn(animationSpec = spring(dampingRatio = 0.82f, stiffness = 350f))
            ) {
                DigitalHeader(
                    onBack = onBack,
                    onShowSidebar = { viewModel.setShowSidebar(true) },
                    onNewConversation = { viewModel.createConversation() }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoadingMessages) {
                    AuraLoadingView(text = "加载中...")
                } else if (state.messages.isEmpty()) {
                    WelcomeView(
                        username = state.currentUsername,
                        onQuickQuestion = { prompt ->
                            viewModel.setInputText(prompt)
                            viewModel.sendMessage()
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                onLongPress = { msg ->
                                    if (!msg.isStreaming) viewModel.setShowActionSheet(true, msg)
                                },
                                onRegenerate = { /* TODO */ }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }

            AnimatedVisibility(
                visible = pageEntered,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 350f),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = spring(dampingRatio = 0.82f, stiffness = 350f))
            ) {
                BottomInputView(
                    inputText = state.inputText,
                    isSending = state.isSending,
                    selectedModel = state.selectedModel,
                    selectedImages = state.selectedImages,
                    isUploadingImage = state.isUploadingImage,
                    isVoiceMode = isVoiceMode,
                    canSend = state.inputText.isNotBlank() && !state.isSending,
                    onInputChange = { viewModel.setInputText(it) },
                    onSend = { viewModel.sendMessage() },
                    onToggleModel = { viewModel.setSelectedModel(it) },
                    onAttachment = { showAttachmentDialog = true },
                    onRemoveImage = { viewModel.removeImage(it) },
                    onToggleVoice = { isVoiceMode = !isVoiceMode },
                    onVoiceResult = { text ->
                        viewModel.setInputText(text)
                        isVoiceMode = false
                        viewModel.sendMessage()
                    }
                )
            }
        }

        ConversationSidebarOverlay(
            show = state.showSidebar,
            conversations = state.conversations,
            currentConversation = state.currentConversation,
            favoriteMessageIds = state.favoriteMessageIds,
            isCreatingConversation = state.isCreatingConversation,
            currentUsername = state.currentUsername,
            currentAvatarUrl = state.currentAvatarUrl,
            onDismiss = { viewModel.setShowSidebar(false) },
            onSelect = { viewModel.selectConversation(it) },
            onDelete = { viewModel.deleteConversation(it) },
            onRename = { id, title -> viewModel.renameConversation(id, title) },
            onCreate = { viewModel.createConversation() },
            onShowFavorites = { /* TODO */ }
        )

        ActionSheetOverlay(
            show = state.showActionSheet,
            message = state.actionSheetMessage,
            onDismiss = { viewModel.setShowActionSheet(false) },
            onCopy = { msg ->
                copyToClipboard(context, msg.content)
                viewModel.setShowActionSheet(false)
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            },
            onSelectText = { msg ->
                selectTextContent = msg.content
                viewModel.setShowActionSheet(false)
                showSelectTextDialog = true
            },
            onDelete = { msg ->
                viewModel.setShowActionSheet(false)
                viewModel.deleteMessage(msg)
            },
            onFavorite = { msg ->
                viewModel.setShowActionSheet(false)
                viewModel.toggleFavorite(msg)
            },
            onShare = { msg ->
                shareText(context, msg.content)
                viewModel.setShowActionSheet(false)
            },
            onFeedback = { msg, fb ->
                // Keep sheet open so user sees updated state (like iOS)
                viewModel.setFeedback(msg, fb)
            }
        )

        // Text selection dialog (iOS-like selectable text view)
        if (showSelectTextDialog) {
            SelectTextDialog(
                content = selectTextContent,
                onDismiss = { showSelectTextDialog = false }
            )
        }

        state.toast?.let { toast ->
            LaunchedEffect(toast) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearToast()
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 120.dp)
            ) { Text(toast) }
        }
    }
}

@Composable
private fun DigitalHeader(
    onBack: () -> Unit,
    onShowSidebar: () -> Unit,
    onNewConversation: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 4.dp)
        ) {
                // Leading: back + menu — IosTopBar style
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "返回",
                            tint = AuraTokens.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = onShowSidebar)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "对话列表",
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Center brand
                Box(
                    modifier = Modifier
                        .size(28.dp)
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
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "医小智",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AuraTokens.Primary
                    )
                    Text(
                        "AI 健康咨询助手",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                // Right: new conversation button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.linearGradient(
                                listOf(AuraTokens.Primary, AuraTokens.Primary2),
                                Offset.Zero, Offset.Infinite
                            )
                        )
                        .clickable(onClick = onNewConversation)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "新建",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
            }
            Divider(color = Color.Black.copy(alpha = 0.06f), thickness = 0.5.dp)
        }
}

@Composable
private fun BottomInputView(
    inputText: String,
    isSending: Boolean,
    selectedModel: AIModel,
    selectedImages: List<Uri>,
    isUploadingImage: Boolean,
    isVoiceMode: Boolean,
    canSend: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleModel: (AIModel) -> Unit,
    onAttachment: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onToggleVoice: () -> Unit,
    onVoiceResult: (String) -> Unit
) {
    val isReasoning = selectedModel == AIModel.REASONER

    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(color = Color(0xFFEDEDED), thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp)
                .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.10f))
                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .padding(bottom = 10.dp)
        ) {
            // Image previews
            if (selectedImages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp)
                ) {
                    selectedImages.forEachIndexed { index, uri ->
                        Box(modifier = Modifier.padding(end = 8.dp).size(80.dp)) {
                            AsyncImage(
                                model = uri, contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                            )
                            if (isUploadingImage && index == selectedImages.lastIndex) {
                                Box(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp) }
                            }
                            Icon(Icons.Default.Cancel, "删除", tint = Color.White,
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = -4.dp).size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .clickable { onRemoveImage(index) })
                        }
                    }
                }
            }

            // Input row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isVoiceMode) {
                    VoiceInputButton(
                        onResult = onVoiceResult,
                        enabled = !isSending,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        if (inputText.isEmpty()) {
                            Text(
                                "发消息或按住说话",
                                color = Color(0xFF999999),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 10.dp, start = 10.dp)
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp, max = 200.dp),
                            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF333333)),
                            cursorBrush = SolidColor(AuraTokens.Primary),
                            enabled = !isSending
                        )
                    }

                    if (canSend) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(8.dp, CircleShape, ambientColor = AuraTokens.Primary.copy(alpha = 0.4f))
                                .background(
                                    Brush.linearGradient(
                                        listOf(AuraTokens.Primary, AuraTokens.Primary2),
                                        Offset.Zero, Offset.Infinite
                                    ),
                                    CircleShape
                                )
                                .clickable { onSend() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Send,
                                "发送",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom toolbar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mode chip
                Box(
                    modifier = Modifier
                        .shadow(
                            if (isReasoning) 8.dp else 4.dp,
                            RoundedCornerShape(50),
                            ambientColor = if (isReasoning) AuraTokens.Primary.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.04f)
                        )
                        .then(
                            if (isReasoning) Modifier.background(
                                Brush.linearGradient(listOf(AuraTokens.Primary, AuraTokens.Primary2), Offset.Zero, Offset.Infinite),
                                RoundedCornerShape(50)
                            ) else Modifier.background(
                                Color.White.copy(alpha = 0.8f),
                                RoundedCornerShape(50)
                            )
                        )
                        .border(
                            1.dp,
                            if (isReasoning) Color.Transparent else Color(0xFFE8E8E8),
                            RoundedCornerShape(50)
                        )
                        .clickable(enabled = !isSending) {
                            onToggleModel(if (isReasoning) AIModel.CHAT else AIModel.REASONER)
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            tint = if (isReasoning) Color.White else Color(0xFF333333),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "深度思考",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isReasoning) Color.White else Color(0xFF333333)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Right buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // + button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                            .border(1.dp, Color(0xFFE8E8E8), CircleShape)
                            .clickable { onAttachment() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            "附件",
                            tint = Color(0xFF333333),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Mic/Voice toggle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                            .border(1.dp, Color(0xFFE8E8E8), CircleShape)
                            .clickable { onToggleVoice() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isVoiceMode) Icons.Default.Keyboard else Icons.Default.Mic,
                            "语音",
                            tint = if (isVoiceMode) AuraTokens.Primary else Color(0xFF333333),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectTextDialog(
    content: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF5F7FA)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(shadowElevation = 1.dp, color = Color.White) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "选取文字",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A),
                            modifier = Modifier.weight(1f).padding(start = 16.dp)
                        )
                        TextButton(onClick = onDismiss) {
                            Text("完成", color = AuraTokens.Primary, fontSize = 16.sp)
                        }
                    }
                }

                SelectionContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = content,
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        lineHeight = 26.sp
                    )
                }
            }
        }
    }
}
