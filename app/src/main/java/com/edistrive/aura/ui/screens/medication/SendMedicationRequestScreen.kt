package com.edistrive.aura.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.SendMedicationRequestBody
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.state.FamilyDetailViewModel
import com.edistrive.aura.ui.state.MedicationViewModel
import com.edistrive.aura.ui.theme.AuraTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMedicationRequestScreen(
    memberId: Int,
    onBack: () -> Unit,
    onDone: () -> Unit,
    medicationViewModel: MedicationViewModel = hiltViewModel(),
    familyViewModel: FamilyDetailViewModel = hiltViewModel()
) {
    val familyState by familyViewModel.uiState.collectAsState()
    val workingState by medicationViewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("每日 1 次") }
    var times by remember { mutableStateOf(listOf("08:00")) }
    var newTime by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(memberId) { familyViewModel.load(memberId) }

    val targetUserId = familyState.member?.linked_user

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "发送用药请求",
                onBack = onBack,
                background = Color(0xFFF7F8FA),
                trailing = {
                    androidx.compose.material3.TextButton(onClick = {
                        if (name.isBlank() || dosage.isBlank()) { error = "请填名称和剂量"; return@TextButton }
                        if (targetUserId == null) { error = "该成员未关联账号，无法发送请求"; return@TextButton }
                        if (times.isEmpty()) { error = "至少一个提醒时间"; return@TextButton }
                        if (startDate.isBlank()) { error = "请填开始日期"; return@TextButton }
                        val body = SendMedicationRequestBody(
                            to_user = targetUserId,
                            family_member = memberId,
                            medication_name = name.trim(),
                            dosage = dosage.trim(),
                            frequency = frequency.trim(),
                            reminder_times = times,
                            start_date = startDate.trim(),
                            end_date = endDate.trim().ifBlank { null },
                            instructions = instructions.trim().ifBlank { null },
                            message = message.trim().ifBlank { null }
                        )
                        medicationViewModel.sendRequest(body) { ok, err ->
                            if (ok) onDone() else error = err
                        }
                    }, enabled = !workingState.isWorking) {
                        Text(
                            if (workingState.isWorking) "发送中..." else "发送",
                            color = AuraTokens.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "接收人：${familyState.member?.preferredName ?: "..."}",
                fontSize = 14.sp,
                color = AuraTokens.TextSecondary
            )
            if (targetUserId == null && familyState.member != null) {
                Text(
                    "提示：该成员尚未关联账号，请先关联后才能发送请求",
                    fontSize = 12.sp,
                    color = Color(0xFFE5484D)
                )
            }
            SectionCard("药品信息") {
                OutlinedTextField(name, { name = it }, label = { Text("名称 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dosage, { dosage = it }, label = { Text("剂量 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(frequency, { frequency = it }, label = { Text("频率") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            SectionCard("提醒时间") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    times.forEachIndexed { idx, t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(AuraTokens.Primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(t, color = AuraTokens.Primary, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = {
                                times = times.toMutableList().also { it.removeAt(idx) }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "删除", tint = Color(0xFFE5484D))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            newTime,
                            { newTime = it },
                            placeholder = { Text("HH:MM") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(onClick = {
                            if (newTime.matches(Regex("\\d{1,2}:\\d{2}"))) {
                                times = (times + newTime).distinct().sorted()
                                newTime = ""
                            } else error = "时间格式 HH:MM"
                        }, colors = ButtonDefaults.buttonColors(containerColor = AuraTokens.Primary)) {
                            Text("添加")
                        }
                    }
                }
            }
            SectionCard("时间范围") {
                com.edistrive.aura.ui.components.DateInputField(
                    label = "开始日期 *",
                    value = startDate,
                    onValueChange = { startDate = it }
                )
                com.edistrive.aura.ui.components.DateInputField(
                    label = "结束日期（可空）",
                    value = endDate,
                    onValueChange = { endDate = it }
                )
            }
            SectionCard("说明") {
                OutlinedTextField(instructions, { instructions = it }, label = { Text("用药说明") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(message, { message = it }, label = { Text("留言（对方可见）") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
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

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
