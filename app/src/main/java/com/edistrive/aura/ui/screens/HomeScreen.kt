package com.edistrive.aura.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.edistrive.aura.ui.state.HomeViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.ui.components.StyledConfirmDialog
import com.edistrive.aura.data.model.FamilyMember
import com.edistrive.aura.data.model.Medication
import com.edistrive.aura.data.model.UserActivity

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: androidx.navigation.NavHostController? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var cancelCheckInTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var showCancelAlert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(uiState.toast) {
        if (uiState.toast != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.consumeToast()
        }
    }

    val bg = Brush.linearGradient(
        colors = listOf(AuraTokens.SurfaceAlt, Color(0xFFC3CFE2))
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 120.dp)
        ) {
            BrandHeader(onSettings = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.SETTINGS) })

            Spacer(modifier = Modifier.height(20.dp))

            DashboardHeader(
                username = uiState.username,
                currentDate = uiState.currentDate,
                greeting = uiState.greeting,
                avatarPath = uiState.currentUser?.avatar,
                healthScore = uiState.healthScore,
                healthTip = uiState.healthTip,
                medicationsCount = uiState.medicationsCount,
                appointmentsCount = uiState.appointmentsCount,
                medicalRecordsCount = uiState.medicalRecordsCount,
                familyMembersCount = uiState.familyMembersCount,
                navController = navController
            )

            Spacer(modifier = Modifier.height(24.dp))

            FamilySection(
                members = uiState.familyMembers,
                onViewAll = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.FAMILY_MANAGEMENT) },
                onMemberClick = { id -> navController?.navigate(com.edistrive.aura.ui.navigation.Routes.familyDetail(id)) },
                onAdd = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.FAMILY_ADD) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            MedicationSection(
                items = uiState.todayMedications,
                onViewAll = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.MEDICATIONS) },
                onTakeMedication = { id, time -> viewModel.takeMedication(id, time) },
                onCancelMedication = { id, time ->
                    cancelCheckInTarget = Pair(id, time)
                    showCancelAlert = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            ServicesGrid(
                onOpenMedicalRecords = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.MEDICAL_RECORDS) },
                onOpenDigitalHuman = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.DIGITAL_HUMAN) },
                onOpenMedications = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.MEDICATIONS) },
                onOpenAcceptInvitation = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.ACCEPT_INVITATION) },
                onOpenHospitals = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.HOSPITALS_MAP) },
                onOpenAppointments = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.APPOINTMENTS) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            ActivitiesSection(
                items = uiState.recentActivities,
                onViewAll = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.RECENT_ACTIVITIES) },
                onActivityClick = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.RECENT_ACTIVITIES) }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Toast overlay
        AnimatedVisibility(
            visible = uiState.toast != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (uiState.toastIsError) Color(0xFFE5484D) else Color(0xFF2BB0A8))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (uiState.toastIsError) Icons.Default.Cancel else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(uiState.toast.orEmpty(), color = Color.White, fontSize = 14.sp)
            }
        }
    }

    // Cancel check-in confirmation
    if (showCancelAlert && cancelCheckInTarget != null) {
        StyledConfirmDialog(
            icon = Icons.Filled.Cancel,
            title = "取消打卡",
            message = "确定要取消 ${cancelCheckInTarget!!.second} 的服药记录吗？",
            confirmLabel = "确定",
            onConfirm = {
                cancelCheckInTarget?.let { (id, time) ->
                    viewModel.cancelMedicationCheckIn(id, time)
                }
                showCancelAlert = false
                cancelCheckInTarget = null
            },
            onDismiss = {
                showCancelAlert = false
                cancelCheckInTarget = null
            }
        )
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    val headerIcon = when (title) {
        "我的家庭" -> Icons.Filled.Person
        "今日用药" -> Icons.Filled.Medication
        "健康服务" -> Icons.Filled.CalendarMonth
        "近期动态" -> Icons.Filled.Notifications
        else -> Icons.Filled.Person
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = headerIcon, contentDescription = null, tint = AuraTokens.TextPrimary)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AuraTokens.TextPrimary
            )
        }
        if (!trailing.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onTrailingClick != null) Modifier.clickable(onClick = onTrailingClick) else Modifier
            ) {
                Text(text = trailing, color = AuraTokens.Primary, fontWeight = FontWeight.SemiBold)
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AuraTokens.Primary
                )
            }
        }
    }
}

