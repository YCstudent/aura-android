package com.edistrive.aura.ui.screens.family

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.FamilyMember
import com.edistrive.aura.ui.components.AuraExtraColors
import com.edistrive.aura.ui.components.AvatarWithGenderBadge
import com.edistrive.aura.ui.components.CapsuleActionButton
import com.edistrive.aura.ui.components.GradientPillButton
import com.edistrive.aura.ui.components.StatusPill
import com.edistrive.aura.ui.components.StyledInputDialog
import com.edistrive.aura.ui.screens.family.components.InviteCodeDialog
import com.edistrive.aura.ui.state.FamilyViewModel
import com.edistrive.aura.ui.theme.AuraTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyManagementScreen(
    onBack: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (Int) -> Unit,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showInviteCodeDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FamilyMember?>(null) }

    LaunchedEffect(Unit) { viewModel.loadAll() }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "家庭成员",
                onBack = onBack,
                background = AuraExtraColors.GrayBg,
                trailing = {
                    androidx.compose.material3.IconButton(onClick = {
                        showInviteCodeDialog = true
                        viewModel.generateInviteCode { code, errMsg ->
                            if (code == null) {
                                showInviteCodeDialog = false
                                if (errMsg != null) {
                                    viewModel.setToast(errMsg)
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Link, contentDescription = "生成邀请码", tint = AuraTokens.Primary)
                    }
                    androidx.compose.material3.IconButton(onClick = onAddMember) {
                        Icon(Icons.Default.Add, contentDescription = "添加成员", tint = AuraTokens.Primary)
                    }
                }
            )
        },
        containerColor = AuraExtraColors.GrayBg
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                FamilyTabBar(
                    activeCount = uiState.activeMembers.size,
                    pendingCount = uiState.pendingMembers.size,
                    selectedTab = selectedTab,
                    onSelect = { selectedTab = it }
                )

                when {
                    uiState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AuraTokens.Primary)
                        }
                    }
                    selectedTab == 0 && uiState.activeMembers.isEmpty() -> {
                        FamilyEmptyState(
                            icon = Icons.Default.Group,
                            iconTint = AuraTokens.Primary.copy(alpha = 0.3f),
                            title = "还没有家庭成员",
                            subtitle = "添加家人，共同管理健康档案",
                            ctaLabel = "添加成员",
                            gradientStart = AuraTokens.Primary,
                            gradientEnd = AuraTokens.Primary2,
                            onCta = onAddMember
                        )
                    }
                    selectedTab == 1 && uiState.pendingMembers.isEmpty() -> {
                        FamilyEmptyState(
                            icon = Icons.Default.HourglassEmpty,
                            iconTint = AuraExtraColors.WarningOrange.copy(alpha = 0.3f),
                            title = "没有待激活的成员",
                            subtitle = "当有家人接受你的邀请后\n他们会出现在这里",
                            ctaLabel = "生成邀请码",
                            gradientStart = AuraExtraColors.SuccessGreen,
                            gradientEnd = AuraExtraColors.SuccessGreenLight,
                            onCta = {
                                showInviteCodeDialog = true
                                viewModel.generateInviteCode { code, errMsg ->
                                    if (code == null) {
                                        showInviteCodeDialog = false
                                        if (errMsg != null) viewModel.setToast(errMsg)
                                    }
                                }
                            }
                        )
                    }
                    else -> {
                        val list = if (selectedTab == 0) uiState.activeMembers else uiState.pendingMembers
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(list, key = { it.id ?: it.hashCode() }) { member ->
                                if (selectedTab == 0) {
                                    ActiveMemberCard(
                                        member = member,
                                        onOpenDetail = { member.id?.let(onOpenDetail) },
                                        onRename = { renameTarget = member },
                                        onEdit = { member.id?.let(onEditMember) },
                                        onUnlink = { member.id?.let(viewModel::unlink) }
                                    )
                                } else {
                                    PendingMemberCard(
                                        member = member,
                                        onActivate = { member.id?.let(viewModel::activate) },
                                        onDelete = { member.id?.let(viewModel::delete) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Toast
            AnimatedVisibility(
                visible = uiState.toast != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 24.dp, end = 24.dp)
            ) {
                ToastBanner(text = uiState.toast.orEmpty())
            }
            LaunchedEffect(uiState.toast) {
                if (uiState.toast != null) {
                    kotlinx.coroutines.delay(1800)
                    viewModel.consumeToast()
                }
            }
        }
    }

    if (showInviteCodeDialog) {
        InviteCodeDialog(
            code = uiState.generatedInviteCode.orEmpty(),
            onDismiss = {
                showInviteCodeDialog = false
                viewModel.clearGeneratedCode()
            },
            onRegenerate = {
                viewModel.clearGeneratedCode()
                viewModel.generateInviteCode { code, errMsg ->
                    if (code == null) {
                        showInviteCodeDialog = false
                        if (errMsg != null) viewModel.setToast(errMsg)
                    }
                }
            }
        )
    }

    renameTarget?.let { target ->
        RenameDialog(
            currentName = target.preferredName,
            onConfirm = { newName ->
                target.id?.let { viewModel.updateDisplayName(it, newName) }
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
}

// ----- Tab bar -----

@Composable
private fun FamilyTabBar(
    activeCount: Int,
    pendingCount: Int,
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
            FamilyTab(
                label = "我的家庭",
                icon = Icons.Default.Group,
                count = activeCount,
                selected = selectedTab == 0,
                onClick = { onSelect(0) },
                modifier = Modifier.weight(1f)
            )
            FamilyTab(
                label = "待激活",
                icon = Icons.Default.Schedule,
                count = pendingCount,
                badge = pendingCount > 0,
                selected = selectedTab == 1,
                onClick = { onSelect(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FamilyTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = if (selected) AuraTokens.Primary else AuraExtraColors.GrayLight
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                color = color,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (badge) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AuraExtraColors.ErrorRed)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text("$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else if (count > 0 && selected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text("($count)", color = color.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) AuraTokens.Primary else Color.Transparent)
        )
    }
}

// ----- Active member card -----

@Composable
private fun ActiveMemberCard(
    member: FamilyMember,
    onOpenDetail: () -> Unit,
    onRename: () -> Unit,
    onEdit: () -> Unit,
    onUnlink: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(onClick = onOpenDetail)
    ) {
        if (member.isAuthorized) {
            Box(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
            ) {
                StatusPill(
                    label = "已关联",
                    fg = AuraExtraColors.SuccessGreen,
                    bg = AuraExtraColors.SuccessGreen.copy(alpha = 0.1f),
                    icon = Icons.Default.Link
                )
            }
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarWithGenderBadge(
                    avatarUrl = member.displayAvatarUrl,
                    initial = member.initial,
                    gender = member.gender,
                    sizeDp = 72,
                    badgeDp = 24
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            member.preferredName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraExtraColors.DarkText
                        )
                        if (member.hasDisplayName) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("(备注)", color = AuraExtraColors.GrayLight, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        member.age?.let { InfoBadge("${it}岁") }
                        member.blood_type?.let { InfoBadge("${it}型") }
                        (member.relation_display ?: member.relation)?.let { InfoBadge(it) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CapsuleActionButton(
                    label = "改名",
                    icon = Icons.Default.Edit,
                    color = AuraExtraColors.WarningOrange,
                    onClick = onRename
                )
                CapsuleActionButton(
                    label = "编辑",
                    icon = Icons.Default.EditNote,
                    color = AuraTokens.Primary,
                    onClick = onEdit
                )
                if (member.isAuthorized) {
                    CapsuleActionButton(
                        label = "解除",
                        icon = Icons.Default.LinkOff,
                        color = AuraExtraColors.ErrorRed,
                        onClick = onUnlink
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AuraExtraColors.GrayLight.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, color = AuraExtraColors.GrayText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ----- Pending member card -----

@Composable
private fun PendingMemberCard(
    member: FamilyMember,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarWithGenderBadge(
                    avatarUrl = member.displayAvatarUrl,
                    initial = member.initial,
                    gender = member.gender,
                    sizeDp = 64,
                    badgeDp = 22
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        member.preferredName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraExtraColors.DarkText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusPill(
                        label = "待激活",
                        fg = AuraExtraColors.WarningOrange,
                        bg = AuraExtraColors.WarningOrange.copy(alpha = 0.1f),
                        icon = Icons.Default.Schedule
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "该成员已关联但尚未激活，激活后可互相查看健康档案",
                fontSize = 12.sp,
                color = AuraExtraColors.GrayLight
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientPillButton(
                    text = "激活",
                    gradientStart = AuraExtraColors.SuccessGreen,
                    gradientEnd = AuraExtraColors.SuccessGreenLight,
                    onClick = onActivate,
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.weight(1f)) {
                    CapsuleActionButton(
                        label = "删除",
                        icon = Icons.Default.LinkOff,
                        color = AuraExtraColors.ErrorRed,
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ----- Empty state -----

@Composable
private fun FamilyEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    ctaLabel: String,
    gradientStart: Color,
    gradientEnd: Color,
    onCta: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = AuraExtraColors.GrayLight, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        GradientPillButton(
            text = ctaLabel,
            gradientStart = gradientStart,
            gradientEnd = gradientEnd,
            onClick = onCta
        )
    }
}

// ----- Toast banner -----

@Composable
private fun ToastBanner(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(AuraExtraColors.SuccessGreen, AuraExtraColors.SuccessGreenLight)
                )
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ----- Rename dialog -----

@Composable
private fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    StyledInputDialog(
        icon = Icons.Default.Edit,
        title = "修改备注名",
        value = text,
        onValueChange = { text = it.take(20) },
        placeholder = "备注名",
        confirmLabel = "保存",
        onConfirm = {
            if (text.isNotBlank()) onConfirm(text.trim())
        },
        onDismiss = onDismiss
    )
}
