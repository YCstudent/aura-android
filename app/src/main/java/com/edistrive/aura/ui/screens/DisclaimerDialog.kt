package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DisclaimerDialog(
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* block */ },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A8080)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "我已阅读并同意")
            }
        },
        title = {
            Text(
                text = "重要声明",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "优雅素问是一款健康信息管理工具，提供的所有内容仅供健康管理参考，不构成医疗诊断、治疗建议或专业医疗意见。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Bullet("本应用不能替代医生的专业判断")
                Bullet("AI提供的建议仅供参考，不具有诊断价值")
                Bullet("如有健康问题，请及时就医并咨询专业医生")
                Bullet("紧急情况请拨打120急救电话")
                Bullet("用药请遵医嘱，不要自行调整用药")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "使用本应用即表示您理解并同意上述声明。",
                    color = Color(0xFF1A8080),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun Bullet(text: String) {
    Text(text = "• $text")
}
