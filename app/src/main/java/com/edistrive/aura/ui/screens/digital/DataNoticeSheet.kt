package com.edistrive.aura.ui.screens.digital

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DataNoticeSheet(
    show: Boolean,
    onConfirm: () -> Unit
) {
    if (!show) return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF3DDC97),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "使用前请知悉",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "医小智的回复由第三方大模型 DeepSeek 生成。为了给您更贴合的建议，每次提问时，App 会将下列信息发送给 DeepSeek：",
                        fontSize = 15.sp,
                        color = Color(0xFF333333),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // What's sent
                    val sentItems = listOf(
                        "您的提问内容和对话上下文",
                        "您的性别、年龄等基本信息",
                        "您的健康档案（既往病史、过敏史等）",
                        "您的近期就诊记录",
                        "您当前的用药信息"
                    )
                    sentItems.forEach { text ->
                        BulletItem(text, Color(0xFF1A8080))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // What's NOT sent
                    Text(
                        "以下信息不会发送给 DeepSeek：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555555)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val notSentItems = listOf(
                        "电话号码、邮箱地址",
                        "密码和头像",
                        "家庭成员详细信息"
                    )
                    notSentItems.forEach { text ->
                        BulletItem(text, Color(0xFF999999))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Disclaimer
                    Text(
                        "回复末尾会附上权威机构的参考来源。AI 生成的健康建议仅供参考，不能替代医生的专业诊断和治疗方案。",
                        fontSize = 13.sp,
                        color = Color(0xFF555555),
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC97)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("我已知悉，开始使用", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun BulletItem(text: String, tint: Color) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            fontSize = 14.sp,
            color = Color(0xFF333333),
            lineHeight = 20.sp
        )
    }
}
