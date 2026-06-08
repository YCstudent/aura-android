package com.edistrive.aura.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.Appointment
import com.edistrive.aura.ui.state.AppointmentsViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.ui.components.AuraLoadingView
import com.edistrive.aura.ui.components.IosTopBar
import com.edistrive.aura.ui.components.StyledConfirmDialog

@Composable
fun AppointmentsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit = {},
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf<Appointment?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAppointments()
        viewModel.loadStatistics()
    }

    LaunchedEffect(state.toast) {
        if (state.toast != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.consumeToast()
        }
    }

    val tabs = listOf("待就诊", "已完成", "已取消")
    val filteredAppointments = when (state.selectedTab) {
        0 -> state.appointments.filter { it.status == "pending" }
        1 -> state.appointments.filter { it.status == "completed" }
        2 -> state.appointments.filter { it.status == "cancelled" }
        else -> emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        Column(modifier = Modifier.fillMaxSize()) {
            IosTopBar(
                title = "预约管理",
                onBack = onBack,
                background = Color(0xFFF5F7FA),
                trailing = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = "添加预约", tint = AuraTokens.Primary)
                    }
                }
            )

            // Tab selector
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = Color.White,
                contentColor = AuraTokens.Primary,
                modifier = Modifier.shadow(2.dp, ambientColor = Color.Black.copy(alpha = 0.05f))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        text = {
                            Text(
                                title,
                                fontWeight = if (state.selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        AuraLoadingView(text = "加载中...")
                    }
                    filteredAppointments.isEmpty() -> {
                        EmptyAppointmentView(
                            tab = state.selectedTab,
                            onAdd = onAdd
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Statistics cards on all tabs
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatCard(
                                        icon = Icons.Default.Pending,
                                        value = "${state.statistics.upcoming ?: 0}",
                                        label = "待就诊",
                                        gradientStart = Color(0xFFFF9500),
                                        gradientEnd = Color(0xFFFF6B00),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        icon = Icons.Default.CalendarMonth,
                                        value = "${state.statistics.today ?: 0}",
                                        label = "今日",
                                        gradientStart = Color(0xFF1A8080),
                                        gradientEnd = Color(0xFF2D9C9C),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        icon = Icons.Default.CheckCircle,
                                        value = "${state.statistics.completed ?: 0}",
                                        label = "已完成",
                                        gradientStart = Color(0xFF52C41A),
                                        gradientEnd = Color(0xFF73D13D),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(4.dp)) }

                            items(filteredAppointments, key = { it.id ?: it.hashCode() }) { appointment ->
                                AppointmentCard(
                                    appointment = appointment,
                                    onComplete = { viewModel.completeAppointment(appointment) },
                                    onCancel = { viewModel.cancelAppointment(appointment) },
                                    onDelete = { showDeleteConfirm = appointment }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }

        // Toast overlay
        AnimatedVisibility(
            visible = state.toast != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (state.toastIsError) Color(0xFFE5484D) else Color(0xFF2BB0A8))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (state.toastIsError) Icons.Default.Close else Icons.Default.CheckCircle,
                    null, tint = Color.White, modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(state.toast.orEmpty(), color = Color.White, fontSize = 14.sp)
            }
        }
    }

    // Delete confirmation
    showDeleteConfirm?.let { appt ->
        StyledConfirmDialog(
            icon = Icons.Filled.Delete,
            title = "删除预约",
            message = "确定要删除「${appt.title ?: ""}」吗？",
            confirmLabel = "删除",
            onConfirm = {
                viewModel.deleteAppointment(appt)
                showDeleteConfirm = null
            },
            onDismiss = { showDeleteConfirm = null }
        )
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(gradientStart.copy(alpha = 0.15f), gradientEnd.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint = gradientStart,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFF303133))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF909399))
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val typeGradient = when (appointment.appointment_type) {
        "outpatient" -> listOf(Color(0xFF1A8080), Color(0xFF2D9C9C))
        "inpatient" -> listOf(Color(0xFF722ED1), Color(0xFF9254DE))
        "checkup" -> listOf(Color(0xFF52C41A), Color(0xFF73D13D))
        "reexamination" -> listOf(Color(0xFFFF9500), Color(0xFFFF6B00))
        "surgery" -> listOf(Color(0xFFF5222D), Color(0xFFFF4D4F))
        else -> listOf(Color(0xFF909399), Color(0xFFC0C4CC))
    }

    val typeLabel = when (appointment.appointment_type) {
        "outpatient" -> "门诊"
        "inpatient" -> "住院"
        "checkup" -> "体检"
        "reexamination" -> "复诊"
        "surgery" -> "手术"
        else -> "其他"
    }

    val typeIcon = when (appointment.appointment_type) {
        "outpatient" -> Icons.Default.LocalHospital
        "inpatient" -> Icons.Default.Hotel
        "checkup" -> Icons.Default.Description
        "reexamination" -> Icons.Default.Refresh
        "surgery" -> Icons.Default.Healing
        else -> Icons.Default.CalendarMonth
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top: icon, type badge, title, date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(typeGradient[0].copy(alpha = 0.15f), typeGradient[1].copy(alpha = 0.1f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        typeIcon, null,
                        tint = typeGradient[0],
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Brush.linearGradient(typeGradient, Offset.Zero, Offset.Infinite).let { _ ->
                                // Use a SolidColor fallback - Surface doesn't accept Brush directly
                                typeGradient[0].copy(alpha = 0.12f)
                            }
                        ) {
                            Text(
                                typeLabel,
                                color = typeGradient[0],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        appointment.title ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF303133),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Date box
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatDay(appointment.appointment_date),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF303133)
                    )
                    Text(
                        formatMonth(appointment.appointment_date),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF909399)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFE4E7ED))
            Spacer(modifier = Modifier.height(12.dp))

            // Info rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppointmentInfoRow(Icons.Default.Business, appointment.hospital ?: "", Color(0xFF1A8080))
                if (!appointment.department.isNullOrBlank()) {
                    AppointmentInfoRow(Icons.Default.MedicalServices, appointment.department, Color(0xFF409EFF))
                }
                AppointmentInfoRow(Icons.Default.Schedule, appointment.appointment_time ?: "", Color(0xFFE6A23C))
            }

            // Action buttons
            if (appointment.status == "pending") {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF52C41A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("完成", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("取消", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF56C6C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("删除记录", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AppointmentInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, fontSize = 15.sp, color = Color(0xFF606266))
    }
}

@Composable
private fun EmptyAppointmentView(tab: Int, onAdd: () -> Unit) {
    val (title, subtitle) = when (tab) {
        0 -> "暂无待就诊预约" to "点击下方按钮添加您的就医预约"
        1 -> "暂无已完成预约" to "完成的预约记录会显示在这里"
        2 -> "暂无已取消预约" to "取消的预约记录会显示在这里"
        else -> "暂无预约" to ""
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFF0F2F5), Color(0xFFE4E7ED)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFFC0C4CC), modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF303133))
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = Color(0xFF909399))

        if (tab == 0) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = AuraTokens.Primary),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加预约", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

private fun formatDay(dateStr: String?): String {
    if (dateStr == null) return "--"
    return runCatching {
        val parts = dateStr.split("-")
        if (parts.size >= 3) parts[2] else "--"
    }.getOrDefault("--")
}

private fun formatMonth(dateStr: String?): String {
    if (dateStr == null) return "--"
    return runCatching {
        val parts = dateStr.split("-")
        if (parts.size >= 2) "${parts[1]}月" else "--"
    }.getOrDefault("--")
}
