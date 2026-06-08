package com.edistrive.aura.ui.screens.family

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.CreateFamilyMemberRequest
import com.edistrive.aura.data.model.FamilyMember
import com.edistrive.aura.data.model.UpdateFamilyMemberRequest
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.state.FamilyDetailViewModel
import com.edistrive.aura.ui.state.FamilyViewModel

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

private data class EditState(
    var name: String = "",
    var relation: String = "other",
    var gender: String = "male",
    var birthDate: String = "",
    var phone: String = "",
    var height: String = "",
    var weight: String = "",
    var bloodType: String = "",
    var allergyHistory: String = "",
    var medicalHistory: String = "",
    var surgeryHistory: String = "",
    var medicationHistory: String = "",
    var chronicDiseases: String = "",
    var notes: String = ""
)

private val RELATIONS = listOf(
    "self" to "本人", "parent" to "父母", "spouse" to "配偶",
    "child" to "子女", "sibling" to "兄弟姐妹",
    "grandparent" to "祖父母", "grandchild" to "孙辈", "other" to "其他"
)
private val GENDERS = listOf("male" to "男", "female" to "女", "other" to "其他")
private val BLOOD_TYPES = listOf("" to "未选择", "A" to "A型", "B" to "B型", "AB" to "AB型", "O" to "O型")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFamilyMemberScreen(
    memberId: Int?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    familyViewModel: FamilyViewModel = hiltViewModel(),
    detailViewModel: FamilyDetailViewModel = hiltViewModel()
) {
    val detailState by detailViewModel.uiState.collectAsState()
    var state by remember { mutableStateOf(EditState()) }
    var loaded by remember { mutableStateOf(memberId == null) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(memberId) { if (memberId != null) detailViewModel.load(memberId) }
    LaunchedEffect(detailState.member) {
        if (memberId != null && !loaded) {
            detailState.member?.let { m -> state = m.toEditState(); loaded = true }
        }
    }

    val focus = LocalFocusManager.current

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = if (memberId == null) "添加成员" else "编辑成员",
                onBack = onBack,
                background = BgGray,
                trailing = {
                    TextButton(
                        onClick = {
                            focus.clearFocus()
                            val validation = state.validateForCreate(creating = memberId == null)
                            if (validation != null) { errorMessage = validation; return@TextButton }
                            saving = true
                            if (memberId == null) {
                                familyViewModel.createMember(state.toCreate()) { ok, err ->
                                    saving = false
                                    if (ok) onDone() else errorMessage = err
                                }
                            } else {
                                familyViewModel.updateMember(memberId, state.toUpdate()) { ok, err ->
                                    saving = false
                                    if (ok) onDone() else errorMessage = err
                                }
                            }
                        },
                        enabled = !saving
                    ) {
                        Text(
                            if (saving) "保存中..." else "保存",
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
            IosSectionHeader(Icons.Default.Person, "基本信息", Teal)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IosInputField(state.name, { state = state.copy(name = it) }, "姓名", "请输入姓名", required = true)
                    IosPillGroup("关系", RELATIONS, state.relation, required = true) { state = state.copy(relation = it) }
                    IosPillGroup("性别", GENDERS, state.gender, required = true) { state = state.copy(gender = it) }
                    com.edistrive.aura.ui.components.DateInputField(
                        label = "出生日期",
                        value = state.birthDate,
                        onValueChange = { state = state.copy(birthDate = it) }
                    )
                    IosInputField(
                        state.phone, { state = state.copy(phone = it) },
                        "电话", "请输入手机号",
                        keyboardType = KeyboardType.Phone
                    )
                }
            }

            // Body Metrics
            IosSectionHeader(Icons.Default.MonitorWeight, "身体指标", Blue)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IosInputField(
                            state.height, { state = state.copy(height = it.filter { ch -> ch.isDigit() || ch == '.' }) },
                            "身高 (cm)", "170",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        IosInputField(
                            state.weight, { state = state.copy(weight = it.filter { ch -> ch.isDigit() || ch == '.' }) },
                            "体重 (kg)", "65",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    IosPillGroup("血型", BLOOD_TYPES, state.bloodType) { state = state.copy(bloodType = it) }
                }
            }

            // Health Archive
            IosSectionHeader(Icons.Default.LocalHospital, "健康档案", Green)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IosTextArea("既往病史", state.medicalHistory, "若无请填\"无\"") { state = state.copy(medicalHistory = it) }
                    IosTextArea("过敏史", state.allergyHistory, "若无请填\"无\"") { state = state.copy(allergyHistory = it) }
                    IosTextArea("手术史", state.surgeryHistory, "若无请填\"无\"") { state = state.copy(surgeryHistory = it) }
                    IosTextArea("长期用药", state.medicationHistory, "若无请填\"无\"") { state = state.copy(medicationHistory = it) }
                    IosTextArea("慢性疾病", state.chronicDiseases, "若无请填\"无\"") { state = state.copy(chronicDiseases = it) }
                    IosTextArea("备注", state.notes, "其他需要说明的信息") { state = state.copy(notes = it) }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
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

@Composable
private fun IosPillGroup(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    required: Boolean = false,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 13.sp, color = BodyText)
            if (required) Text("*", fontSize = 13.sp, color = Red, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, display) ->
                val sel = value == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Teal else Color.White)
                        .border(1.dp, if (sel) Color.Transparent else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { onSelected(value) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        display,
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (sel) Color.White else BodyText
                    )
                }
            }
        }
    }
}

