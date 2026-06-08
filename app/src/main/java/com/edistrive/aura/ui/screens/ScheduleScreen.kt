package com.edistrive.aura.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.CreateScheduleRequest
import com.edistrive.aura.data.model.ScheduleResponse
import com.edistrive.aura.ui.components.IosTopBar
import com.edistrive.aura.ui.components.StyledConfirmDialog
import com.edistrive.aura.ui.state.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.*

private val PrimaryColor = Color(0xFF1A8080)
private val BgColor = Color(0xFFF2F2F7)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF666666)
private val TextTertiary = Color(0xFF999999)
private val TextDisabled = Color(0xFFCCCCCC)
private val WhiteBg = Color.White

private val TYPE_CONFIGS = mapOf(
    "appointment" to Triple(Icons.Filled.LocalHospital, "预约管理", Color(0xFF007AFF)),
    "medication" to Triple(Icons.Filled.Medication, "用药提醒", Color(0xFFFF9500)),
    "checkup" to Triple(Icons.Filled.Favorite, "体检复查", Color(0xFFFF3B30)),
    "exercise" to Triple(Icons.Filled.DirectionsRun, "运动计划", Color(0xFF34C759)),
    "other" to Triple(Icons.Filled.Event, "其他事项", Color(0xFF1A8080))
)

private val PRIORITY_COLORS = mapOf(
    "high" to Color(0xFFFF3B30),
    "medium" to Color(0xFFFF9500),
    "low" to Color(0xFF34C759)
)

