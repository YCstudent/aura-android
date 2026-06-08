package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.edistrive.aura.ui.state.HomeViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.ui.components.StyledConfirmDialog

@Composable
fun MeScreen(
    modifier: Modifier = Modifier,
    navController: androidx.navigation.NavHostController? = null,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 个人卡片
            PersonalCard(
                username = uiState.username,
                avatarPath = uiState.currentUser?.avatar,
                profileCompleted = uiState.currentUser?.profile_completed ?: false,
                navController = navController
            )

            // 2. 数据统计卡片
            StatsCard(
                healthScore = uiState.healthScore,
                familyCount = uiState.familyMembersCount,
                recordsCount = uiState.medicalRecordsCount
            )

            // 3. 核心功能区域
            FunctionSection(
                title = "核心功能",
                icon = Icons.Filled.Star
            ) {
                MenuRow(
                    icon = Icons.Filled.People,
                    title = "家庭管理",
                    subtitle = "管理家人健康档案",
                    color = AuraTokens.Primary,
                    onClick = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.FAMILY_MANAGEMENT) }
                )
                Divider(modifier = Modifier.padding(start = 68.dp))
                MenuRow(
                    icon = Icons.Filled.Description,
                    title = "超级病历",
                    subtitle = "全生命周期健康档案",
                    color = Color(0xFFFF9800),
                    onClick = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.MEDICAL_RECORDS) }
                )
                Divider(modifier = Modifier.padding(start = 68.dp))
                MenuRow(
                    icon = Icons.Filled.Medication,
                    title = "用药提醒",
                    subtitle = "管理用药计划",
                    color = Color(0xFFF56C6C),
                    onClick = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.MEDICATIONS) }
                )
                Divider(modifier = Modifier.padding(start = 68.dp))
                MenuRow(
                    icon = Icons.Filled.CalendarMonth,
                    title = "预约管理",
                    subtitle = "记录已预约的就医时间",
                    color = Color(0xFF2196F3),
                    onClick = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.APPOINTMENTS) }
                )
            }

            // 4. 更多功能区域
            FunctionSection(
                title = "更多功能",
                icon = Icons.Filled.Apps
            ) {
                MenuRow(
                    icon = Icons.Filled.LocationOn,
                    title = "医院导航",
                    subtitle = "查找周边医疗资源",
                    color = Color(0xFF9C27B0),
                    onClick = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.HOSPITALS_MAP) }
                )
                Divider(modifier = Modifier.padding(start = 68.dp))
                MenuRow(
                    icon = Icons.Filled.History,
                    title = "活动记录",
                    subtitle = "查看操作历史",
                    color = Color(0xFF00BCD4),
                    onClick = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.RECENT_ACTIVITIES) }
                )
                Divider(modifier = Modifier.padding(start = 68.dp))
                MenuRow(
                    icon = Icons.Filled.Link,
                    title = "接受邀请",
                    subtitle = "关联家人健康档案",
                    color = Color(0xFFFF5722),
                    onClick = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.ACCEPT_INVITATION) }
                )
            }

            // 5. 设置区域
            FunctionSection(
                title = "设置",
                icon = Icons.Filled.Settings
            ) {
                MenuRow(
                    icon = Icons.Filled.Settings,
                    title = "应用设置",
                    subtitle = "账号、通知、隐私等设置",
                    color = AuraTokens.Primary,
                    onClick = { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.SETTINGS) }
                )
            }

            // 6. 退出登录按钮
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFF56C6C)
                    )
                    Text(
                        text = "退出登录",
                        color = Color(0xFFF56C6C),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // 退出登录确认对话框
    if (showLogoutDialog) {
        StyledConfirmDialog(
            icon = Icons.Filled.ExitToApp,
            title = "退出登录",
            message = "确定要退出当前账号吗？",
            confirmLabel = "退出",
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun PersonalCard(
    username: String,
    avatarPath: String?,
    profileCompleted: Boolean,
    navController: androidx.navigation.NavHostController? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController?.navigate(com.edistrive.aura.ui.navigation.Routes.PROFILE_CENTER) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AuraTokens.Primary.copy(alpha = 0.2f),
                                AuraTokens.Primary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = avatarPath?.let {
                    if (it.startsWith("http")) it else "https://zgjcyl.com$it"
                }
                if (!avatarUrl.isNullOrBlank()) {
                    val ctx = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(avatarUrl)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = AuraTokens.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (profileCompleted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (profileCompleted) Color(0xFF67C23A) else Color(0xFFE6A23C),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (profileCompleted) "档案完整" else "档案待完善",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "点击编辑资料 →",
                    fontSize = 12.sp,
                    color = AuraTokens.Primary
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    healthScore: Int,
    familyCount: Int,
    recordsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItem(
                icon = Icons.Filled.Favorite,
                value = healthScore.toString(),
                label = "健康分",
                color = Color(0xFFF56C6C),
                modifier = Modifier.weight(1f)
            )
            StatItem(
                icon = Icons.Filled.Person,
                value = familyCount.toString(),
                label = "家庭成员",
                color = AuraTokens.Primary,
                modifier = Modifier.weight(1f)
            )
            StatItem(
                icon = Icons.Filled.Description,
                value = recordsCount.toString(),
                label = "病历",
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            color.copy(alpha = 0.08f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF303133)
        )

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF909399)
        )
    }
}

@Composable
private fun FunctionSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AuraTokens.Primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF303133)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF303133)
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color(0xFF909399)
            )
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFC0C4CC),
            modifier = Modifier.size(13.dp)
        )
    }
}
