package com.edistrive.aura.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.edistrive.aura.ui.theme.AuraTokens

/** Standardized status pill (e.g. 已关联 / 待激活 / 独立). */
@Composable
fun StatusPill(
    label: String,
    fg: Color,
    bg: Color,
    icon: ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Outlined capsule action button used in cards (改名 / 编辑 / 解除). */
@Composable
fun CapsuleActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Gradient pill primary CTA (e.g. 添加成员). */
@Composable
fun GradientPillButton(
    text: String,
    gradientStart: Color,
    gradientEnd: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val brush = Brush.horizontalGradient(listOf(gradientStart, gradientEnd))
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(brush, RoundedCornerShape(25.dp))
                .padding(horizontal = 28.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Avatar circle with optional gender badge bottom-right (matches iOS family card). */
@Composable
fun AvatarWithGenderBadge(
    avatarUrl: String?,
    initial: String,
    gender: String?,
    sizeDp: Int = 72,
    badgeDp: Int = 24
) {
    Box(modifier = Modifier.size(sizeDp.dp)) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(AuraTokens.SurfaceAlt)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(AuraTokens.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial,
                    color = AuraTokens.Primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (sizeDp / 2.5).sp
                )
            }
        }
        val badgeColor = when (gender) {
            "male" -> AuraTokens.Primary
            "female" -> Color(0xFFEF7AA0)
            else -> Color(0xFF909399)
        }
        Box(
            modifier = Modifier
                .size(badgeDp.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(badgeColor)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (gender) { "female" -> "♀"; else -> "♂" },
                color = Color.White,
                fontSize = (badgeDp / 2).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

object AuraExtraColors {
    val SuccessGreen = Color(0xFF67C23A)
    val SuccessGreenLight = Color(0xFF85CE61)
    val WarningOrange = Color(0xFFE6A23C)
    val WarningOrangeLight = Color(0xFFF39C12)
    val ErrorRed = Color(0xFFF56C6C)
    val ErrorRedLight = Color(0xFFFF7675)
    val GrayText = Color(0xFF606266)
    val GrayLight = Color(0xFF909399)
    val GrayLightest = Color(0xFFDCDFE6)
    val GrayBg = Color(0xFFF5F7FA)
    val GrayBgLighter = Color(0xFFF4F4F5)
    val GrayBgLightest = Color(0xFFFAFAFA)
    val DarkText = Color(0xFF2C3E50)
    val DetailGradientEnd = Color(0xFFC3CFE2)
}

/**
 * Material3 DatePicker dialog. Returns YYYY-MM-DD string via [onConfirm].
 * Pass [initialIso] to preselect a date.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AuraDatePickerDialog(
    initialIso: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialMillis = remember(initialIso) {
        runCatching {
            val d = java.time.LocalDate.parse(initialIso?.substring(0, minOf(10, initialIso.length)) ?: "")
            d.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
    }
    val state = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                state.selectedDateMillis?.let {
                    val date = java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate()
                    onConfirm(date.toString())
                } ?: onDismiss()
            }) { androidx.compose.material3.Text("确定") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("取消")
            }
        }
    ) {
        androidx.compose.material3.DatePicker(state = state)
    }
}

/** Date input row that opens a [AuraDatePickerDialog] when tapped. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DateInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.material3.OutlinedTextField(
        value = if (value.isBlank()) "" else value,
        onValueChange = {},
        readOnly = true,
        label = { androidx.compose.material3.Text(label) },
        trailingIcon = {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.CalendarMonth,
                contentDescription = "选择日期",
                tint = AuraExtraColors.GrayLight
            )
        },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .clickable { open = true },
        enabled = false,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            disabledTextColor = AuraExtraColors.DarkText,
            disabledLabelColor = AuraExtraColors.GrayLight,
            disabledBorderColor = AuraExtraColors.GrayLightest,
            disabledTrailingIconColor = AuraExtraColors.GrayLight
        )
    )
    if (open) {
        AuraDatePickerDialog(
            initialIso = value.takeIf { it.isNotBlank() },
            onDismiss = { open = false },
            onConfirm = {
                onValueChange(it)
                open = false
            }
        )
    }
}

/**
 * Shared brand view: iOS-style gradient background + logo + app name + subtitle.
 * Used by SplashScreen and AuraLoadingView for consistent branding.
 */
@Composable
fun AuraBrandView(
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A8080), Color(0xFF2D9C9C)),
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset.Infinite
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo — matching iOS: 96dp, cornerRadius 24, shadow
            Image(
                painter = androidx.compose.ui.res.painterResource(id = com.edistrive.aura.R.drawable.app_logo),
                contentDescription = "优雅素问",
                modifier = Modifier
                    .size(96.dp)
                    .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "优雅素问",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "智能健康助手",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )

            if (content != null) {
                Spacer(modifier = Modifier.height(48.dp))
                content()
            }
        }
    }
}

/**
 * iOS-style loading page — same brand layout as splash screen, plus a loading spinner and text.
 * Matches iOS pattern: brand identity stays consistent, spinner indicates activity.
 */
@Composable
fun AuraLoadingView(
    text: String = "加载中...",
    modifier: Modifier = Modifier
) {
    AuraBrandView(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
            Text(
                text = text,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