@Composable
fun ScheduleScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val schedules = state.schedules

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var showingDetail by remember { mutableStateOf<ScheduleResponse?>(null) }
    var showingQuickAdd by remember { mutableStateOf(false) }
    var quickAddDate by remember { mutableStateOf<String?>(null) }
    var showingMonthPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSchedules()
    }

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val monthLabel = SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(currentMonth.time)

    // Precompute schedules grouped by date for O(1) cell lookup
    val schedulesByDate = remember(schedules) {
        schedules.groupBy { it.schedule_date ?: "" }
    }

    val monthSchedules = schedules.filter { s ->
        s.schedule_date?.let { dateStr ->
            val parts = dateStr.split("-")
            parts.size == 3 && parts[0].toIntOrNull() == currentMonth.get(Calendar.YEAR) &&
                parts[1].toIntOrNull() == currentMonth.get(Calendar.MONTH) + 1
        } ?: false
    }
    val monthCompleted = monthSchedules.count { it.is_completed == true }
    val monthUpcoming = monthSchedules.count { s ->
        s.is_completed != true && (s.schedule_date ?: "") >= todayStr
    }
    val monthOverdue = monthSchedules.count { s ->
        s.is_completed != true && (s.schedule_date ?: "") < todayStr
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        IosTopBar(title = "日历", onBack = onBack, background = BgColor, trailing = {
            IconButton(onClick = {
                quickAddDate = selectedDate ?: todayStr
                showingQuickAdd = true
            }) {
                Icon(Icons.Filled.Add, null, tint = PrimaryColor)
            }
        })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Month selector with clickable label → month picker
            MonthSelector(
                label = monthLabel,
                onPrev = {
                    val newCal = Calendar.getInstance()
                    newCal.timeInMillis = currentMonth.timeInMillis
                    newCal.add(Calendar.MONTH, -1)
                    currentMonth = newCal
                    selectedDate = null
                },
                onNext = {
                    val newCal = Calendar.getInstance()
                    newCal.timeInMillis = currentMonth.timeInMillis
                    newCal.add(Calendar.MONTH, 1)
                    currentMonth = newCal
                    selectedDate = null
                },
                onLabelClick = { showingMonthPicker = true }
            )

            // 2. Monthly stats
            MonthStatsRow(monthCompleted, monthUpcoming, monthOverdue)

            // 3. Calendar grid card — animates on month change
            AnimatedContent(
                targetState = currentMonth.timeInMillis,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                }
            ) { _ ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WhiteBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                                Text(
                                    day, Modifier.weight(1f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        CalendarGrid(
                            currentMonth = currentMonth,
                            schedulesByDate = schedulesByDate,
                            selectedDate = selectedDate,
                            todayStr = todayStr,
                            onSelectDate = { dateStr ->
                                selectedDate = if (dateStr == selectedDate) null else dateStr
                            },
                            onLongPressDate = { dateStr ->
                                quickAddDate = dateStr
                                showingQuickAdd = true
                            }
                        )
                    }
                }
            }

            // 4. Selected date schedules with slide+fade animation
            AnimatedVisibility(
                visible = selectedDate != null,
                enter = slideInVertically(
                    animationSpec = tween(350)
                ) { fullHeight -> fullHeight / 2 } + fadeIn(animationSpec = tween(350)),
                exit = slideOutVertically(
                    animationSpec = tween(300)
                ) { fullHeight -> fullHeight / 2 } + fadeOut(animationSpec = tween(300))
            ) {
                selectedDate?.let { dateStr ->
                    SelectedDateSection(
                        dateStr = dateStr,
                        schedules = schedulesByDate[dateStr] ?: emptyList(),
                        onToggle = { viewModel.toggleComplete(it.id) },
                        onTap = { showingDetail = it },
                        onAdd = {
                            quickAddDate = dateStr
                            showingQuickAdd = true
                        }
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // Month/year picker dialog
    if (showingMonthPicker) {
        MonthYearPickerDialog(
            currentYear = currentMonth.get(Calendar.YEAR),
            currentMonthVal = currentMonth.get(Calendar.MONTH) + 1,
            onDismiss = { showingMonthPicker = false },
            onConfirm = { year, month ->
                currentMonth = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                selectedDate = null
                showingMonthPicker = false
            }
        )
    }

    // Schedule detail dialog
    showingDetail?.let { schedule ->
        ScheduleDetailDialog(
            schedule = schedule,
            onDismiss = { showingDetail = null },
            onToggle = {
                viewModel.toggleComplete(schedule.id)
                showingDetail = null
            },
            onDelete = {
                viewModel.deleteSchedule(schedule.id)
                showingDetail = null
            }
        )
    }

    // Quick add dialog
    if (showingQuickAdd) {
        QuickAddDialog(
            dateStr = quickAddDate ?: todayStr,
            onDismiss = { showingQuickAdd = false },
            onSave = { req ->
                viewModel.createSchedule(req)
                showingQuickAdd = false
            }
        )
    }
}

// ========== MonthYearPicker Dialog (matches iOS MonthYearPicker) ==========

@Composable
private fun MonthYearPickerDialog(
    currentYear: Int,
    currentMonthVal: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit
) {
    val cal = Calendar.getInstance()
    val thisYear = cal.get(Calendar.YEAR)
    val years = (thisYear - 5..thisYear + 5).toList()

    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonthVal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("选择月份", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Year selector — horizontal scrollable chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("年份", fontSize = 13.sp, color = TextTertiary, modifier = Modifier.padding(start = 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        years.forEach { year ->
                            val isSel = year == selectedYear
                            Surface(
                                onClick = { selectedYear = year },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) PrimaryColor else Color(0xFFF5F7FA)
                            ) {
                                Text(
                                    "${year}年",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSel) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }

                // Month grid — 4 columns x 3 rows
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("月份", fontSize = 13.sp, color = TextTertiary, modifier = Modifier.padding(start = 4.dp))
                    val months = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
                    val rows = months.chunked(4)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                row.forEach { mLabel ->
                                    val mIdx = months.indexOf(mLabel) + 1
                                    val isSel = mIdx == selectedMonth
                                    Surface(
                                        onClick = { selectedMonth = mIdx },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) PrimaryColor else Color(0xFFF5F7FA),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            mLabel,
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .fillMaxWidth(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSel) Color.White else TextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                // Fill remaining cells if row has fewer than 4 items
                                repeat(4 - row.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = TextSecondary)
                }
                TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                    Text("确定", color = PrimaryColor, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        containerColor = WhiteBg
    )
}

// ========== Month Selector (matches iOS monthSelector) ==========

@Composable
private fun MonthSelector(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLabelClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.ChevronLeft, null, Modifier.size(18.dp), tint = PrimaryColor)
        }

        // Clickable month label centered between arrows
        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onLabelClick() }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.ArrowDropDown, null,
                    Modifier.size(14.dp), tint = TextSecondary
                )
            }
        }

        IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = PrimaryColor)
        }
    }
}

