package com.edistrive.aura.ui.screens.medication

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.Medication
import com.edistrive.aura.data.model.MedicationRequestModel
import com.edistrive.aura.ui.components.AuraExtraColors
import com.edistrive.aura.ui.components.IosTopBar
import com.edistrive.aura.ui.screens.medication.components.MedicationRequestCard
import com.edistrive.aura.ui.state.MedicationViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.util.DateFormat
import java.time.LocalTime
import kotlinx.coroutines.delay

// Diagonal gradient matching iOS topLeading → bottomTrailing
private val DiagonalTeal = Brush.linearGradient(
    listOf(AuraTokens.Primary, AuraTokens.Primary2),
    start = Offset.Zero,
    end = Offset.Infinite
)
private val DiagonalTealLight = Brush.linearGradient(
    listOf(AuraTokens.Primary.copy(alpha = 0.2f), AuraTokens.Primary2.copy(alpha = 0.2f)),
    start = Offset.Zero,
    end = Offset.Infinite
)
private val DiagonalTealLighter = Brush.linearGradient(
    listOf(AuraTokens.Primary.copy(alpha = 0.1f), AuraTokens.Primary2.copy(alpha = 0.1f)),
    start = Offset.Zero,
    end = Offset.Infinite
)
private val DiagonalTealBorder = Brush.linearGradient(
    listOf(AuraTokens.Primary.copy(alpha = 0.3f), AuraTokens.Primary2.copy(alpha = 0.3f)),
    start = Offset.Zero,
    end = Offset.Infinite
)
private val DiagonalSuccess = Brush.linearGradient(
    listOf(AuraExtraColors.SuccessGreen, AuraExtraColors.SuccessGreenLight),
    start = Offset.Zero,
    end = Offset.Infinite
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationManagementScreen(
    memberId: Int?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    // Android 13+ notification permission — matching iOS requestAuthorization in AppState.init()
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — reminders still work either way, notification just won't show */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(memberId) { viewModel.loadAll(memberId) }

    Scaffold(
        topBar = {
            IosTopBar(
                title = "用药提醒",
                onBack = onBack,
                background = AuraExtraColors.GrayBg,
                trailing = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = "新建", tint = AuraTokens.Primary)
                    }
                }
            )
        },
        containerColor = AuraExtraColors.GrayBg
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MedicationTabBar(
                    expiredCount = state.expiredMedications.size,
                    pendingRequestCount = state.requests.count { it.status == "pending" },
                    selectedTab = selectedTab,
                    onSelect = { selectedTab = it }
                )

                when (selectedTab) {
                    0 -> ActiveTab(
                        today = state.todayMedications,
                        all = state.activeMedications,
                        onAdd = onAdd,
                        onEdit = onEdit,
                        onTake = viewModel::take,
                        onCancel = viewModel::cancelTake,
                        onDelete = viewModel::delete,
                        onToggle = viewModel::toggleActive
                    )
                    1 -> ExpiredTab(state.expiredMedications, onDelete = viewModel::delete)
                    2 -> RequestsTab(
                        requests = state.requests,
                        onAccept = viewModel::acceptRequest,
                        onReject = viewModel::rejectRequest,
                        onWithdraw = viewModel::withdrawRequest,
                        onDelete = viewModel::deleteRequest
                    )
                }
            }

            AnimatedVisibility(
                visible = state.toast != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 24.dp, end = 24.dp)
            ) {
                ToastBanner(text = state.toast.orEmpty())
            }
            LaunchedEffect(state.toast) {
                if (state.toast != null) {
                    delay(1800)
                    viewModel.consumeToast()
                }
            }
        }
    }
}

// ----- Tab Bar (matching iOS tabSelector) -----

@Composable
private fun MedicationTabBar(
    expiredCount: Int,
    pendingRequestCount: Int,
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    Surface(color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            MedTab("我的用药", null, selectedTab == 0, { onSelect(0) }, Modifier.weight(1f))
            MedTab(
                "已结束",
                if (expiredCount > 0) "$expiredCount" else null,
                selectedTab == 1,
                { onSelect(1) },
                Modifier.weight(1f)
            )
            MedTab(
                "收到的请求",
                if (pendingRequestCount > 0) "$pendingRequestCount" else null,
                selectedTab == 2,
                { onSelect(2) },
                Modifier.weight(1f),
                redBadge = pendingRequestCount > 0
            )
        }
    }
}