@Composable
private fun FamilySection(
    members: List<FamilyMember>,
    onViewAll: () -> Unit = {},
    onMemberClick: (Int) -> Unit = {},
    onAdd: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewAll),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeaderRow(title = "我的家庭", trailing = "查看全部")
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(members) { m ->
                    FamilyMemberCard(member = m, onClick = { m.id?.let(onMemberClick) })
                }
                item {
                    AddMemberCard(onClick = onAdd)
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberCard(member: FamilyMember, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    val avatarUrl = member.avatar?.let {
        if (it.startsWith("http")) it else "https://zgjcyl.com$it"
    }

    Card(
        modifier = Modifier
            .width(112.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTokens.SurfaceAlt)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(29.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(29.dp))
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = AuraTokens.TextSecondary)
                }
            }
            Text(
                text = member.display_name ?: member.name ?: "成员",
                color = AuraTokens.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddMemberCard(onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .width(112.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AuraTokens.Primary.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "+", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "添加成员", color = Color.White.copy(alpha = 0.95f), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MedicationSection(
    items: List<Medication>,
    onViewAll: () -> Unit = {},
    onTakeMedication: (Int, String) -> Unit = { _, _ -> },
    onCancelMedication: (Int, String) -> Unit = { _, _ -> }
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewAll),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeaderRow(title = "今日用药", trailing = "查看全部")
            }
            if (items.isEmpty()) {
                Text(text = "暂无今日用药", color = AuraTokens.TextSecondary)
            } else {
                items.take(3).forEach { med ->
                    HomeMedicationCard(
                        item = med,
                        onTakeMedication = onTakeMedication,
                        onCancelMedication = onCancelMedication
                    )
                }
            }
        }
    }
}

