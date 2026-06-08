package com.edistrive.aura.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.edistrive.aura.ui.components.AuraLoadingView
import com.edistrive.aura.data.model.FamilyMember
import com.edistrive.aura.ui.state.FamilyDetailViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.util.DateFormat
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthReportScreen(
    memberId: Int,
    onBack: () -> Unit,
    viewModel: FamilyDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(memberId) { viewModel.load(memberId) }
    LaunchedEffect(state.member) {
        if (state.member != null) {
            delay(1500)
            ready = true
        }
    }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "健康报告",
                onBack = onBack,
                background = Color(0xFFF7F8FA)
            )
        },
        containerColor = Color(0xFFF7F8FA)
    ) { padding ->
        val member = state.member
        if (!ready || member == null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AuraLoadingView(text = "正在生成健康报告...")
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ReportHeader(member)
            BasicInfoCard(member)
            HealthMetricsCard(member)
            HealthHistoryCard(member)
            RecommendationsCard(member)
        }
    }
}

@Composable
private fun ReportHeader(member: FamilyMember) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(AuraTokens.Primary, AuraTokens.Primary2)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "${member.preferredName} 的健康报告",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "生成时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA).format(java.util.Date())}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun BasicInfoCard(member: FamilyMember) {
    SectionCard("基本信息") {
        InfoRow("姓名", member.preferredName)
        InfoRow("性别", member.gender_display ?: when (member.gender) {
            "male" -> "男"; "female" -> "女"; else -> "其他"
        })
        InfoRow("年龄", member.age?.let { "${it}岁" } ?: "-")
        InfoRow("出生日期", DateFormat.chineseFromIso(member.birth_date))
        InfoRow("电话", member.phone.orEmpty().ifBlank { "-" })
    }
}

@Composable
private fun HealthMetricsCard(member: FamilyMember) {
    val height = member.height
    val weight = member.weight
    val bmi: Double? = if (height != null && weight != null && height > 0) {
        weight / ((height / 100.0) * (height / 100.0))
    } else null
    val assessment = when {
        bmi == null -> "-"
        bmi < 18.5 -> "偏瘦"
        bmi < 24.0 -> "正常"
        bmi < 28.0 -> "偏胖"
        else -> "肥胖"
    }

    SectionCard("健康指标") {
        InfoRow("身高", height?.let { "${it.toInt()} cm" } ?: "-")
        InfoRow("体重", weight?.let { "${it.toInt()} kg" } ?: "-")
        InfoRow("BMI", bmi?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "-")
        InfoRow("BMI 评估", assessment, highlight = assessment != "-" && assessment != "正常")
        InfoRow("血型", member.blood_type?.let { "${it}型" } ?: "-")
    }
}

@Composable
private fun HealthHistoryCard(member: FamilyMember) {
    SectionCard("健康档案") {
        HistoryRow("既往病史", member.medical_history)
        HistoryRow("过敏史", member.allergy_history)
        HistoryRow("慢性疾病", member.chronic_diseases)
        HistoryRow("手术史", member.surgery_history)
        HistoryRow("长期用药", member.medication_history)
    }
}

@Composable
private fun RecommendationsCard(member: FamilyMember) {
    val recs = buildList {
        add("均衡饮食，规律作息，每日 7–8 小时睡眠")
        add("每周累计 150 分钟以上中等强度有氧运动")
        if (!member.chronic_diseases.isNullOrBlank()) add("慢性病请遵医嘱定期复查，监测相关指标")
        if (!member.allergy_history.isNullOrBlank()) add("避免接触已知过敏原，外出携带常用抗过敏药")
        if (!member.medication_history.isNullOrBlank()) add("长期用药请按时服用，定期评估剂量与副作用")
        if ((member.age ?: 0) >= 40) add("建议每年体检一次，重点关注血压/血糖/血脂")
        if ((member.age ?: 0) >= 60) add("注意预防跌倒，保持适度社交与认知活动")
    }
    SectionCard("健康建议") {
        recs.forEachIndexed { i, line ->
            Text("${i + 1}. $line", fontSize = 14.sp, color = AuraTokens.TextPrimary, modifier = Modifier.padding(vertical = 2.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "本报告基于已录入的健康档案生成，仅供参考，不能替代医生的诊断。",
            fontSize = 11.sp,
            color = AuraTokens.TextSecondary
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AuraTokens.TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AuraTokens.TextSecondary, fontSize = 13.sp)
        Text(
            value,
            color = if (highlight) Color(0xFFE6A23C) else AuraTokens.TextPrimary,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun HistoryRow(label: String, value: String?) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = AuraTokens.TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value?.takeIf { it.isNotBlank() } ?: "无", fontSize = 13.sp, color = AuraTokens.TextPrimary)
    }
}

@Suppress("unused")
private fun Modifier.unused(): Modifier = this