@Composable
private fun MedTab(
    label: String,
    countText: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    redBadge: Boolean = false
) {
    val color = if (selected) AuraTokens.Primary else AuraExtraColors.GrayLight
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = color,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (countText != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (redBadge) Color.Red else color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        countText,
                        color = if (redBadge) Color.White else color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) AuraTokens.Primary else Color.Transparent)
        )
    }
}

// ----- Active tab -----

@Composable
private fun ActiveTab(
    today: List<Medication>,
    all: List<Medication>,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onTake: (Int, String) -> Unit,
    onCancel: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onToggle: (Int) -> Unit
) {
    // Optimistic UI state: key = "${medId}_${time}", value = "taken" or "pending"
    val optimistic = remember { mutableStateMapOf<String, String>() }

    // Clear optimistic state when real data refreshes from API
    LaunchedEffect(all, today) {
        optimistic.clear()
    }

    if (all.isEmpty() && today.isEmpty()) {
        EmptyMedicationState(onAdd)
        return
    }

    fun optKey(id: Int, time: String) = "${id}_${time}"

    fun isTaken(med: Medication, time: String): Boolean {
        val key = med.id?.let { optKey(it, time) } ?: return false
        return when (optimistic[key]) {
            "taken" -> true
            "pending" -> false
            else -> med.records_today.orEmpty().any { it.effectiveTime == time && it.status == "taken" }
        }
    }

    fun onTakeOptimistic(id: Int, time: String) {
        optimistic[optKey(id, time)] = "taken"
        onTake(id, time)
    }

    fun onCancelOptimistic(id: Int, time: String) {
        optimistic[optKey(id, time)] = "pending"
        onCancel(id, time)
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            TodaySection(today, onEdit, ::onTakeOptimistic, ::onCancelOptimistic, ::isTaken)
        }
        item {
            AllRemindersSection(all, onEdit, onDelete, onToggle, ::onTakeOptimistic, ::onCancelOptimistic, ::isTaken)
        }
    }
}

// ----- Today section (matching iOS todayMedicationsCard) -----

@Composable
private fun TodaySection(
    today: List<Medication>,
    onEdit: (Int) -> Unit,
    onTake: (Int, String) -> Unit,
    onCancel: (Int, String) -> Unit,
    isTaken: (Medication, String) -> Boolean = { m, t -> m.records_today.orEmpty().any { it.effectiveTime == t && it.status == "taken" } }
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = AuraTokens.Primary, modifier = Modifier.size(18.dp))
                Text("今日用药", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AuraExtraColors.DarkText)
            }
            if (today.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Medication, contentDescription = null, tint = AuraTokens.Primary.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("今天没有用药安排", fontSize = 14.sp, color = AuraExtraColors.GrayLight)
                        Text("保持健康的生活方式", fontSize = 12.sp, color = AuraExtraColors.GrayLightest)
                    }
                }
            } else {
                today.forEach { med ->
                    TodayMedicationCard(
                        medication = med,
                        onClick = { med.id?.let(onEdit) },
                        onTake = onTake,
                        onCancel = onCancel,
                        isTaken = { time -> isTaken(med, time) }
                    )
                }
            }
        }
    }
}

// ----- Today medication card (matching iOS TodayMedicationCard) -----

