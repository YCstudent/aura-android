package com.edistrive.aura.ui.screens.medication.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edistrive.aura.data.model.MedicationRequestModel
import com.edistrive.aura.ui.components.AuraExtraColors
import com.edistrive.aura.ui.theme.AuraTokens

@Composable
fun MedicationRequestCard(
    request: MedicationRequestModel,
    currentUserId: Int? = null,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusColor, statusLabel) = when (request.status) {
        "pending" -> AuraExtraColors.WarningOrange to (request.status_display ?: "等待中")
        "accepted" -> AuraExtraColors.SuccessGreen to (request.status_display ?: "已接受")
        "rejected" -> AuraExtraColors.ErrorRed to (request.status_display ?: "已拒绝")
        else -> AuraExtraColors.GrayLight to (request.status_display ?: request.status.orEmpty())
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .shadow(12.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.08f))
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // status row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.weight(1f))
                val isReceiver = currentUserId != null && request.to_user == currentUserId
                val direction = if (isReceiver) "来自：${request.from_username.orEmpty()}"
                else "发送给：${request.to_username.orEmpty()}"
                val dirIcon = if (isReceiver) Icons.Default.ArrowBack else Icons.Default.ArrowForward
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(dirIcon, contentDescription = null, tint = AuraExtraColors.GrayLight, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(direction, color = AuraExtraColors.GrayLight, fontSize = 13.sp)
                }
            }
            // medication info
            Column {
                Text(request.medication_name.orEmpty(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AuraExtraColors.DarkText)
                Spacer(modifier = Modifier.height(4.dp))
                val info = listOfNotNull(request.dosage, request.frequency).joinToString(" | ")
                if (info.isNotBlank()) {
                    Text(info, fontSize = 14.sp, color = AuraExtraColors.GrayText)
                }
            }
            // reminder times
            val times = request.reminder_times.orEmpty()
            if (times.isNotEmpty()) {
                Column {
                    Text("提醒时间", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AuraExtraColors.GrayText)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(times) { t ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AuraTokens.Primary.copy(alpha = 0.1f))
                                    .border(1.dp, AuraTokens.Primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(t, color = AuraTokens.Primary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            request.message?.takeIf { it.isNotBlank() }?.let { msg ->
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AuraExtraColors.GrayBg)
                    .padding(12.dp)) {
                    Text("留言：$msg", fontSize = 13.sp, color = AuraExtraColors.DarkText)
                }
            }
            // actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (request.status == "pending") {
                    val isReceiver = currentUserId != null && request.to_user == currentUserId
                    val isSender = currentUserId != null && request.from_user == currentUserId
                    when {
                        isReceiver -> {
                            GradientActionButton(
                                "接受",
                                start = AuraExtraColors.SuccessGreen,
                                end = AuraExtraColors.SuccessGreenLight,
                                onClick = onAccept,
                                modifier = Modifier.weight(1f)
                            )
                            GradientActionButton(
                                "拒绝",
                                start = AuraExtraColors.ErrorRed,
                                end = AuraExtraColors.ErrorRedLight,
                                onClick = onReject,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        isSender -> {
                            GradientActionButton(
                                "撤回",
                                start = AuraExtraColors.WarningOrange,
                                end = AuraExtraColors.WarningOrangeLight,
                                onClick = onWithdraw,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            GradientActionButton(
                                "接受",
                                start = AuraExtraColors.SuccessGreen,
                                end = AuraExtraColors.SuccessGreenLight,
                                onClick = onAccept,
                                modifier = Modifier.weight(1f)
                            )
                            GradientActionButton(
                                "拒绝",
                                start = AuraExtraColors.ErrorRed,
                                end = AuraExtraColors.ErrorRedLight,
                                onClick = onReject,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                        .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("删除记录", color = AuraExtraColors.ErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientActionButton(
    label: String,
    start: Color,
    end: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(listOf(start, end)))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
