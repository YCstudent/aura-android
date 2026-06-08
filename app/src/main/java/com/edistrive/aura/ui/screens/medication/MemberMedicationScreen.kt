package com.edistrive.aura.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.Medication
import com.edistrive.aura.ui.components.AuraExtraColors
import com.edistrive.aura.ui.screens.medication.components.MedicationRequestCard
import com.edistrive.aura.ui.state.FamilyDetailViewModel
import com.edistrive.aura.ui.state.MedicationViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.util.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberMedicationScreen(
    memberId: Int,
    onBack: () -> Unit,
    onSendRequest: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel(),
    detailViewModel: FamilyDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val detailState by detailViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(memberId) {
        viewModel.loadAll(memberId)
        detailViewModel.load(memberId)
    }

    val memberName = detailState.member?.preferredName ?: "成员"
    val memberLinkedUserId = detailState.member?.linked_user

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "${memberName}的用药",
                onBack = onBack,
                background = AuraExtraColors.GrayBg,
                trailing = {
                    if (memberLinkedUserId != null) {
                        androidx.compose.material3.IconButton(onClick = onSendRequest) {
                            Icon(Icons.Default.Send, contentDescription = "发送请求", tint = AuraTokens.Primary)
                        }
                    }
                }
            )
        },
        containerColor = AuraExtraColors.GrayBg
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            // Read-only banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuraTokens.Primary.copy(alpha = 0.08f))
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AuraTokens.Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (memberLinkedUserId != null)
                        "查看 ${memberName} 的用药情况，可发送用药请求"
                    else
                        "${memberName} 尚未关联账号，仅可查看",
                    color = AuraTokens.Primary,
                    fontSize = 13.sp
                )
            }

            Surface(color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                ) {
                    SubTab("当前用药", selectedTab == 0, { selectedTab = 0 }, Modifier.weight(1f))
                    SubTab("已结束 (${state.expiredMedications.size})", selectedTab == 1, { selectedTab = 1 }, Modifier.weight(1f))
                }
            }

            when (selectedTab) {
                0 -> ReadonlyActiveTab(state.todayMedications, state.activeMedications)
                1 -> ReadonlyExpiredTab(state.expiredMedications)
            }
        }
    }
}

@Composable
private fun SubTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (selected) AuraTokens.Primary else AuraExtraColors.GrayLight
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(label, color = color, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) AuraTokens.Primary else Color.Transparent)
        )
    }
}

@Composable
private fun ReadonlyActiveTab(today: List<Medication>, all: List<Medication>) {
    if (all.isEmpty() && today.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Medication, contentDescription = null, tint = AuraTokens.Primary.copy(alpha = 0.3f), modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("成员暂无用药", color = AuraExtraColors.GrayLight)
            }
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (today.isNotEmpty()) {
            item { SectionTitle("今日用药", Icons.Default.Schedule) }
            items(today, key = { "today_${it.id}" }) { ReadonlyMedicationCard(it, today = true) }
            item { Spacer(modifier = Modifier.height(4.dp)) }
        }
        item { SectionTitle("全部提醒", Icons.Default.Medication) }
        items(all, key = { it.id ?: it.hashCode() }) { ReadonlyMedicationCard(it, today = false) }
    }
}

@Composable
private fun ReadonlyExpiredTab(list: List<Medication>) {
    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无已结束的用药", color = AuraExtraColors.GrayLight)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(list, key = { it.id ?: it.hashCode() }) { med ->
            Box(modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(14.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.03f))
                .clip(RoundedCornerShape(14.dp))
                .background(AuraExtraColors.GrayBgLightest)
                .border(1.dp, AuraExtraColors.GrayLightest, RoundedCornerShape(14.dp))
                .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AuraExtraColors.GrayBgLighter),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Medication, contentDescription = null, tint = AuraExtraColors.GrayLight, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(med.name.orEmpty(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraExtraColors.GrayText, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AuraExtraColors.GrayBgLighter)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("已结束", fontSize = 11.sp, color = AuraExtraColors.GrayLight)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AuraExtraColors.GrayLight, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${DateFormat.chineseFromIso(med.start_date)} - ${DateFormat.chineseFromIso(med.end_date)}",
                            fontSize = 13.sp,
                            color = AuraExtraColors.GrayLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AuraTokens.Primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = AuraTokens.Primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReadonlyMedicationCard(med: Medication, today: Boolean) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .shadow(10.dp, RoundedCornerShape(14.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.05f))
        .clip(RoundedCornerShape(14.dp))
        .background(Color.White)
        .let {
            if (today) it.border(
                1.5.dp,
                Brush.horizontalGradient(listOf(AuraTokens.Primary.copy(alpha = 0.3f), AuraTokens.Primary2.copy(alpha = 0.3f))),
                RoundedCornerShape(14.dp)
            ) else it
        }
        .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(if (today) 48.dp else 40.dp)
                        .clip(CircleShape)
                        .background(
                            if (today) Brush.linearGradient(listOf(AuraTokens.Primary, AuraTokens.Primary2))
                            else Brush.linearGradient(
                                listOf(
                                    AuraTokens.Primary.copy(alpha = 0.2f),
                                    AuraTokens.Primary2.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = if (today) Color.White else AuraTokens.Primary, modifier = Modifier.size(if (today) 22.dp else 18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(med.name.orEmpty(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        listOfNotNull(med.dosage, med.frequency).joinToString(" · "),
                        fontSize = 13.sp,
                        color = AuraExtraColors.GrayText
                    )
                }
            }
            val times = med.reminder_times.orEmpty()
            if (times.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(times) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AuraTokens.Primary.copy(alpha = 0.08f))
                                .border(1.dp, AuraTokens.Primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(it, color = AuraTokens.Primary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