@Composable
private fun TodayMedicationCard(
    medication: Medication,
    onClick: () -> Unit,
    onTake: (Int, String) -> Unit,
    onCancel: (Int, String) -> Unit,
    isTaken: (String) -> Boolean = { t -> medication.records_today.orEmpty().any { it.effectiveTime == t && it.status == "taken" } }
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, DiagonalTealBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pill icon - 52dp diagonal-gradient circle with shadow (matching iOS)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(8.dp, CircleShape, ambientColor = AuraTokens.Primary.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(DiagonalTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Name + active status pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        medication.name.orEmpty(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraExtraColors.DarkText,
                        modifier = Modifier.weight(1f)
                    )
                    ActiveStatusPill(active = medication.is_active != false)
                }

                medication.dosage?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 14.sp, color = AuraExtraColors.GrayText)
                }

                // Time tags (matching iOS ClickableTimeTagView horizontal scroll)
                val times = medication.reminder_times.orEmpty()
                if (times.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(times) { time ->
                            val taken = isTaken(time)
                            val passed = !taken && isPassedTime(time)
                            TimeTag(
                                time = time,
                                state = when {
                                    taken -> TimeTagState.Taken
                                    passed -> TimeTagState.Passed
                                    else -> TimeTagState.Pending
                                },
                                onTap = {
                                    medication.id?.let { id ->
                                        if (taken) onCancel(id, time) else onTake(id, time)
                                    }
                                }
                            )
                        }
                    }
                }

                // Bottom date row (matching iOS bottom HStack)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AuraExtraColors.GrayLight, modifier = Modifier.size(11.dp))
                        Text(
                            "${medication.start_date.orEmpty()} 开始",
                            fontSize = 12.sp,
                            color = AuraExtraColors.GrayLight
                        )
                    }
                    medication.days_remaining?.takeIf { it > 0 }?.let { days ->
                        Text("·", color = Color(0xFFE4E7ED), fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("剩", fontSize = 12.sp, color = AuraExtraColors.WarningOrange)
                            Text("$days", fontSize = 13.sp, color = AuraExtraColors.WarningOrange, fontWeight = FontWeight.Bold)
                            Text("天", fontSize = 12.sp, color = AuraExtraColors.WarningOrange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveStatusPill(active: Boolean) {
    val (label, fg, bg) = if (active) {
        Triple("进行中", AuraExtraColors.SuccessGreen, AuraExtraColors.SuccessGreen.copy(alpha = 0.1f))
    } else {
        Triple("已暂停", AuraExtraColors.GrayLight, AuraExtraColors.GrayBgLighter)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ----- Time tag states -----

private enum class TimeTagState { Pending, Taken, Passed }

@Composable
private fun TimeTag(time: String, state: TimeTagState, onTap: () -> Unit) {
    val (bgBrush, fg, borderBrush) = when (state) {
        TimeTagState.Pending -> Triple(
            Brush.horizontalGradient(
                listOf(AuraTokens.Primary.copy(alpha = 0.15f), AuraTokens.Primary2.copy(alpha = 0.15f))
            ),
            AuraTokens.Primary,
            Brush.horizontalGradient(
                listOf(AuraTokens.Primary.copy(alpha = 0.3f), AuraTokens.Primary2.copy(alpha = 0.3f))
            )
        )
        TimeTagState.Taken -> Triple(
            Brush.horizontalGradient(listOf(AuraExtraColors.SuccessGreen, AuraExtraColors.SuccessGreenLight)),
            Color.White,
            null
        )
        TimeTagState.Passed -> Triple(
            Brush.horizontalGradient(listOf(AuraExtraColors.GrayLight, Color(0xFF7F8C8D))),
            Color.White,
            null
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgBrush)
            .then(
                if (borderBrush != null) Modifier.border(1.dp, borderBrush, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        val icon = if (state == TimeTagState.Taken) Icons.Default.CheckCircle else Icons.Default.Schedule
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(time, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun isPassedTime(timeStr: String): Boolean {
    return try {
        LocalTime.now().isAfter(LocalTime.parse(timeStr))
    } catch (_: Exception) { false }
}

// ----- All reminders section (matching iOS activeMedicationsCard) -----

@Composable
private fun AllRemindersSection(
    all: List<Medication>,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onTake: (Int, String) -> Unit,
    onCancelTake: (Int, String) -> Unit,
    isTaken: (Medication, String) -> Boolean = { m, t -> m.records_today.orEmpty().any { it.effectiveTime == t && it.status == "taken" } }
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.List, contentDescription = null, tint = AuraTokens.Primary, modifier = Modifier.size(18.dp))
                Text("全部提醒", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AuraExtraColors.DarkText)
            }
            if (all.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, contentDescription = null, tint = AuraTokens.Primary.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("暂无提醒", fontSize = 14.sp, color = AuraExtraColors.GrayLight)
                        Text("点击右上角添加用药提醒", fontSize = 12.sp, color = AuraExtraColors.GrayLightest)
                    }
                }
            } else {
                all.forEach { med ->
                    AllReminderRow(
                        medication = med,
                        onEdit = { med.id?.let(onEdit) },
                        onDelete = { med.id?.let(onDelete) },
                        onToggle = { med.id?.let(onToggle) },
                        onTake = onTake,
                        onCancelTake = onCancelTake,
                        isTaken = { time -> isTaken(med, time) }
                    )
                }
            }
        }
    }
}

// ----- All reminder row (matching iOS MedicationRow) -----

@Composable
private fun AllReminderRow(
    medication: Medication,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onTake: (Int, String) -> Unit,
    onCancelTake: (Int, String) -> Unit,
    isTaken: (String) -> Boolean = { t -> medication.records_today.orEmpty().any { it.effectiveTime == t && it.status == "taken" } }
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top: pill icon + name/dosage·freq + toggle
            Row(verticalAlignment = Alignment.Top) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 40dp pill icon with diagonal gradient bg
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DiagonalTealLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Medication, contentDescription = null, tint = AuraTokens.Primary, modifier = Modifier.size(18.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(medication.name.orEmpty(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
                        val dosageFreq = listOfNotNull(medication.dosage, medication.frequency).joinToString(" · ")
                        if (dosageFreq.isNotBlank()) {
                            Text(dosageFreq, fontSize = 13.sp, color = AuraExtraColors.GrayLight)
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = medication.is_active != false,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AuraTokens.Primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = AuraExtraColors.GrayLightest
                    )
                )
            }

            // Today check-in zone (matching iOS check-in VStack)
            val times = medication.reminder_times.orEmpty()
            if (times.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AuraExtraColors.GrayBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuraExtraColors.SuccessGreen, modifier = Modifier.size(13.dp))
                            Text("今日打卡", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AuraExtraColors.GrayText)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(times) { time ->
                                val taken = isTaken(time)
                                CheckInButton(
                                    time = time,
                                    taken = taken,
                                    onCheckIn = {
                                        medication.id?.let { id -> onTake(id, time) }
                                    },
                                    onCancel = {
                                        medication.id?.let { id -> onCancelTake(id, time) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = Color(0xFFE4E7ED))

            // Notes row (if any)
            medication.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AuraExtraColors.GrayBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = AuraExtraColors.GrayLight, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(notes, fontSize = 13.sp, color = AuraExtraColors.GrayText, maxLines = 2)
                }
                Divider(color = Color(0xFFE4E7ED))
            }

            // Bottom: date info + edit/delete buttons (matching iOS bottom HStack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Date info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AuraExtraColors.GrayLight, modifier = Modifier.size(11.dp))
                        Text(
                            "${medication.start_date.orEmpty()} 开始",
                            fontSize = 12.sp,
                            color = AuraExtraColors.GrayLight,
                            maxLines = 1
                        )
                    }
                    medication.days_remaining?.takeIf { it > 0 }?.let { days ->
                        Text("·", color = Color(0xFFE4E7ED), fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = AuraExtraColors.WarningOrange, modifier = Modifier.size(10.dp))
                            Text("剩 $days 天", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AuraExtraColors.WarningOrange)
                        }
                    }
                }

                // Edit button (matching iOS capsule style)
                MedicationCapsuleButton(
                    label = "编辑",
                    icon = Icons.Default.Edit,
                    color = AuraTokens.Primary,
                    onClick = onEdit
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Delete button
                MedicationCapsuleButton(
                    label = "删除",
                    icon = Icons.Default.Delete,
                    color = AuraExtraColors.ErrorRed,
                    onClick = onDelete
                )
            }
        }
    }
}

// ----- Capsule action button matching iOS padding H:12 V:7 -----

@Composable
private fun MedicationCapsuleButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ----- Check-in button (matching iOS CheckInButton with press animation) -----

@Composable
private fun CheckInButton(
    time: String,
    taken: Boolean,
    onCheckIn: () -> Unit,
    onCancel: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "checkInScale"
    )

    val bg = if (taken) DiagonalSuccess else DiagonalTealLighter
    val borderMod = if (taken) Modifier else Modifier.border(1.5.dp, AuraTokens.Primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
    val shadowMod = if (taken) Modifier.shadow(6.dp, RoundedCornerShape(10.dp), ambientColor = AuraExtraColors.SuccessGreen.copy(alpha = 0.3f))
    else Modifier

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(shadowMod)
            .background(bg, RoundedCornerShape(10.dp))
            .then(borderMod)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable {
                pressed = true
                if (taken) onCancel() else onCheckIn()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (taken) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                Text("已服用", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = AuraTokens.Primary, modifier = Modifier.size(20.dp))
                Text(time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraTokens.Primary)
            }
        }
    }

    // Reset press state after animation
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(100)
            pressed = false
        }
    }
}

