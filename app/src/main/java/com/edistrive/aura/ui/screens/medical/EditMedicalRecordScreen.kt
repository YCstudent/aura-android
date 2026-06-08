package com.edistrive.aura.ui.screens.medical

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.edistrive.aura.data.model.CreateMedicalRecordRequest
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.data.model.MedicalRecord
import com.edistrive.aura.data.model.StructuredMedicalInfo
import com.edistrive.aura.data.model.UpdateMedicalRecordRequest
import com.edistrive.aura.ui.state.MedicalRecordDetailViewModel
import com.edistrive.aura.ui.state.MedicalRecordsViewModel

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
private val Green = Color(0xFF67C23A)
private val Orange = Color(0xFFFF9500)

private data class RecordEditState(
    var title: String = "",
    var type: String = "门诊",
    var visitDate: String = "",
    var hospital: String = "",
    var department: String = "",
    var symptoms: String = "",
    var possibleDiseases: String = "",
    var suggestionDesc: String = "",
    var suggestionTests: String = "",
    var treatmentRx: String = "",
    var treatmentOtc: String = "",
    var notes: String = ""
)

private val TYPE_OPTIONS = listOf("门诊", "急诊", "住院", "体检")

private data class MedTemplate(
    val name: String, val emoji: String, val color: Color,
    val title: String, val type: String, val symptoms: String,
    val possibleDiseases: String, val suggestionDesc: String,
    val suggestionTests: String, val treatmentRx: String, val treatmentOtc: String
)

