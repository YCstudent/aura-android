package com.edistrive.aura.ui.screens.medication

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.CreateMedicationRequest
import com.edistrive.aura.data.model.Medication
import com.edistrive.aura.data.model.UpdateMedicationRequest
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.state.MedicationDetailViewModel
import com.edistrive.aura.ui.state.MedicationViewModel

// iOS exact colors
private val Teal = Color(0xFF1A8080)
private val DarkText = Color(0xFF2C3E50)
private val BodyText = Color(0xFF606266)
private val SubText = Color(0xFF909399)
private val BorderColor = Color(0xFFE4E7ED)
private val BgGray = Color(0xFFF5F7FA)
private val FieldBg = Color(0xFFF8F9FA)
private val Red = Color(0xFFFF3B30)
private val Blue = Color(0xFF409EFF)
private val Green = Color(0xFF67C23A)
private val Orange = Color(0xFFFF9500)

private data class MedicationForm(
    var name: String = "",
    var dosage: String = "",
    var frequency: String = "每日 1 次",
    var reminderTimes: List<String> = listOf("08:00"),
    var startDate: String = "",
    var endDate: String = "",
    var notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicationScreen(
    medicationId: Int?,
    memberId: Int?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel(),
    detailViewModel: MedicationDetailViewModel = hiltViewModel()
) {
    val detailState by detailViewModel.state.collectAsState()
    val workingState by viewModel.uiState.collectAsState()
    var form by remember { mutableStateOf(MedicationForm()) }
    var loaded by remember { mutableStateOf(medicationId == null) }
    var error by remember { mutableStateOf<String?>(null) }

    var timeEditorIndex by remember { mutableStateOf<Int?>(null) }
    var timeEditorBuffer by remember { mutableStateOf("08:00") }

    LaunchedEffect(medicationId) { medicationId?.let { detailViewModel.load(it) } }
    LaunchedEffect(detailState.medication) {
        if (medicationId != null && !loaded) {
            detailState.medication?.let {
                form = it.toForm()
                loaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = if (medicationId == null) "新建用药" else "编辑用药",
                onBack = onBack,
                background = BgGray,
                trailing = {
                    TextButton(onClick = {
                        if (form.name.isBlank() || form.dosage.isBlank()) {
                            error = "请填写名称和剂量"
                            return@TextButton
                        }
                        if (form.startDate.isBlank()) {
                            error = "请选择开始日期"
                            return@TextButton
                        }
                        if (form.reminderTimes.isEmpty()) {
                            error = "至少需要一个提醒时间"
                            return@TextButton
                        }
                        if (medicationId == null) {
                            viewModel.create(form.toCreate(memberId)) { ok, err ->
                                if (ok) onDone() else error = err
                            }
                        } else {
                            viewModel.update(medicationId, form.toUpdate()) { ok, err ->
                                if (ok) onDone() else error = err
                            }
                        }
                    }, enabled = !workingState.isWorking) {
                        Text(
                            if (workingState.isWorking) "保存中..." else "保存",
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
            // Basic Info
            IosSectionHeader(Icons.Default.Medication, "基本信息", Teal)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IosInputField(form.name, { form = form.copy(name = it) }, "药品名称", "如：阿莫西林胶囊", required = true)
                    IosInputField(form.dosage, { form = form.copy(dosage = it) }, "剂量", "如：0.3g / 每次 1 粒", required = true)
                    IosInputField(form.frequency, { form = form.copy(frequency = it) }, "用药频率", "如：每日 3 次")
                }
            }

            // Reminder Times
            IosSectionHeader(Icons.Default.Schedule, "提醒时间", Blue)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("设置用药提醒时间", fontSize = 13.sp, color = SubText)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        form.reminderTimes.forEachIndexed { idx, time ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Blue.copy(alpha = 0.08f))
                                    .border(1.dp, Blue.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                    .clickable {
                                        timeEditorBuffer = time
                                        timeEditorIndex = idx
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Schedule, null, tint = Blue, modifier = Modifier.size(14.dp))
                                Text(time, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Blue)
                                Icon(
                                    Icons.Default.Close, null,
                                    tint = Red,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            form = form.copy(reminderTimes = form.reminderTimes.toMutableList().also { it.removeAt(idx) })
                                        }
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            timeEditorBuffer = "08:00"
                            timeEditorIndex = form.reminderTimes.size
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Teal.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加提醒时间", fontSize = 13.sp)
                    }
                }
            }

            // Date Range
            IosSectionHeader(Icons.Default.DateRange, "用药日期", Green)
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
                        label = "开始日期",
                        value = form.startDate,
                        onValueChange = { form = form.copy(startDate = it) }
                    )
                    com.edistrive.aura.ui.components.DateInputField(
                        label = "结束日期（可留空，表示长期服用）",
                        value = form.endDate,
                        onValueChange = { form = form.copy(endDate = it) }
                    )
                }
            }

            // Notes
            IosSectionHeader(Icons.Default.Note, "备注", SubText)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    IosTextArea("备注信息", form.notes, placeholder = "其他需要说明的信息...") { form = form.copy(notes = it) }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    timeEditorIndex?.let { idx ->
        TimePickerDialog(
            initial = timeEditorBuffer,
            onDismiss = { timeEditorIndex = null },
            onConfirm = { value ->
                val list = form.reminderTimes.toMutableList()
                if (idx < list.size) list[idx] = value else list.add(value)
                form = form.copy(reminderTimes = list.distinct().sorted())
                timeEditorIndex = null
            }
        )
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
    required: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 13.sp, color = BodyText)
            if (required) Text("*", fontSize = 13.sp, color = Red, fontWeight = FontWeight.Bold)
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
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

@Composable
private fun IosTextArea(
    label: String,
    value: String,
    placeholder: String = "",
    minLines: Int = 2,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, color = BodyText)
        TextField(
            value = value,
            onValueChange = onValueChange,
            minLines = minLines,
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

@Composable
private fun TimePickerDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var hh by remember { mutableStateOf(initial.substringBefore(":", "08")) }
    var mm by remember { mutableStateOf(initial.substringAfter(":", "00")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val h = hh.toIntOrNull()?.coerceIn(0, 23) ?: 8
                val m = mm.toIntOrNull()?.coerceIn(0, 59) ?: 0
                onConfirm("%02d:%02d".format(h, m))
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("设置提醒时间") },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = hh,
                    onValueChange = { hh = it.filter { c -> c.isDigit() }.take(2) },
                    modifier = Modifier.width(80.dp),
                    label = { Text("时") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text("  :  ", fontSize = 20.sp, color = DarkText)
                OutlinedTextField(
                    value = mm,
                    onValueChange = { mm = it.filter { c -> c.isDigit() }.take(2) },
                    modifier = Modifier.width(80.dp),
                    label = { Text("分") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        containerColor = Color.White
    )
}

private fun MedicationForm.toCreate(memberId: Int?) = CreateMedicationRequest(
    name = name.trim(),
    dosage = dosage.trim().ifBlank { null },
    frequency = frequency.trim().ifBlank { null },
    reminder_times = reminderTimes,
    start_date = startDate.trim().ifBlank { null },
    end_date = endDate.trim().ifBlank { null },
    notes = notes.trim().ifBlank { null },
    family_member = memberId
)

private fun MedicationForm.toUpdate() = UpdateMedicationRequest(
    name = name.trim().ifBlank { null },
    dosage = dosage.trim().ifBlank { null },
    frequency = frequency.trim().ifBlank { null },
    reminder_times = reminderTimes,
    start_date = startDate.trim().ifBlank { null },
    end_date = endDate.trim().ifBlank { null },
    notes = notes.trim().ifBlank { null }
)

private fun Medication.toForm() = MedicationForm(
    name = name.orEmpty(),
    dosage = dosage.orEmpty(),
    frequency = frequency ?: "每日 1 次",
    reminderTimes = reminder_times.orEmpty().ifEmpty { listOf("08:00") },
    startDate = start_date.orEmpty(),
    endDate = end_date.orEmpty(),
    notes = notes.orEmpty()
)
