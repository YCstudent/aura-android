package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edistrive.aura.ui.components.IosTopBar
import com.edistrive.aura.ui.components.StyledConfirmDialog
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.theme.AuraTokens

private val PrimaryColor = Color(0xFF1A8080)
private val TextPrimary = Color(0xFF2C3E50)
private val TextSecondary = Color(0xFF909399)
private val DangerColor = Color(0xFFF56C6C)
private val ChevronColor = Color(0xFFC0C4CC)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onUserAgreement: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
    onClearCache: () -> Unit
) {
    var medicationNotification by remember { mutableStateOf(true) }
    var appointmentNotification by remember { mutableStateOf(true) }
    var showLogoutAlert by remember { mutableStateOf(false) }
    var showClearCacheAlert by remember { mutableStateOf(false) }
    var showAboutAlert by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        IosTopBar(title = "设置", onBack = onBack, background = Color(0xFFF5F7FA))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 账号设置 — matches iOS "账号设置" section
            SettingsSection(
                icon = Icons.Filled.Person,
                title = "账号设置",
                iconColor = PrimaryColor
            ) {
                SettingsNavItem(Icons.Filled.Person, "个人信息", "修改头像、昵称、个人资料", onClick = onProfile)
                Divider(Modifier.padding(start = 75.dp))
                SettingsNavItem(Icons.Filled.Key, "修改密码", "定期更换密码保护账号安全", onClick = onChangePassword)
            }

            // 2. 通知提醒 — matches iOS "通知提醒" section
            SettingsSection(
                icon = Icons.Filled.Notifications,
                title = "通知提醒",
                iconColor = PrimaryColor
            ) {
                SettingsToggleItem(Icons.Filled.Medication, "用药提醒", "推送通知提醒服药时间", medicationNotification) { medicationNotification = it }
                Divider(Modifier.padding(start = 75.dp))
                SettingsToggleItem(Icons.Filled.CalendarMonth, "预约提醒", "推送通知提醒即将到来的预约", appointmentNotification) { appointmentNotification = it }
            }

            // 3. 关于应用 — matches iOS "关于应用" section
            SettingsSection(
                icon = Icons.Filled.Info,
                title = "关于应用",
                iconColor = PrimaryColor
            ) {
                SettingsNavItem(Icons.Filled.Security, "隐私政策", "了解我们如何保护您的隐私", onClick = onPrivacyPolicy)
                Divider(Modifier.padding(start = 75.dp))
                SettingsNavItem(Icons.Filled.Description, "用户服务协议", "查看服务条款和使用规范", onClick = onUserAgreement)
                Divider(Modifier.padding(start = 75.dp))
                SettingsInfoItem(Icons.Filled.Info, "版本信息", "v1.0.0") { showAboutAlert = true }
            }

            // 4. 危险操作 — matches iOS "危险操作" section
            SettingsSection(
                icon = Icons.Filled.Warning,
                title = "危险操作",
                iconColor = DangerColor
            ) {
                SettingsNavItem(Icons.Filled.Delete, "清除缓存", "清除本地缓存数据", isDanger = true, onClick = { showClearCacheAlert = true })
                Divider(Modifier.padding(start = 75.dp))
                SettingsNavItem(Icons.Filled.ExitToApp, "退出登录", "退出当前账号", isDanger = true, onClick = { showLogoutAlert = true })
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // Styled dialogs — match iOS alert style
    if (showAboutAlert) {
        StyledInfoDialog(
            icon = Icons.Filled.Info,
            title = "关于优雅素问",
            message = "优雅素问 v1.0.0\n\n智能健康管理助手\n让健康管理更简单",
            onDismiss = { showAboutAlert = false }
        )
    }

    if (showLogoutAlert) {
        StyledConfirmDialog(
            icon = Icons.Filled.ExitToApp,
            title = "退出登录",
            message = "确定要退出登录吗？",
            confirmLabel = "退出",
            onConfirm = { showLogoutAlert = false; onLogout() },
            onDismiss = { showLogoutAlert = false }
        )
    }

    if (showClearCacheAlert) {
        StyledConfirmDialog(
            icon = Icons.Filled.Delete,
            title = "清除缓存",
            message = "清除缓存后需要重新登录，是否继续？",
            confirmLabel = "清除",
            onConfirm = { showClearCacheAlert = false; onClearCache() },
            onDismiss = { showClearCacheAlert = false }
        )
    }
}

// ========== SettingsSection — matches iOS settingsSection builder ==========

@Composable
private fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        // Section header with gradient — matches iOS section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(iconColor.copy(alpha = 0.08f), iconColor.copy(alpha = 0.04f))),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = iconColor)
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        // Content with white background and rounded bottom corners
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
        ) {
            content()
        }
    }
}

// ========== SettingsNavItem — matches iOS SettingNavigationItem ==========

@Composable
private fun SettingsNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (isDanger) DangerColor else PrimaryColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = color)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isDanger) DangerColor else TextPrimary)
            Text(description, fontSize = 13.sp, color = TextSecondary)
        }

        Icon(Icons.Filled.ChevronRight, null, Modifier.size(13.dp), tint = ChevronColor)
    }
}

// ========== SettingsToggleItem — matches iOS SettingToggleItem ==========

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(PrimaryColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = PrimaryColor)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(description, fontSize = 13.sp, color = TextSecondary)
        }

        Switch(checked = isOn, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryColor))
    }
}

// ========== SettingsInfoItem — matches iOS SettingInfoItem ==========

@Composable
private fun SettingsInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(PrimaryColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = PrimaryColor)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("当前版本：$value", fontSize = 13.sp, color = TextSecondary)
        }

        Icon(Icons.Filled.ChevronRight, null, Modifier.size(13.dp), tint = ChevronColor)
    }
}
