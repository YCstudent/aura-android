package com.edistrive.aura.ui.screens.family

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.FamilyMember
import com.edistrive.aura.ui.components.AuraExtraColors
import com.edistrive.aura.ui.components.AvatarWithGenderBadge
import com.edistrive.aura.ui.components.StatusPill
import com.edistrive.aura.ui.components.StyledConfirmDialog
import com.edistrive.aura.ui.screens.family.components.InviteCodeDialog
import com.edistrive.aura.ui.state.FamilyDetailViewModel
import com.edistrive.aura.ui.state.FamilyViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.util.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberDetailScreen(
    memberId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onOpenMedicalRecords: (Int) -> Unit,
    onOpenMedications: (Int) -> Unit,
    onOpenHealthReport: (Int) -> Unit,
    viewModel: FamilyDetailViewModel = hiltViewModel(),
    actionViewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val actionState by actionViewModel.uiState.collectAsState()
    var showUnlinkConfirm by remember { mutableStateOf(false) }
    var showInviteCodeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(memberId) { viewModel.load(memberId) }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = state.member?.preferredName?.ifBlank { null } ?: "成员详情",
                onBack = onBack,
                background = Color.Transparent,
                showDivider = false,
                trailing = {
                    if (state.member != null) {
                        androidx.compose.material3.IconButton(onClick = { onEdit(memberId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = AuraTokens.Primary)
                        }
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(AuraExtraColors.GrayBg, AuraExtraColors.DetailGradientEnd)
                )
            )
        ) {
            val member = state.member
            when {
                state.isLoading -> {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AuraTokens.Primary)
                    }
                }
                state.errorMessage != null -> {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding), contentAlignment = Alignment.Center) {
                        Text(state.errorMessage!!, color = AuraExtraColors.ErrorRed)
                    }
                }
                member == null -> {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding), contentAlignment = Alignment.Center) {
                        Text("未找到该成员", color = AuraExtraColors.GrayLight)
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        ProfileOverviewCard(
                            member = member,
                            onOpenMedicalRecords = { onOpenMedicalRecords(memberId) },
                            onOpenMedications = { onOpenMedications(memberId) },
                            onOpenHealthReport = { onOpenHealthReport(memberId) }
                        )
                        MetricsSection(member)
                        HealthArchiveSection(member)
                        AccountManagementSection(
                            member = member,
                            onUnlinkRequest = { showUnlinkConfirm = true },
                            onInvite = {
                                showInviteCodeDialog = true
                                actionViewModel.generateInviteCode { code, errMsg ->
                                    if (code == null) {
                                        showInviteCodeDialog = false
                                        if (errMsg != null) actionViewModel.setToast(errMsg)
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showUnlinkConfirm) {
        StyledConfirmDialog(
            icon = Icons.Default.LinkOff,
            title = "解除关联",
            message = "解除后将不再能互相查看健康档案，确认吗？",
            confirmLabel = "确认解除",
            isDanger = true,
            onConfirm = {
                actionViewModel.unlink(memberId)
                showUnlinkConfirm = false
                onBack()
            },
            onDismiss = { showUnlinkConfirm = false }
        )
    }

    if (showInviteCodeDialog) {
        InviteCodeDialog(
            code = actionState.generatedInviteCode.orEmpty(),
            onDismiss = {
                showInviteCodeDialog = false
                actionViewModel.clearGeneratedCode()
            },
            onRegenerate = {
                actionViewModel.clearGeneratedCode()
                actionViewModel.generateInviteCode { code, errMsg ->
                    if (code == null) {
                        showInviteCodeDialog = false
                        if (errMsg != null) actionViewModel.setToast(errMsg)
                    }
                }
            }
        )
    }
}

// ----- Profile overview -----

@Composable
private fun ProfileOverviewCard(
    member: FamilyMember,
    onOpenMedicalRecords: () -> Unit,
    onOpenMedications: () -> Unit,
    onOpenHealthReport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarWithGenderBadge(
                    avatarUrl = member.displayAvatarUrl,
                    initial = member.initial,
                    gender = member.gender,
                    sizeDp = 80,
                    badgeDp = 28
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            member.preferredName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraExtraColors.DarkText,
                            maxLines = 1
                        )
                        if (member.hasDisplayName) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("(备注)", color = AuraExtraColors.GrayLight, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    when {
                        member.isAuthorized -> StatusPill(
                            label = "已关联",
                            fg = Color.White,
                            bg = AuraExtraColors.SuccessGreen
                        )
                        member.isStandalone -> StatusPill(
                            label = "独立档案",
                            fg = AuraTokens.Primary,
                            bg = AuraTokens.Primary.copy(alpha = 0.1f)
                        )
                        member.isPending -> StatusPill(
                            label = "待激活",
                            fg = Color.White,
                            bg = AuraExtraColors.WarningOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val parts = listOfNotNull(
                        member.relation_display ?: member.relation,
                        member.age?.let { "${it}岁" },
                        member.blood_type?.let { "${it}型" },
                        member.phone?.takeIf { it.isNotBlank() }
                    )
                    if (parts.isNotEmpty()) {
                        Text(parts.joinToString(" · "), fontSize = 13.sp, color = AuraExtraColors.GrayLight)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = AuraExtraColors.GrayLightest)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickAction("健康病历", Icons.Default.Description, AuraTokens.Primary, onOpenMedicalRecords)
                Divider(modifier = Modifier
                    .height(40.dp)
                    .width(1.dp), color = AuraExtraColors.GrayLightest)
                QuickAction("用药提醒", Icons.Default.Schedule, AuraExtraColors.WarningOrange, onOpenMedications)
                Divider(modifier = Modifier
                    .height(40.dp)
                    .width(1.dp), color = AuraExtraColors.GrayLightest)
                QuickAction("健康报告", Icons.Default.MonitorHeart, AuraExtraColors.SuccessGreen, onOpenHealthReport)
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = AuraExtraColors.DarkText)
    }
}

// ----- Body metrics -----

@Composable
private fun MetricsSection(member: FamilyMember) {
    Column {
        Text("身体指标", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(120.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard(
                icon = Icons.Default.Person,
                label = "身高",
                value = member.height?.let { "${it.toInt()}" } ?: "-",
                unit = if (member.height != null) "cm" else "",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.MonitorHeart,
                label = "体重",
                value = member.weight?.let { "${it.toInt()}" } ?: "-",
                unit = if (member.weight != null) "kg" else "",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.CalendarMonth,
                label = "出生",
                value = DateFormat.chineseFromIso(member.birth_date),
                unit = "",
                modifier = Modifier.weight(1f),
                valueFontSize = 14
            )
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueFontSize: Int = 16
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .shadow(8.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(vertical = 20.dp)
    ) {
        Icon(icon, contentDescription = null, tint = AuraTokens.Primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = AuraExtraColors.GrayLight, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = valueFontSize.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(unit, color = AuraExtraColors.GrayLight, fontSize = 11.sp)
            }
        }
    }
}

// ----- Health archive -----

@Composable
private fun HealthArchiveSection(member: FamilyMember) {
    Column {
        Text("健康档案", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
        ) {
            Column {
                ArchiveRow("既往病史", member.medical_history, false)
                ArchiveRow("过敏史", member.allergy_history, true)
                ArchiveRow("慢性疾病", member.chronic_diseases, true)
                ArchiveRow("手术史", member.surgery_history, true)
                ArchiveRow("长期用药", member.medication_history, true)
                ArchiveRow("备注", member.notes, true)
            }
        }
    }
}

@Composable
private fun ArchiveRow(label: String, value: String?, withTopDivider: Boolean) {
    if (withTopDivider) Divider(color = AuraExtraColors.GrayLightest)
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AuraExtraColors.GrayLight)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value?.takeIf { it.isNotBlank() } ?: "无",
            fontSize = 15.sp,
            color = AuraExtraColors.DarkText
        )
    }
}

// ----- Account management -----

@Composable
private fun AccountManagementSection(
    member: FamilyMember,
    onUnlinkRequest: () -> Unit,
    onInvite: () -> Unit
) {
    Column {
        Text("账号管理", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
        Spacer(modifier = Modifier.height(12.dp))
        when {
            member.isAuthorized -> AuthorizedAccountCard(member, onUnlinkRequest)
            member.isStandalone -> StandaloneAccountCard(onInvite)
            member.isPending -> PendingAccountCard()
            else -> Box {}
        }
    }
}

@Composable
private fun AuthorizedAccountCard(member: FamilyMember, onUnlink: () -> Unit) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .shadow(8.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.05f))
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(AuraExtraColors.SuccessGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = AuraExtraColors.SuccessGreen, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("已关联账号", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "用户：${member.linked_user_info?.username ?: "未知"}",
                        fontSize = 14.sp,
                        color = AuraTokens.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "✓ 双方可互相查看健康档案、添加病历、共享提醒",
                fontSize = 12.sp,
                color = AuraExtraColors.SuccessGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUnlink,
                colors = ButtonDefaults.buttonColors(containerColor = AuraExtraColors.ErrorRed),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("解除关联", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StandaloneAccountCard(onInvite: () -> Unit) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .shadow(8.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.05f))
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(AuraTokens.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = AuraTokens.Primary, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("独立档案", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "此档案当前仅由您管理",
                        fontSize = 13.sp,
                        color = AuraExtraColors.GrayLight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = AuraTokens.Primary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "关联后支持双向同步健康数据",
                            fontSize = 12.sp,
                            color = AuraTokens.Primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onInvite,
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuraTokens.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("邀请家人关联", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PendingAccountCard() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .shadow(8.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.05f))
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(AuraExtraColors.WarningOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = AuraExtraColors.WarningOrange, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("等待对方接受", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
                Spacer(modifier = Modifier.height(2.dp))
                Text("邀请已发送，请等待家人接受", fontSize = 13.sp, color = AuraExtraColors.GrayLight)
            }
        }
    }
}
