package com.edistrive.aura.ui.screens.medical

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.MedicalRecord
import com.edistrive.aura.ui.state.MedicalRecordTypes
import com.edistrive.aura.ui.state.MedicalRecordsViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.util.DateFormat

private val TYPE_COLORS = mapOf(
    "门诊" to Color(0xFF5B9FFF),
    "急诊" to Color(0xFFF56C6C),
    "住院" to Color(0xFFFF9800),
    "体检" to Color(0xFF67C23A)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordsScreen(
    memberId: Int?,
    onBack: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    viewModel: MedicalRecordsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val filteredRecords = viewModel.filteredRecords()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(memberId) { viewModel.load(memberId) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.toast) {
        state.toast?.let { snackbarHostState.showSnackbar(it); viewModel.consumeToast() }
    }

    val hasActiveFilters = state.searchText.isNotBlank() || state.filterType.isNotBlank() || state.startDate.isNotBlank() || state.endDate.isNotBlank()

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "超级病历",
                onBack = onBack,
                background = Color(0xFFF5F7FA),
                trailing = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = "新建病历", tint = AuraTokens.Primary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp)
        ) {
            // ---- Search bar ----
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF909399), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = state.searchText,
                        onValueChange = { viewModel.setSearch(it) },
                        placeholder = { Text("搜索病历（标题、症状、诊断）", fontSize = 15.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    if (state.searchText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearch("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "清除", tint = Color(0xFFC0C4CC), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Filter chips row (scrollable) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MedicalRecordTypes.forEach { (value, label) ->
                    FilterChipButton(
                        label = label,
                        selected = state.filterType == value,
                        onClick = { viewModel.setFilterType(value) }
                    )
                }

                // Date filter button
                FilterChipButton(
                    label = if (state.startDate.isNotBlank() || state.endDate.isNotBlank()) "已筛选" else "日期",
                    selected = state.startDate.isNotBlank() || state.endDate.isNotBlank(),
                    onClick = { showDatePicker = !showDatePicker },
                    icon = Icons.Default.CalendarMonth
                )

                // Clear filters
                if (hasActiveFilters) {
                    FilterChipButton(
                        label = "清空",
                        selected = false,
                        onClick = {
                            viewModel.setSearch("")
                            viewModel.setFilterType("")
                            viewModel.setDateRange("", "")
                        },
                        isClear = true
                    )
                }
            }

            // ---- Active filter result count ----
            if (hasActiveFilters) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color.White) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("找到 ", fontSize = 14.sp, color = Color(0xFF606266))
                        Text("${filteredRecords.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraTokens.Primary)
                        Text(" 条病历", fontSize = 14.sp, color = Color(0xFF606266))
                    }
                }
            }

            // ---- Date picker ----
            if (showDatePicker) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color.White) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("筛选日期范围", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { showDatePicker = false }) { Text("确定", color = AuraTokens.Primary) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("开始日期", fontSize = 12.sp, color = Color(0xFF909399))
                                Spacer(modifier = Modifier.height(4.dp))
                                var showStart by remember { mutableStateOf(false) }
                                OutlinedTextField(
                                    value = state.startDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("选择") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showStart = true },
                                    singleLine = true,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                if (showStart) {
                                    val datePickerState = rememberDatePickerState()
                                    DatePickerDialog(
                                        onDismissRequest = { showStart = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                datePickerState.selectedDateMillis?.let {
                                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                    viewModel.setDateRange(sdf.format(java.util.Date(it)), state.endDate)
                                                }
                                                showStart = false
                                            }) { Text("确定") }
                                        },
                                        dismissButton = { TextButton(onClick = { showStart = false }) { Text("取消") } }
                                    ) { DatePicker(state = datePickerState) }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("结束日期", fontSize = 12.sp, color = Color(0xFF909399))
                                Spacer(modifier = Modifier.height(4.dp))
                                var showEnd by remember { mutableStateOf(false) }
                                OutlinedTextField(
                                    value = state.endDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("选择") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showEnd = true },
                                    singleLine = true,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                if (showEnd) {
                                    val datePickerState = rememberDatePickerState()
                                    DatePickerDialog(
                                        onDismissRequest = { showEnd = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                datePickerState.selectedDateMillis?.let {
                                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                    viewModel.setDateRange(state.startDate, sdf.format(java.util.Date(it)))
                                                }
                                                showEnd = false
                                            }) { Text("确定") }
                                        },
                                        dismissButton = { TextButton(onClick = { showEnd = false }) { Text("取消") } }
                                    ) { DatePicker(state = datePickerState) }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Record list ----
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AuraTokens.Primary)
                    }
                }
                filteredRecords.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFFC0C4CC),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "暂无病历记录",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF606266)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "点击右上角添加您的第一条病历",
                                fontSize = 14.sp,
                                color = Color(0xFF909399)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onAdd,
                                colors = ButtonDefaults.buttonColors(containerColor = AuraTokens.Primary),
                                shape = RoundedCornerShape(20.dp)
                            ) { Text("新建病历") }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(filteredRecords, key = { it.id ?: it.hashCode() }) { rec ->
                            TimelineRecordCard(
                                record = rec,
                                onClick = { rec.id?.let(onOpenDetail) },
                                onEdit = { rec.id?.let(onEdit) },
                                onDelete = { rec.id?.let(viewModel::delete) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---- Timeline record card (matching iOS RecordCard) ----

@Composable
private fun TimelineRecordCard(
    record: MedicalRecord,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left: date block + connecting line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(54.dp)
        ) {
            // Date block
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AuraTokens.Primary,
                modifier = Modifier.size(54.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        timelineDateMain(record.effectiveDate),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Text(
                        timelineDateYear(record.effectiveDate),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            // Connecting line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(Color(0xFFE0E0E0))
            )
        }

        // Right: content card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.weight(1f).padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title + type badge + menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        record.title.orEmpty(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.weight(1f)
                    )
                    record.effectiveType?.takeIf { it.isNotBlank() }?.let { type ->
                        val typeColor = TYPE_COLORS[type] ?: AuraTokens.Primary
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = typeColor
                        ) {
                            Text(
                                type,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(18.dp))
                        }
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("编辑") }, onClick = { showMenu = false; onEdit() })
                        DropdownMenuItem(text = { Text("删除", color = Color(0xFFF56C6C)) }, onClick = { showMenu = false; onDelete() })
                    }
                }

                // Symptoms
                val symptoms = record.symptoms?.takeIf { it.isNotBlank() }
                if (symptoms != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(symptoms, fontSize = 14.sp, color = Color(0xFF666666), maxLines = 2)
                }

                // Hospital + Department
                val meta = listOfNotNull(
                    record.hospital?.takeIf { it.isNotBlank() },
                    record.department?.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(meta, fontSize = 12.sp, color = Color(0xFF999999))
                    }
                }
            }
        }
    }
}

// ---- Filter chip button (matching iOS MedicalRecordFilterChip) ----

@Composable
private fun FilterChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isClear: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = when {
            isClear -> Color.White
            selected -> AuraTokens.Primary
            else -> Color.White
        },
        shadowElevation = if (selected) 4.dp else 1.dp,
        border = if (!selected && !isClear) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (selected) Color.White else Color(0xFF606266))
            }
            if (isClear) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFF56C6C))
            }
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isClear -> Color(0xFFF56C6C)
                    selected -> Color.White
                    else -> Color(0xFF606266)
                }
            )
        }
    }
}

// ---- Helpers ----

private fun timelineDateMain(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "?"
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(dateStr.substringBefore("T").substringBefore(" ")) ?: return "?"
        val cal = java.util.Calendar.getInstance().apply { time = date }
        "${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
    } catch (_: Exception) { "?" }
}

private fun timelineDateYear(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(dateStr.substringBefore("T").substringBefore(" ")) ?: return ""
        val cal = java.util.Calendar.getInstance().apply { time = date }
        "${cal.get(java.util.Calendar.YEAR)}"
    } catch (_: Exception) { "" }
}