// ----- Expired tab -----

@Composable
private fun ExpiredTab(list: List<Medication>, onDelete: (Int) -> Unit) {
    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuraExtraColors.GrayLight, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("暂无已结束的用药", fontSize = 14.sp, color = AuraExtraColors.GrayLight)
                Text("已结束的用药计划会显示在这里", fontSize = 12.sp, color = AuraExtraColors.GrayLightest)
            }
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(list, key = { it.id ?: it.hashCode() }) { med ->
            ExpiredCard(med, onDelete = { med.id?.let(onDelete) })
        }
    }
}

// ----- Expired card (matching iOS ExpiredMedicationRow) -----

@Composable
private fun ExpiredCard(med: Medication, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFAFAFA))
            .border(1.dp, Color(0xFFE4E7ED), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top row: pill icon + name/dosage + expired badge
            Row(verticalAlignment = Alignment.Top) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Pill icon - solid gray bg matching iOS #f4f4f5
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AuraExtraColors.GrayBgLighter),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Medication,
                            contentDescription = null,
                            tint = AuraExtraColors.GrayLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Muted name matching iOS #909399
                        Text(med.name.orEmpty(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraExtraColors.GrayLight)
                        val dosageFreq = listOfNotNull(med.dosage, med.frequency).joinToString(" · ")
                        if (dosageFreq.isNotBlank()) {
                            // Extra muted dosage matching iOS #c0c4cc
                            Text(dosageFreq, fontSize = 13.sp, color = Color(0xFFC0C4CC))
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                // Expired badge matching iOS "已结束" pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AuraExtraColors.GrayBgLighter)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuraExtraColors.GrayLight, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("已结束", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AuraExtraColors.GrayLight)
                }
            }

            Divider(color = Color(0xFFE4E7ED))

            // Bottom: date range + delete button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFC0C4CC), modifier = Modifier.size(13.dp))
                    Text(
                        "${med.start_date.orEmpty()} 至 ${med.end_date.orEmpty()}",
                        fontSize = 13.sp,
                        color = AuraExtraColors.GrayLight
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Delete button matching iOS gradient+overlay+shadow style
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AuraExtraColors.ErrorRed.copy(alpha = 0.08f),
                                    AuraExtraColors.ErrorRedLight.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.5.dp, AuraExtraColors.ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .shadow(4.dp, RoundedCornerShape(10.dp), ambientColor = AuraExtraColors.ErrorRed.copy(alpha = 0.1f))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = AuraExtraColors.ErrorRed, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("删除", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AuraExtraColors.ErrorRed)
                }
            }
        }
    }
}