// ========== Month Stats (matches iOS monthStatisticsCard) ==========

@Composable
private fun MonthStatsRow(completed: Int, upcoming: Int, overdue: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatMiniCard(Icons.Filled.CheckCircle, "$completed", "已完成", Color(0xFF34C759), Modifier.weight(1f))
        StatMiniCard(Icons.Filled.Schedule, "$upcoming", "待办", Color(0xFFFF9500), Modifier.weight(1f))
        if (overdue > 0) {
            StatMiniCard(Icons.Filled.Warning, "$overdue", "逾期", Color(0xFFFF3B30), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(14.dp), tint = color)
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ========== Calendar Grid (matches iOS calendarGrid) ==========

@Composable
private fun CalendarGrid(
    currentMonth: Calendar,
    schedulesByDate: Map<String, List<ScheduleResponse>>,
    selectedDate: String?,
    todayStr: String,
    onSelectDate: (String) -> Unit,
    onLongPressDate: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.time = currentMonth.time
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val days = mutableListOf<String?>()
    repeat(firstDayOfWeek) { days.add(null) }
    for (d in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, d)
        days.add(dateFormat.format(cal.time))
    }
    while (days.size < 42) days.add(null)
    if (days.size >= 35 && days.takeLast(7).all { it == null }) {
        days.subList(35, days.size).clear()
    }
    if (days.size == 42 && days.subList(35, 42).all { it == null }) {
        days.subList(35, 42).clear()
    }

    val rows = days.chunked(7)

    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { dateStr ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (dateStr != null) {
                            DayCell(
                                dateStr = dateStr,
                                todayStr = todayStr,
                                selectedDate = selectedDate,
                                currentMonth = currentMonth,
                                daySchedules = schedulesByDate[dateStr] ?: emptyList(),
                                onSelect = { onSelectDate(dateStr) },
                                onLongPress = { onLongPressDate(dateStr) }
                            )
                        } else {
                            Spacer(Modifier.height(70.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    dateStr: String,
    todayStr: String,
    selectedDate: String?,
    currentMonth: Calendar,
    daySchedules: List<ScheduleResponse>,
    onSelect: () -> Unit,
    onLongPress: () -> Unit
) {
    val isToday = dateStr == todayStr
    val isSelected = dateStr == selectedDate
    val isCurrentMonth = dateStr.split("-")[1].toIntOrNull() == currentMonth.get(Calendar.MONTH) + 1

    val day = dateStr.split("-").last().toIntOrNull()?.toString() ?: ""
    val dayColor = when {
        isSelected -> Color.White
        isToday -> PrimaryColor
        isCurrentMonth -> TextPrimary
        else -> TextDisabled
    }

    // Spring animation for selection highlight — matches iOS withAnimation(.spring(response: 0.3))
    val targetBg = when {
        isSelected -> PrimaryColor
        isToday -> PrimaryColor.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    val highlightBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
    )

    val highlightBorder = if (isToday && !isSelected) PrimaryColor else Color.Transparent

    val activeSchedules = daySchedules.filter { it.is_completed != true }
    val hasHighPriority = activeSchedules.any { it.priority == "high" }

    Box(
        modifier = Modifier
            .height(70.dp)
            .combinedClickable(onClick = onSelect, onLongClick = onLongPress),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(highlightBg)
                    .border(
                        width = if (isToday && !isSelected) 2.dp else 0.dp,
                        color = highlightBorder,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    day,
                    fontSize = 16.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = dayColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(2.dp))

            if (daySchedules.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val showCount = minOf(daySchedules.size, 3)
                    for (i in 0 until showCount) {
                        val s = daySchedules[i]
                        val dotColor = if (s.is_completed == true) TextDisabled
                        else PRIORITY_COLORS[s.priority] ?: TextDisabled
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                if (daySchedules.size > 3) {
                    Text(
                        "+${daySchedules.size - 3}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary
                    )
                } else if (hasHighPriority) {
                    Icon(
                        Icons.Filled.Warning,
                        null,
                        Modifier.size(8.dp),
                        tint = Color(0xFFFF3B30)
                    )
                } else {
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ========== Selected Date Section (matches iOS selectedDateSchedules) ==========

@Composable
private fun SelectedDateSection(
    dateStr: String,
    schedules: List<ScheduleResponse>,
    onToggle: (ScheduleResponse) -> Unit,
    onTap: (ScheduleResponse) -> Unit,
    onAdd: () -> Unit
) {
    val sorted = schedules.sortedWith(compareBy({ it.is_completed ?: false }, { it.schedule_time ?: "" }))
    val dateLabel = formatSelectedDate(dateStr)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(dateLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (sorted.isNotEmpty()) {
                    Text("${sorted.size}个日程", fontSize = 14.sp, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, null, Modifier.size(16.dp), tint = PrimaryColor)
                Spacer(Modifier.width(4.dp))
                Text("添加", color = PrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (sorted.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.EventNote, null, Modifier.size(40.dp), tint = TextDisabled)
                    Text("当天暂无日程", fontSize = 14.sp, color = TextTertiary)
                    TextButton(onClick = onAdd) {
                        Text("添加日程", color = PrimaryColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = WhiteBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    sorted.forEachIndexed { i, schedule ->
                        CalendarScheduleRow(
                            schedule = schedule,
                            onTap = { onTap(schedule) },
                            onToggle = { onToggle(schedule) }
                        )
                        if (i < sorted.size - 1) {
                            Divider(Modifier.padding(start = 60.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarScheduleRow(
    schedule: ScheduleResponse,
    onTap: () -> Unit,
    onToggle: () -> Unit
) {
    val config = TYPE_CONFIGS[schedule.schedule_type]
    val typeColor = config?.third ?: Color(0xFF1A8080)
    val typeIcon = config?.first ?: Icons.Filled.Event
    val isCompleted = schedule.is_completed == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(22.dp)) {
            Icon(
                if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                null,
                Modifier.size(22.dp),
                tint = if (isCompleted) Color(0xFF34C759) else TextDisabled
            )
        }

        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(typeColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(typeIcon, null, Modifier.size(16.dp), tint = typeColor)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    schedule.title ?: "无标题",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (schedule.priority == "high" && !isCompleted) {
                    Icon(Icons.Filled.Warning, null, Modifier.size(12.dp), tint = Color(0xFFFF3B30))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(schedule.schedule_time?.take(5) ?: "", fontSize = 13.sp, color = TextSecondary)
                if (!schedule.location.isNullOrBlank()) {
                    Text("·", fontSize = 13.sp, color = TextDisabled)
                    Text(schedule.location, fontSize = 13.sp, color = TextSecondary, maxLines = 1)
                }
            }
        }

        Icon(Icons.Filled.ChevronRight, null, Modifier.size(12.dp), tint = TextDisabled)
    }
}

// ========== Schedule Detail Dialog (matches iOS ScheduleDetailSheet) ==========

@Composable
private fun ScheduleDetailDialog(
    schedule: ScheduleResponse,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val config = TYPE_CONFIGS[schedule.schedule_type]
    val typeColor = config?.third ?: Color(0xFF1A8080)
    val typeLabel = config?.second ?: "其他事项"
    val typeIcon = config?.first ?: Icons.Filled.Event
    val isCompleted = schedule.is_completed == true
    val priorityColor = PRIORITY_COLORS[schedule.priority] ?: Color(0xFF909399)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("日程详情") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeColor.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(typeIcon, null, Modifier.size(16.dp), tint = typeColor)
                        Text(typeLabel, fontSize = 15.sp, color = typeColor)
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F7FA))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(priorityColor))
                        Text(
                            when (schedule.priority) { "high" -> "高"; "medium" -> "中"; else -> "低" },
                            fontSize = 15.sp, color = TextSecondary
                        )
                    }
                }

                Text(schedule.title ?: "无标题", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailInfoRow(Icons.Filled.CalendarMonth, "日期", displayDateShort(schedule.schedule_date))
                    DetailInfoRow(Icons.Filled.Schedule, "时间", schedule.schedule_time?.take(5) ?: "")
                    if (!schedule.location.isNullOrBlank()) {
                        DetailInfoRow(Icons.Filled.LocationOn, "地点", schedule.location)
                    }
                }

                if (!schedule.description.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("描述", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(schedule.description, fontSize = 15.sp, color = TextSecondary)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F7FA))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                        null, Modifier.size(20.dp),
                        tint = if (isCompleted) Color(0xFF34C759) else TextDisabled
                    )
                    Text(
                        if (isCompleted) "已完成" else "未完成",
                        fontSize = 15.sp, color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    showDeleteConfirm = true
                }) {
                    Text("删除", color = Color(0xFFF56C6C))
                }
                TextButton(onClick = {
                    onToggle()
                }) {
                    Text(if (isCompleted) "标记未完成" else "标记完成", color = PrimaryColor)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        containerColor = WhiteBg
    )

    if (showDeleteConfirm) {
        StyledConfirmDialog(
            icon = Icons.Default.Delete,
            title = "确认删除",
            message = "确定要删除「${schedule.title}」吗？",
            confirmLabel = "删除",
            isDanger = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun DetailInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(16.dp).width(24.dp), tint = PrimaryColor)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 13.sp, color = TextTertiary)
            Text(value, fontSize = 15.sp, color = TextPrimary)
        }
    }
}

// ========== Quick Add Dialog (matches iOS QuickAddFromCalendar) ==========

@Composable
private fun QuickAddDialog(
    dateStr: String,
    onDismiss: () -> Unit,
    onSave: (CreateScheduleRequest) -> Unit
) {
    val dateLabel = formatSelectedDate(dateStr)
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("other") }
    var selectedTime by remember { mutableStateOf("09:00") }
    val titleValid = title.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WhiteBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                // Date header — matches iOS header with f5f7fa background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F7FA))
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        dateLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryColor
                    )
                    Text(
                        "快速添加日程",
                        fontSize = 14.sp,
                        color = TextTertiary
                    )
                }

                // Content area
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Title input — matches iOS TextField with label
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("标题", fontSize = 14.sp, color = TextTertiary)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("输入日程标题", color = TextDisabled) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = Color(0xFFE8E8E8)
                            )
                        )
                    }

                    // Type selector — matches iOS QuickTypeButton row
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("类型", fontSize = 14.sp, color = TextTertiary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for ((key, triple) in TYPE_CONFIGS) {
                                val (icon, label, color) = triple
                                val sel = selectedType == key
                                val chipBg = if (sel) color.copy(alpha = 0.12f) else Color(0xFFF5F7FA)

                                Surface(
                                    onClick = { selectedType = key },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .then(
                                                if (sel) Modifier.border(1.5.dp, color, RoundedCornerShape(10.dp))
                                                else Modifier
                                            )
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(chipBg)
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            icon, null, Modifier.size(15.dp),
                                            tint = if (sel) color else TextSecondary
                                        )
                                        Text(
                                            label,
                                            fontSize = 13.sp,
                                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (sel) color else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Time input — matches iOS time field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("时间", fontSize = 14.sp, color = TextTertiary)
                        OutlinedTextField(
                            value = selectedTime,
                            onValueChange = { selectedTime = it },
                            placeholder = { Text("HH:mm", color = TextDisabled) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Schedule, null,
                                    Modifier.size(18.dp), tint = PrimaryColor
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = Color(0xFFE8E8E8)
                            )
                        )
                    }
                }

                // Bottom bar — cancel + save buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消", color = TextSecondary, fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            if (titleValid) {
                                onSave(CreateScheduleRequest(
                                    schedule_type = selectedType, title = title, description = "",
                                    schedule_date = dateStr, schedule_time = "$selectedTime:00",
                                    priority = "medium", reminder = true, location = ""
                                ))
                            }
                        },
                        enabled = titleValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor,
                            disabledContainerColor = TextDisabled
                        )
                    ) {
                        Text(
                            "添加日程",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (titleValid) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ========== Helpers ==========

private fun formatSelectedDate(dateStr: String): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = sdf.parse(dateStr) ?: return dateStr
    val displayFormat = SimpleDateFormat("MM月dd日 EEEE", Locale.CHINESE)
    return displayFormat.format(date)
}

private fun displayDateShort(dateStr: String?): String {
    if (dateStr == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = sdf.parse(dateStr) ?: return dateStr
    val cal = Calendar.getInstance()
    cal.time = date
    val today = Calendar.getInstance()
    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "今天"
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1 -> "明天"
        else -> SimpleDateFormat("MM月dd日", Locale.getDefault()).format(date)
    }
}