private fun EditState.toCreate() = CreateFamilyMemberRequest(
    name = name.trim(),
    relation = relation,
    gender = gender,
    birth_date = birthDate.trim().ifBlank { null },
    phone = phone.trim().ifBlank { null },
    height = height.toDoubleOrNull(),
    weight = weight.toDoubleOrNull(),
    blood_type = bloodType.ifBlank { null },
    allergy_history = allergyHistory.trim().ifBlank { null },
    medical_history = medicalHistory.trim().ifBlank { null },
    surgery_history = surgeryHistory.trim().ifBlank { null },
    medication_history = medicationHistory.trim().ifBlank { null },
    chronic_diseases = chronicDiseases.trim().ifBlank { null },
    notes = notes.trim().ifBlank { null }
)

private fun EditState.toUpdate() = UpdateFamilyMemberRequest(
    name = name.trim().ifBlank { null },
    relation = relation,
    gender = gender,
    birth_date = birthDate.trim().ifBlank { null },
    phone = phone.trim().ifBlank { null },
    height = height.toDoubleOrNull(),
    weight = weight.toDoubleOrNull(),
    blood_type = bloodType.ifBlank { null },
    allergy_history = allergyHistory.trim().ifBlank { null },
    medical_history = medicalHistory.trim().ifBlank { null },
    surgery_history = surgeryHistory.trim().ifBlank { null },
    medication_history = medicationHistory.trim().ifBlank { null },
    chronic_diseases = chronicDiseases.trim().ifBlank { null },
    notes = notes.trim().ifBlank { null }
)

private fun EditState.validateForCreate(creating: Boolean): String? {
    if (creating && name.isBlank()) return "请输入姓名"
    if (creating && relation.isBlank()) return "请选择关系"
    if (creating && gender.isBlank()) return "请选择性别"
    if (birthDate.isNotBlank() && !birthDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
        return "出生日期格式需为 YYYY-MM-DD"
    }
    return null
}

private fun FamilyMember.toEditState() = EditState(
    name = name.orEmpty(),
    relation = relation ?: "other",
    gender = gender ?: "male",
    birthDate = birth_date.orEmpty(),
    phone = phone.orEmpty(),
    height = height?.toString().orEmpty(),
    weight = weight?.toString().orEmpty(),
    bloodType = blood_type.orEmpty(),
    allergyHistory = allergy_history.orEmpty(),
    medicalHistory = medical_history.orEmpty(),
    surgeryHistory = surgery_history.orEmpty(),
    medicationHistory = medication_history.orEmpty(),
    chronicDiseases = chronic_diseases.orEmpty(),
    notes = notes.orEmpty()
)