private val MED_TEMPLATES = listOf(
    MedTemplate("感冒发烧", "🤒", Color(0xFFFF6B6B), "感冒发烧", "门诊", "发热、鼻塞、流涕、咽痛、咳嗽、全身乏力", "上呼吸道感染（感冒）", "多休息，多喝水，注意保暖", "血常规、C反应蛋白", "布洛芬片（退烧）、阿莫西林胶囊（抗生素）", "板蓝根颗粒、维生素C、多喝温水、清淡饮食"),
    MedTemplate("肠胃炎", "🤢", Color(0xFF4ECDC4), "急性肠胃炎", "门诊", "腹痛、腹泻、恶心、呕吐、食欲不振", "急性肠胃炎", "注意饮食卫生，避免生冷食物", "大便常规、血常规", "蒙脱石散、左氧氟沙星", "益生菌、口服补液盐、清淡易消化饮食"),
    MedTemplate("头痛失眠", "😫", Color(0xFFA29BFE), "头痛失眠", "门诊", "头痛、睡眠质量差、入睡困难、多梦", "神经性头痛、失眠症", "减轻工作压力，保持规律作息", "头颅CT、脑电图", "布洛芬（镇痛）、艾司唑仑（助眠）", "谷维素、维生素B1、睡前泡脚、避免咖啡因"),
    MedTemplate("过敏鼻炎", "🤧", Color(0xFFFD79A8), "过敏性鼻炎", "门诊", "鼻痒、打喷嚏、流清涕、鼻塞", "过敏性鼻炎", "避免接触过敏原，保持室内通风", "过敏原检测、鼻镜检查", "氯雷他定片、布地奈德鼻喷雾剂", "生理盐水鼻腔冲洗、戴口罩、远离花粉"),
    MedTemplate("扭伤挫伤", "🩹", Color(0xFFFDCB6E), "软组织损伤", "急诊", "局部肿胀、疼痛、活动受限", "软组织挫伤/扭伤", "制动休息，冰敷消肿", "X光片（排除骨折）", "双氯芬酸钠缓释片、云南白药胶囊", "24小时内冰敷，48小时后热敷，避免剧烈运动"),
    MedTemplate("体检报告", "📋", Color(0xFF00B894), "年度健康体检", "体检", "无明显不适，常规体检", "健康状态良好", "保持良好生活习惯", "血常规、尿常规、肝肾功能、心电图、胸片", "无需用药", "均衡饮食、规律运动、戒烟限酒、定期体检")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicalRecordScreen(
    recordId: Int?,
    memberId: Int?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    detailViewModel: MedicalRecordDetailViewModel = hiltViewModel(),
    viewModel: MedicalRecordsViewModel = hiltViewModel()
) {
    val detailState by detailViewModel.state.collectAsState()
    val recordsState by viewModel.uiState.collectAsState()

    var form by remember { mutableStateOf(RecordEditState()) }
    var loaded by remember { mutableStateOf(recordId == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(recordId) {
        if (recordId != null) detailViewModel.load(recordId)
        if (recordId == null) viewModel.clearOcrAndStructured()
    }
    LaunchedEffect(detailState.record) {
        if (recordId != null && !loaded) {
            detailState.record?.let {
                form = it.toEditState()
                loaded = true
            }
        }
    }
    LaunchedEffect(recordsState.structured) {
        recordsState.structured?.let { applyStructured(form, it).let { f -> form = f } }
    }

    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val names = uris.mapIndexed { i, _ -> "图片${i + 1}" }
            viewModel.addOcrImages(uris, names)
        }
    }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = if (recordId == null) "新建病历" else "编辑病历",
                onBack = onBack,
                background = BgGray,
                trailing = {
                    TextButton(
                        onClick = {
                            if (form.title.isBlank()) {
                                errorMessage = "请输入标题"
                                return@TextButton
                            }
                            val uris = recordsState.ocrResults.map { it.uri }
                            if (recordId == null) {
                                viewModel.create(form.toCreate(memberId), uris) { ok, err ->
                                    if (ok) { viewModel.clearOcrAndStructured(); onDone() }
                                    else errorMessage = err
                                }
                            } else {
                                viewModel.update(recordId, form.toUpdate(), uris) { ok, err ->
                                    if (ok) { viewModel.clearOcrAndStructured(); onDone() }
                                    else errorMessage = err
                                }
                            }
                        },
                        enabled = !recordsState.isWorking
                    ) {
                        Text(
                            if (recordsState.isWorking) "保存中..." else "保存",
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
            // Templates (new records only)
            if (recordId == null) {
                IosSectionHeader(icon = Icons.Default.AutoAwesome, title = "快捷模板", tint = Orange)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(MED_TEMPLATES) { tpl ->
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable {
                                    form = form.copy(
                                        title = tpl.title, type = tpl.type,
                                        symptoms = tpl.symptoms,
                                        possibleDiseases = tpl.possibleDiseases,
                                        suggestionDesc = tpl.suggestionDesc,
                                        suggestionTests = tpl.suggestionTests,
                                        treatmentRx = tpl.treatmentRx,
                                        treatmentOtc = tpl.treatmentOtc
                                    )
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = tpl.color.copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tpl.color.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(tpl.emoji, fontSize = 24.sp)
                                Text(tpl.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = tpl.color)
                                Text("点击应用", fontSize = 11.sp, color = tpl.color.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }

            // Basic Info
            IosSectionHeader(icon = Icons.Default.Description, title = "基本信息", tint = Teal)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IosInputField(
                        value = form.title,
                        onValueChange = { form = form.copy(title = it) },
                        label = "标题",
                        placeholder = "请输入病历标题",
                        required = true
                    )
                    IosTypePills(selected = form.type, options = TYPE_OPTIONS) { form = form.copy(type = it) }
                    IosDateField(
                        value = form.visitDate,
                        label = "就诊日期",
                        placeholder = "YYYY-MM-DD",
                        onClick = { showDatePicker = true }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IosInputField(
                            value = form.hospital,
                            onValueChange = { form = form.copy(hospital = it) },
                            label = "医院",
                            placeholder = "医院名称",
                            modifier = Modifier.weight(1f)
                        )
                        IosInputField(
                            value = form.department,
                            onValueChange = { form = form.copy(department = it) },
                            label = "科室",
                            placeholder = "科室名称",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Symptoms & Images
            IosSectionHeader(icon = Icons.Default.Healing, title = "症状与图片", tint = Teal)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IosTextArea(
                        value = form.symptoms,
                        onValueChange = { form = form.copy(symptoms = it) },
                        label = "症状描述",
                        placeholder = "请详细描述症状...",
                        minLines = 3
                    )
                    ImageThumbRow(
                        items = recordsState.ocrResults,
                        onAdd = { pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onRemove = { uri -> viewModel.removeOcrItem(uri) }
                    )
                    Button(
                        onClick = { viewModel.aiAnalyze(form.symptoms) { } },
                        enabled = !recordsState.aiAnalyzing && (form.symptoms.isNotBlank() || recordsState.ocrResults.isNotEmpty()),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Teal,
                            disabledContainerColor = Teal.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (recordsState.aiAnalyzing) "AI 分析中..." else "AI 智能分析",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // AI Diagnosis
            IosSectionHeader(icon = Icons.Default.AutoAwesome, title = "诊断与建议（AI 填充）", tint = Orange)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IosTextArea("患病可能", form.possibleDiseases) { form = form.copy(possibleDiseases = it) }
                    IosTextArea("健康建议", form.suggestionDesc) { form = form.copy(suggestionDesc = it) }
                    IosTextArea("推荐检查", form.suggestionTests) { form = form.copy(suggestionTests = it) }
                    IosTextArea("处方建议", form.treatmentRx) { form = form.copy(treatmentRx = it) }
                    IosTextArea("非处方/生活方式", form.treatmentOtc) { form = form.copy(treatmentOtc = it) }
                }
            }

            // Notes
            IosSectionHeader(icon = Icons.Default.Note, title = "备注", tint = SubText)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    IosTextArea("备注信息", form.notes) { form = form.copy(notes = it) }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        form = form.copy(visitDate = sdf.format(java.util.Date(it)))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (errorMessage != null) {
        StyledInfoDialog(
            icon = Icons.Default.Warning,
            title = "提示",
            message = errorMessage.orEmpty(),
            confirmLabel = "好的",
            iconColor = Color(0xFFF56C6C),
            onDismiss = { errorMessage = null }
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
private fun IosDateField(
    value: String,
    label: String,
    placeholder: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, color = BodyText)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(FieldBg)
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (value.isBlank()) placeholder else value,
                    fontSize = 14.sp,
                    color = if (value.isBlank()) SubText else DarkText
                )
                Icon(Icons.Default.CalendarMonth, null, tint = SubText, modifier = Modifier.size(18.dp))
            }
        }
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
private fun IosTypePills(selected: String, options: List<String>, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("类型", fontSize = 13.sp, color = BodyText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                val sel = selected == opt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Teal else Color.White)
                        .border(1.dp, if (sel) Color.Transparent else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { onSelected(opt) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        opt,
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (sel) Color.White else BodyText
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageThumbRow(
    items: List<com.edistrive.aura.ui.state.OcrItemState>,
    onAdd: () -> Unit,
    onRemove: (Uri) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(items, key = { it.uri.toString() }) { item ->
            Box(modifier = Modifier.size(90.dp)) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FieldBg)
                )
                if (item.isRecognizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(22.dp),
                        strokeWidth = 2.dp,
                        color = Teal
                    )
                }
                IconButton(
                    onClick = { onRemove(item.uri) },
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close, "移除",
                        tint = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .padding(2.dp)
                            .size(12.dp)
                    )
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, Teal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .background(Teal.copy(alpha = 0.04f))
                    .clickable { onAdd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "添加图片", tint = Teal, modifier = Modifier.size(28.dp))
            }
        }
    }
}

private fun RecordEditState.toCreate(memberId: Int?) = CreateMedicalRecordRequest(
    title = title.trim(),
    type = type,
    visit_date = visitDate.trim().ifBlank { null },
    hospital = hospital.trim().ifBlank { null },
    department = department.trim().ifBlank { null },
    symptoms = symptoms.trim().ifBlank { null },
    possible_diseases = possibleDiseases.trim().ifBlank { null },
    suggestion_desc = suggestionDesc.trim().ifBlank { null },
    suggestion_tests = suggestionTests.trim().ifBlank { null },
    treatment_rx = treatmentRx.trim().ifBlank { null },
    treatment_otc = treatmentOtc.trim().ifBlank { null },
    notes = notes.trim().ifBlank { null },
    family_member = memberId
)

private fun RecordEditState.toUpdate() = UpdateMedicalRecordRequest(
    title = title.trim().ifBlank { null },
    type = type,
    visit_date = visitDate.trim().ifBlank { null },
    hospital = hospital.trim().ifBlank { null },
    department = department.trim().ifBlank { null },
    symptoms = symptoms.trim().ifBlank { null },
    possible_diseases = possibleDiseases.trim().ifBlank { null },
    suggestion_desc = suggestionDesc.trim().ifBlank { null },
    suggestion_tests = suggestionTests.trim().ifBlank { null },
    treatment_rx = treatmentRx.trim().ifBlank { null },
    treatment_otc = treatmentOtc.trim().ifBlank { null },
    notes = notes.trim().ifBlank { null }
)

private fun MedicalRecord.toEditState() = RecordEditState(
    title = title.orEmpty(),
    type = effectiveType ?: "门诊",
    visitDate = effectiveDate.orEmpty(),
    hospital = hospital.orEmpty(),
    department = department.orEmpty(),
    symptoms = symptoms.orEmpty(),
    possibleDiseases = possible_diseases.orEmpty(),
    suggestionDesc = suggestion_desc.orEmpty(),
    suggestionTests = suggestion_tests.orEmpty(),
    treatmentRx = treatment_rx.orEmpty(),
    treatmentOtc = treatment_otc.orEmpty(),
    notes = notes.orEmpty()
)

private fun applyStructured(form: RecordEditState, s: StructuredMedicalInfo): RecordEditState {
    fun fill(current: String, incoming: String?) =
        if (current.isBlank() && !incoming.isNullOrBlank()) incoming else current
    return form.copy(
        title = fill(form.title, s.title),
        hospital = fill(form.hospital, s.hospital),
        department = fill(form.department, s.department),
        symptoms = fill(form.symptoms, s.symptoms),
        possibleDiseases = fill(form.possibleDiseases, s.possible_diseases),
        suggestionDesc = fill(form.suggestionDesc, s.suggestion_desc),
        suggestionTests = fill(form.suggestionTests, s.suggestion_tests),
        treatmentRx = fill(form.treatmentRx, s.treatment_rx),
        treatmentOtc = fill(form.treatmentOtc, s.treatment_otc)
    )
}
