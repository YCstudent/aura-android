package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.CreateAppointmentRequest
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.state.AppointmentsViewModel

// iOS exact colors
private val Teal = Color(0xFF1A8080)
private val Teal2 = Color(0xFF2D9C9C)
private val DarkText = Color(0xFF2C3E50)
private val BodyText = Color(0xFF606266)
private val SubText = Color(0xFF909399)
private val BorderColor = Color(0xFFE4E7ED)
private val BgGray = Color(0xFFF5F7FA)
private val FieldBg = Color(0xFFF8F9FA)
private val Red = Color(0xFFFF3B30)
private val Blue = Color(0xFF409EFF)
private val Orange = Color(0xFFFF9500)
private val Green = Color(0xFF67C23A)

private val APPOINTMENT_TYPES = listOf(
    "outpatient" to "门诊", "inpatient" to "住院", "checkup" to "体检",
    "reexamination" to "复诊", "surgery" to "手术", "other" to "其他"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedType by remember { mutableStateOf("outpatient") }
    var title by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var remindBefore by remember { mutableStateOf("60") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "新建预约",
                onBack = onBack,
                background = BgGray,
                trailing = {
                    TextButton(onClick = {
                        if (title.isBlank()) { error = "请输入预约标题"; return@TextButton }
                        if (hospital.isBlank()) { error = "请输入医院名称"; return@TextButton }
                        if (date.isBlank()) { error = "请选择预约日期"; return@TextButton }
                        if (time.isBlank()) { error = "请选择预约时间"; return@TextButton }
                        viewModel.createAppointment(
                            CreateAppointmentRequest(
                                appointment_type = selectedType,
                                title = title.trim(),
                                hospital = hospital.trim(),
                                department = department.trim().ifBlank { null },
                                doctor = doctor.trim().ifBlank { null },
                                appointment_date = date.trim(),
                                appointment_time = time.trim(),
                                duration = duration.toIntOrNull() ?: 30,
                                remind_before = remindBefore.toIntOrNull() ?: 60,
                                location = location.trim().ifBlank { null },
                                notes = notes.trim().ifBlank { null }
                            )
                        ) { ok, err -> if (ok) onDone() else error = err }
                    }, enabled = !state.isLoading) {
                        Text(
                            if (state.isLoading) "保存中..." else "保存",
                            color = Teal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        },
        containerColor = BgGray
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type selector
            IosSectionHeader(Icons.Default.Category, "预约类型", Teal)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var rowIndex = 0
                    while (rowIndex * 3 < APPOINTMENT_TYPES.size) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 0 until 3) {
                                val idx = rowIndex * 3 + i
                                if (idx < APPOINTMENT_TYPES.size) {
                                    val (key, label) = APPOINTMENT_TYPES[idx]
                                    val isSelected = selectedType == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) Brush.horizontalGradient(listOf(Teal, Teal2))
                                                else SolidColor(Color.White),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .border(1.dp, if (isSelected) Color.Transparent else BorderColor, RoundedCornerShape(10.dp))
                                            .clickable { selectedType = key }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            color = if (isSelected) Color.White else BodyText,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        rowIndex++
                    }
                }
            }

            // Basic Info
            IosSectionHeader(Icons.Default.Description, "基本信息", Blue)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IosInputField(title, { title = it }, "预约标题", "如：心内科复诊", required = true)
                    IosInputField(hospital, { hospital = it }, "医院名称", "如：北京协和医院", required = true)
                    IosInputField(department, { department = it }, "科室", "如：心内科")
                    IosInputField(doctor, { doctor = it }, "医生", "如：张医生")
                }
            }

            // Time arrangement
            IosSectionHeader(Icons.Default.Schedule, "时间安排", Orange)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    com.edistrive.aura.ui.components.DateInputField(
                        label = "预约日期",
                        value = date,
                        onValueChange = { date = it }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            IosInputField(time, { time = it }, "预约时间", "如：09:00")
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            val cal = java.util.Calendar.getInstance()
                            time = "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
                        }) {
                            Icon(Icons.Default.Schedule, null, tint = Teal, modifier = Modifier.size(24.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IosInputField(
                            duration, { duration = it.filter { c -> c.isDigit() }.take(3) },
                            "时长(分钟)", "30",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        IosInputField(
                            remindBefore, { remindBefore = it.filter { c -> c.isDigit() }.take(3) },
                            "提前提醒(分钟)", "60",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Other info
            IosSectionHeader(Icons.Default.MoreHoriz, "其他信息", SubText)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IosInputField(location, { location = it }, "具体位置", "如：门诊楼3层301室")
                    IosTextArea("备注", notes, "其他需要说明的信息...") { notes = it }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    if (error != null) {
        StyledInfoDialog(
            icon = Icons.Default.Warning,
            title = "提示",
            message = error.orEmpty(),
            confirmLabel = "好的",
            iconColor = Color(0xFFF56C6C),
            onDismiss = { error = null }
        )
    }
}

// ---- iOS-style Components ----

@Composable
private fun IosSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
    }
}

@Composable
private fun IosInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    required: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 13.sp, color = BodyText)
            if (required) Text("*", fontSize = 13.sp, color = Red, fontWeight = FontWeight.Bold)
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = SubText) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = DarkText),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Teal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun IosTextArea(
    label: String,
    value: String,
    placeholder: String = "",
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, color = BodyText)
        TextField(
            value = value,
            onValueChange = onValueChange,
            minLines = 2,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = SubText) },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = DarkText),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Teal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
        )
    }
}
