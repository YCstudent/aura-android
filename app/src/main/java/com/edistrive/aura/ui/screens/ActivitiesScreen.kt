package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edistrive.aura.data.model.UserActivity
import com.edistrive.aura.ui.navigation.Routes
import com.edistrive.aura.ui.state.ActivitiesViewModel
import com.edistrive.aura.ui.theme.AuraTokens

// iOS exact colors
private val Teal = Color(0xFF1A8080)
private val Teal2 = Color(0xFF2D9C9C)
private val DarkText = Color(0xFF2C3E50)
private val GrayText = Color(0xFF7F8C8D)
private val LightGray = Color(0xFF95A5A6)
private val ChevronGray = Color(0xFFBDC3C7)
private val ChipText = Color(0xFF5A6C7D)
private val EmptyBg = Color(0xFFECF0F1)

@Composable
fun ActivitiesScreen(
    onBack: () -> Unit,
    navController: NavController? = null,
    viewModel: ActivitiesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadActivities() }

    val bg = Brush.linearGradient(
        colors = listOf(AuraTokens.SurfaceAlt, Color(0xFFC3CFE2)),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset.Infinite
    )

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // ---- Navigation bar ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronLeft, null,
                        tint = Teal,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "近期动态",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                    Text(
                        "${state.filteredActivities.size} 条记录",
                        fontSize = 13.sp,
                        color = GrayText
                    )
                }

                Spacer(Modifier.weight(1f))
            }

            // ---- Filter chips ----
            val filters = remember {
                listOf(
                    FilterItem("all", "全部", Icons.Default.Menu),
                    FilterItem("medical_record", "病历", Icons.Default.Description),
                    FilterItem("ai_chat", "AI对话", Icons.Default.Chat),
                    FilterItem("profile_update", "个人信息", Icons.Default.Person),
                    FilterItem("phone_change", "手机号", Icons.Default.Phone),
                    FilterItem("email_change", "邮箱", Icons.Default.Email),
                    FilterItem("password_change", "密码", Icons.Default.Lock),
                    FilterItem("login", "登录", Icons.Default.Login),
                    FilterItem("health_profile", "健康档案", Icons.Default.Favorite),
                    FilterItem("medication_reminder", "用药提醒", Icons.Default.Schedule),
                    FilterItem("account", "账号相关", Icons.Default.AccountCircle)
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(filters) { f ->
                    val sel = state.selectedFilter == f.key
                    val gradient = Brush.horizontalGradient(listOf(Teal, Teal2))

                    Row(
                        modifier = Modifier
                            .shadow(
                                elevation = if (sel) 8.dp else 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = if (sel) Teal.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.06f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) gradient else Brush.horizontalGradient(listOf(Color.White, Color.White)))
                            .clickable { viewModel.setFilter(f.key) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            f.icon, null,
                            tint = if (sel) Color.White else ChipText,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            f.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (sel) Color.White else ChipText
                        )
                    }
                }
            }

            // ---- Content ----
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Teal)
                            Spacer(Modifier.height(12.dp))
                            Text("加载中...", fontSize = 14.sp, color = LightGray)
                        }
                    }
                }
                state.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(EmptyBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning, null,
                                    tint = LightGray,
                                    modifier = Modifier.size(45.dp)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "加载失败",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkText
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.error ?: "请检查网络连接后重试",
                                fontSize = 14.sp,
                                color = GrayText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadActivities() },
                                colors = ButtonDefaults.buttonColors(containerColor = Teal)
                            ) {
                                Text("重新加载", color = Color.White)
                            }
                        }
                    }
                }
                state.filteredActivities.isEmpty() && !state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(EmptyBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Schedule, null,
                                    tint = LightGray,
                                    modifier = Modifier.size(45.dp)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "暂无活动记录",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkText
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "开始使用系统功能，这里会显示您的操作记录",
                                fontSize = 14.sp,
                                color = GrayText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.filteredActivities) { a ->
                            ActivityCard(activity = a, navController = navController)
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}

// ---- Activity Card ----
@Composable
private fun ActivityCard(
    activity: UserActivity,
    navController: NavController? = null
) {
    val icon = activityIcon(activity.type)
    val color = activityColor(activity.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                val route = when (activity.type) {
                    "medical_record" -> Routes.MEDICAL_RECORDS
                    "medication" -> Routes.MEDICATIONS
                    "appointment" -> Routes.APPOINTMENTS
                    "ai_chat" -> Routes.DIGITAL_HUMAN
                    "family_member" -> Routes.FAMILY_MANAGEMENT
                    "profile_update" -> Routes.PROFILE_CENTER
                    else -> null
                }
                route?.let { navController?.navigate(it) }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    activity.title ?: "动态",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                activity.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        fontSize = 14.sp,
                        color = GrayText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule, null,
                        tint = LightGray,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        activity.time ?: "",
                        fontSize = 12.sp,
                        color = LightGray
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint = ChevronGray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private data class FilterItem(
    val key: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun activityIcon(type: String?) = when (type) {
    "medical_record" -> Icons.Default.Description
    "ai_chat" -> Icons.Default.Chat
    "profile_update" -> Icons.Default.Person
    "phone_change" -> Icons.Default.Phone
    "email_change" -> Icons.Default.Email
    "password_change" -> Icons.Default.Lock
    "login" -> Icons.Default.Login
    "health_profile" -> Icons.Default.Favorite
    "medication_reminder" -> Icons.Default.Schedule
    "medication" -> Icons.Default.Medication
    "appointment" -> Icons.Default.CalendarMonth
    "family_member" -> Icons.Default.Person
    else -> Icons.Default.Notifications
}

private fun activityColor(c: String?) = when (c) {
    "blue" -> Color(0xFF3498DB)
    "green" -> Color(0xFF2ECC71)
    "red" -> Color(0xFFE74C3C)
    "orange" -> Color(0xFFF39C12)
    "purple" -> Color(0xFF9B59B6)
    else -> Color(0xFF95A5A6)
}