// ----- Requests tab -----

@Composable
private fun RequestsTab(
    requests: List<MedicationRequestModel>,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onWithdraw: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    if (requests.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Mail, contentDescription = null, tint = AuraTokens.Primary.copy(alpha = 0.3f), modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("暂无用药请求", fontSize = 16.sp, color = AuraExtraColors.GrayLight)
            Text("家庭成员发送的用药请求会显示在这里", fontSize = 13.sp, color = AuraExtraColors.GrayLightest)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(requests, key = { it.id ?: it.hashCode() }) { req ->
            MedicationRequestCard(
                request = req,
                onAccept = { req.id?.let(onAccept) },
                onReject = { req.id?.let(onReject) },
                onWithdraw = { req.id?.let(onWithdraw) },
                onDelete = { req.id?.let(onDelete) }
            )
        }
    }
}

// ----- Empty state -----

@Composable
private fun EmptyMedicationState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Medication, contentDescription = null, tint = AuraTokens.Primary.copy(alpha = 0.3f), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("还没有用药计划", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
        Spacer(modifier = Modifier.height(8.dp))
        Text("添加用药提醒，按时服药更安心", fontSize = 14.sp, color = AuraExtraColors.GrayLight)
        Spacer(modifier = Modifier.height(24.dp))
        com.edistrive.aura.ui.components.GradientPillButton(
            text = "新建用药",
            gradientStart = AuraTokens.Primary,
            gradientEnd = AuraTokens.Primary2,
            onClick = onAdd
        )
    }
}

// ----- Toast banner (matching iOS toast) -----

@Composable
private fun ToastBanner(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(AuraExtraColors.SuccessGreen, AuraExtraColors.SuccessGreenLight)
                )
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
