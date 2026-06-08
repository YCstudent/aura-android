package com.edistrive.aura.ui.screens.medical

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.MedicalRecord
import com.edistrive.aura.ui.state.MedicalRecordsViewModel
import com.edistrive.aura.ui.theme.AuraTokens

private val TYPE_COLORS = mapOf(
    "门诊" to Color(0xFF5B9FFF),
    "急诊" to Color(0xFFF56C6C),
    "住院" to Color(0xFFFF9800),
    "体检" to Color(0xFF67C23A)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberMedicalRecordsScreen(
    memberId: Int,
    onBack: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    viewModel: MedicalRecordsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(memberId) { viewModel.load(memberId) }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "健康病历",
                onBack = onBack,
                background = Color(0xFFF5F7FA)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AuraTokens.Primary
                    )
                }
                state.records.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            tint = Color(0xFFC0C4CC),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暂无病历记录", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF606266))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("该成员暂无病历", fontSize = 14.sp, color = Color(0xFF909399))
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.records, key = { it.id ?: it.hashCode() }) { rec ->
                            MemberTimelineCard(rec, onClick = { rec.id?.let(onOpenDetail) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberTimelineCard(record: MedicalRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date block
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(54.dp)
        ) {
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
            Box(modifier = Modifier.width(2.dp).height(20.dp).background(Color(0xFFE0E0E0)))
        }

        // Content
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.weight(1f).padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                        Surface(shape = RoundedCornerShape(10.dp), color = typeColor) {
                            Text(type, fontSize = 11.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
                val symptoms = record.symptoms?.takeIf { it.isNotBlank() }
                if (symptoms != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(symptoms, fontSize = 14.sp, color = Color(0xFF666666), maxLines = 2)
                }
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
