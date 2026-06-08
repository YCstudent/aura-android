package com.edistrive.aura.ui.screens.medical

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.edistrive.aura.data.model.MedicalRecord
import com.edistrive.aura.ui.state.MedicalRecordDetailViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.util.DateFormat

// iOS exact colors
private val Teal = Color(0xFF1A8080)
private val DarkText = Color(0xFF2C3E50)
private val BodyText = Color(0xFF606266)
private val SubText = Color(0xFF909399)
private val Red = Color(0xFFFF3B30)
private val Blue = Color(0xFF409EFF)
private val Green = Color(0xFF67C23A)
private val Orange = Color(0xFFFF9500)
private val LightBlue = Color(0xFF5AC8FA)
private val PillOrange = Color(0xFFE6A23C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordDetailScreen(
    recordId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    viewModel: MedicalRecordDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(recordId) { viewModel.load(recordId) }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "病历详情",
                onBack = onBack,
                background = Color(0xFFF5F7FA),
                trailing = {
                    if (state.record != null) {
                        IconButton(onClick = { onEdit(recordId) }) {
                            Icon(Icons.Default.Edit, "编辑", tint = Teal)
                        }
                    }
                }
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        val record = state.record
        if (state.isLoading || record == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (state.errorMessage != null)
                    Text(state.errorMessage!!, color = Color(0xFFE5484D))
                else
                    CircularProgressIndicator(color = Teal)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            HeaderCard(record)

            // Symptoms
            if (!record.symptoms.isNullOrBlank()) {
                DetailCard(
                    title = "症状描述",
                    icon = Icons.Default.Healing,
                    iconTint = Teal
                ) {
                    Text(
                        record.symptoms,
                        fontSize = 15.sp,
                        color = BodyText,
                        lineHeight = 22.sp
                    )
                }
            }

            // AI Diagnosis
            if (!record.possible_diseases.isNullOrBlank() ||
                !record.suggestion_desc.isNullOrBlank() ||
                !record.suggestion_tests.isNullOrBlank() ||
                !record.treatment_rx.isNullOrBlank() ||
                !record.treatment_otc.isNullOrBlank()
            ) {
                DiagnosisCard(record)
            }

            // Hospital info
            DetailCard(
                title = "就医信息",
                icon = Icons.Default.Business,
                iconTint = LightBlue
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(
                        label = "医院",
                        value = record.hospital?.takeIf { it.isNotBlank() } ?: "未填写"
                    )
                    DetailRow(
                        label = "科室",
                        value = record.department?.takeIf { it.isNotBlank() } ?: "未填写"
                    )
                }
            }

            // Images
            val images = record.images.orEmpty()
            DetailCard(
                title = "影像资料",
                icon = Icons.Default.Photo,
                iconTint = LightBlue
            ) {
                if (images.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(images, key = { it.id ?: it.hashCode() }) { img ->
                            AsyncImage(
                                model = img.displayUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8F9FA))
                            )
                        }
                    }
                } else {
                    Text(
                        "暂无图片",
                        fontSize = 14.sp,
                        color = SubText,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    )
                }
            }

            // Notes
            if (!record.notes.isNullOrBlank()) {
                DetailCard(
                    title = "备注",
                    icon = Icons.Default.Note,
                    iconTint = SubText
                ) {
                    Text(
                        record.notes,
                        fontSize = 15.sp,
                        color = BodyText,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

// ---- Header Card ----
@Composable
private fun HeaderCard(record: MedicalRecord) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    record.title.orEmpty(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                if (!record.effectiveType.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Teal)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            record.effectiveType!!,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
            Text(
                DateFormat.chineseFromIso(record.effectiveDate).takeIf { it != "-" } ?: "",
                fontSize = 14.sp,
                color = SubText
            )
        }
    }
}

// ---- Detail Card ----
@Composable
private fun DetailCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }
            content()
        }
    }
}

// ---- Detail Row ----
@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 13.sp, color = SubText)
        Text(value, fontSize = 15.sp, color = BodyText)
    }
}

// ---- AI Diagnosis Card ----
@Composable
private fun DiagnosisCard(record: MedicalRecord) {
    val gradient = Brush.linearGradient(
        colors = listOf(Orange.copy(alpha = 0.05f), Color.White),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(12.dp))
                .border(2.dp, Orange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            // Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Description, null,
                    tint = Orange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "智能诊断报告",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.AutoAwesome, null,
                    tint = Orange,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // 1. 患病可能
                if (!record.possible_diseases.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Favorite, null,
                                tint = Red,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "患病可能",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Red
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Red.copy(alpha = 0.08f))
                                .padding(12.dp)
                        ) {
                            Text(
                                record.possible_diseases,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkText,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                // 2. 建议补充
                val hasDesc = !record.suggestion_desc.isNullOrBlank()
                val hasTests = !record.suggestion_tests.isNullOrBlank()
                if (hasDesc || hasTests) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info, null,
                                tint = Blue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "建议补充",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Blue
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Blue.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (hasDesc) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LooksOne, null,
                                            tint = Blue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "健康建议",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SubText
                                            )
                                            Text(
                                                record.suggestion_desc!!,
                                                fontSize = 15.sp,
                                                color = BodyText,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                                if (hasTests) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LooksTwo, null,
                                            tint = Blue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "推荐检查项目",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SubText
                                            )
                                            Text(
                                                record.suggestion_tests!!,
                                                fontSize = 15.sp,
                                                color = BodyText,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. 处理方法
                val hasRx = !record.treatment_rx.isNullOrBlank()
                val hasOtc = !record.treatment_otc.isNullOrBlank()
                if (hasRx || hasOtc) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Healing, null,
                                tint = Green,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "处理方法",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Green
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Green.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (hasRx) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Medication, null,
                                            tint = PillOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "处方药物",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PillOrange
                                            )
                                            Text(
                                                record.treatment_rx!!,
                                                fontSize = 15.sp,
                                                color = BodyText,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                                if (hasOtc) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Eco, null,
                                            tint = Green,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "非处方药/生活建议",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Green
                                            )
                                            Text(
                                                record.treatment_otc!!,
                                                fontSize = 15.sp,
                                                color = BodyText,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