// iOS-style medication card for HomeScreen
@Composable
private fun HomeMedicationCard(
    item: Medication,
    onTakeMedication: (Int, String) -> Unit = { _, _ -> },
    onCancelMedication: (Int, String) -> Unit = { _, _ -> }
) {
    val diagonalTeal = Brush.linearGradient(
        listOf(AuraTokens.Primary, AuraTokens.Primary2),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val diagonalTealBorder = Brush.linearGradient(
        listOf(AuraTokens.Primary.copy(alpha = 0.3f), AuraTokens.Primary2.copy(alpha = 0.3f)),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, diagonalTealBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(8.dp, CircleShape, ambientColor = AuraTokens.Primary.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(diagonalTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.name.orEmpty(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.weight(1f)
                    )
                    HomeActiveStatusPill(active = item.is_active != false)
                }

                item.dosage?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 14.sp, color = Color(0xFF606266))
                }

                val times = item.reminder_times.orEmpty()
                if (times.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(times) { time ->
                            val taken = item.records_today.orEmpty().any {
                                it.effectiveTime == time && it.status == "taken"
                            }
                            val passed = !taken && java.time.LocalTime.now().isAfter(
                                try { java.time.LocalTime.parse(time) } catch (_: Exception) { java.time.LocalTime.MIN }
                            )
                            val medId = item.id ?: return@items
                            HomeTimeTag(
                                time = time,
                                taken = taken,
                                passed = passed,
                                onClick = {
                                    if (taken) onCancelMedication(medId, time)
                                    else onTakeMedication(medId, time)
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFF909399), modifier = Modifier.size(11.dp))
                        Text(
                            "${item.start_date.orEmpty()} 开始",
                            fontSize = 12.sp,
                            color = Color(0xFF909399)
                        )
                    }
                    item.days_remaining?.takeIf { it > 0 }?.let { days ->
                        Text("·", color = Color(0xFFE4E7ED), fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("剩", fontSize = 12.sp, color = Color(0xFFE6A23C))
                            Text("$days", fontSize = 13.sp, color = Color(0xFFE6A23C), fontWeight = FontWeight.Bold)
                            Text("天", fontSize = 12.sp, color = Color(0xFFE6A23C))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActiveStatusPill(active: Boolean) {
    val (label, fg, bg) = if (active) {
        Triple("进行中", Color(0xFF67C23A), Color(0xFF67C23A).copy(alpha = 0.1f))
    } else {
        Triple("已暂停", Color(0xFF909399), Color(0xFFF4F4F5))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(fg))
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HomeTimeTag(time: String, taken: Boolean, passed: Boolean, onClick: () -> Unit = {}) {
    val bgBrush: Brush
    val fg: Color
    val extraMod: Modifier

    when {
        taken -> {
            bgBrush = Brush.horizontalGradient(listOf(Color(0xFF67C23A), Color(0xFF85CE61)))
            fg = Color.White
            extraMod = Modifier.shadow(6.dp, RoundedCornerShape(10.dp), ambientColor = Color(0xFF67C23A).copy(alpha = 0.3f))
        }
        passed -> {
            bgBrush = Brush.horizontalGradient(listOf(Color(0xFFB0B0B0), Color(0xFF999999)))
            fg = Color.White
            extraMod = Modifier
        }
        else -> {
            bgBrush = Brush.horizontalGradient(listOf(AuraTokens.Primary.copy(alpha = 0.1f), AuraTokens.Primary2.copy(alpha = 0.1f)))
            fg = AuraTokens.Primary
            extraMod = Modifier.border(1.5.dp, AuraTokens.Primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(extraMod)
            .background(bgBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Icon(
            if (taken) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(time, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ServicesGrid(
    onOpenMedicalRecords: () -> Unit = {},
    onOpenDigitalHuman: () -> Unit = {},
    onOpenMedications: () -> Unit = {},
    onOpenAcceptInvitation: () -> Unit = {},
    onOpenHospitals: () -> Unit = {},
    onOpenAppointments: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SectionHeaderRow(title = "健康服务")

            // Row 1
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                ServiceIcon(
                    title = "超级病历",
                    gradientStart = Color(0xFF722ED1),
                    gradientEnd = Color(0xFF9254DE),
                    icon = Icons.Filled.Description,
                    onClick = onOpenMedicalRecords
                )
                ServiceIcon(
                    title = "医小智",
                    gradientStart = Color(0xFF52C41A),
                    gradientEnd = Color(0xFF73D13D),
                    icon = Icons.Filled.Chat,
                    onClick = onOpenDigitalHuman
                )
                ServiceIcon(
                    title = "用药提醒",
                    gradientStart = Color(0xFFFAAD14),
                    gradientEnd = Color(0xFFFFC53D),
                    icon = Icons.Filled.Alarm,
                    onClick = onOpenMedications
                )
            }

            // Row 2
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                ServiceIcon(
                    title = "接受邀请",
                    gradientStart = Color(0xFF13C2C2),
                    gradientEnd = Color(0xFF36CFC9),
                    icon = Icons.Filled.Link,
                    onClick = onOpenAcceptInvitation
                )
                ServiceIcon(
                    title = "医院导航",
                    gradientStart = Color(0xFF1890FF),
                    gradientEnd = Color(0xFF40A9FF),
                    icon = Icons.Filled.LocationOn,
                    onClick = onOpenHospitals
                )
                ServiceIcon(
                    title = "预约管理",
                    gradientStart = Color(0xFFF5222D),
                    gradientEnd = Color(0xFFFF4D4F),
                    icon = Icons.Filled.CalendarMonth,
                    onClick = onOpenAppointments
                )
            }
        }
    }
}

@Composable
private fun ServiceIcon(
    title: String,
    gradientStart: Color,
    gradientEnd: Color,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    val iconBoxSize = 60.dp
    val cornerRadius = 18.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = gradientStart.copy(alpha = 0.30f),
                    spotColor = gradientStart.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.linearGradient(
                        listOf(gradientStart, gradientEnd),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset.Infinite
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title,
            color = Color(0xFF333333),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActivitiesSection(
    items: List<UserActivity>,
    onViewAll: () -> Unit = {},
    onActivityClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeaderRow(
                title = "近期动态",
                trailing = "查看全部",
                onTrailingClick = onViewAll
            )
            if (items.isEmpty()) {
                Text(text = "暂无动态", color = AuraTokens.TextSecondary)
            } else {
                items.take(5).forEach { a ->
                    ActivityRow(item = a, onClick = onActivityClick)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(item: UserActivity, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(AuraTokens.SurfaceAlt, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val activityIcon = when (item.type) {
            "medical_record" -> Icons.Filled.Description
            "ai_chat" -> Icons.Filled.Chat
            "profile_update" -> Icons.Filled.Person
            "login" -> Icons.Filled.Login
            else -> Icons.Filled.Notifications
        }
        val activityColor = when (item.color) {
            "blue" -> Color(0xFF3498DB)
            "green" -> Color(0xFF27AE60)
            "purple" -> Color(0xFF9B59B6)
            "orange" -> Color(0xFFE67E22)
            else -> Color(0xFF7C4DFF)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(activityColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = activityIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.title ?: "动态",
                fontWeight = FontWeight.SemiBold,
                color = AuraTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, color = AuraTokens.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            item.time?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, color = AuraTokens.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BrandHeader(onSettings: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = com.edistrive.aura.R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = AuraTokens.Primary.copy(alpha = 0.15f),
                        spotColor = AuraTokens.Primary.copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "优雅素问",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AuraTokens.Primary
                )
                Text(
                    text = "守护家人健康的数智伙伴",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF5A9EA0)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "设置",
                tint = AuraTokens.Primary
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    username: String,
    currentDate: String,
    greeting: String,
    avatarPath: String?,
    healthScore: Int,
    healthTip: String,
    medicationsCount: Int,
    appointmentsCount: Int,
    medicalRecordsCount: Int,
    familyMembersCount: Int,
    navController: androidx.navigation.NavHostController? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        val gradient = Brush.linearGradient(
            colors = listOf(AuraTokens.Primary, AuraTokens.Primary2)
        )

        val context = LocalContext.current
        val avatarUrl = avatarPath?.let {
            if (it.startsWith("http")) it else "https://zgjcyl.com$it"
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    navController?.navigate(com.edistrive.aura.ui.navigation.Routes.PROFILE_CENTER)
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarUrl)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = username,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = currentDate,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.70f)) {
                    Text(
                        text = greeting,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HealthTipBubble(text = healthTip)
                }

                ScoreRing(score = healthScore)
            }

            Spacer(modifier = Modifier.height(16.dp))

            StatsGrid(
                medicalRecordsCount = medicalRecordsCount,
                medicationsCount = medicationsCount,
                familyMembersCount = familyMembersCount,
                appointmentsCount = appointmentsCount
            )
        }
    }
}

@Composable
private fun HealthTipBubble(text: String) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun ScoreRing(score: Int) {
    Box(
        modifier = Modifier
            .size(82.dp)
            .border(6.dp, Color.White, RoundedCornerShape(41.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "分",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun StatsGrid(
    medicalRecordsCount: Int,
    medicationsCount: Int,
    familyMembersCount: Int,
    appointmentsCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(
                title = "病历",
                value = medicalRecordsCount,
                accent = Color(0xFF4C87FF),
                icon = Icons.Filled.Description,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
            StatTile(
                title = "用药",
                value = medicationsCount,
                accent = Color(0xFFFF6B6B),
                icon = Icons.Filled.Medication,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(
                title = "成员",
                value = familyMembersCount,
                accent = Color(0xFF3DDC97),
                icon = Icons.Filled.Person,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
            StatTile(
                title = "预约",
                value = appointmentsCount,
                accent = Color(0xFFFFB020),
                icon = Icons.Filled.CalendarMonth,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: Int,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
        }
        Column {
            Text(text = value.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = title, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun StatPill(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.32f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.size(8.dp))
            Column {
                Text(text = title, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                Text(text = value, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = AuraTokens.TextPrimary
    )
}

@Composable
private fun PlaceholderRowCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuraTokens.SurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = AuraTokens.Primary)
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = AuraTokens.TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = AuraTokens.TextSecondary)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = AuraTokens.TextSecondary)
        }
    }
}

@Composable
private fun ServicesGridPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ServiceTile(title = "家庭管理", icon = Icons.Filled.Person, modifier = Modifier.fillMaxWidth(0.5f))
            ServiceTile(title = "病历记录", icon = Icons.Filled.Description, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ServiceTile(title = "用药管理", icon = Icons.Filled.Medication, modifier = Modifier.fillMaxWidth(0.5f))
            ServiceTile(title = "就医预约", icon = Icons.Filled.CalendarMonth, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ServiceTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuraTokens.SurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = AuraTokens.Primary)
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(text = title, fontWeight = FontWeight.SemiBold, color = AuraTokens.TextPrimary)
        }
    }
}
