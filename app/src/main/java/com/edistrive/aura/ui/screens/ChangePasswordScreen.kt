package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.ChangePasswordRequest
import com.edistrive.aura.ui.components.IosTopBar
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.state.ChangePasswordViewModel
import com.edistrive.aura.ui.theme.AuraTokens

@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.success) {
        if (state.success) {
            alertMessage = "密码修改成功"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        IosTopBar(title = "修改密码", onBack = onBack, background = Color(0xFFF5F7FA))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current password — matches iOS Section "当前密码"
            Text("当前密码", fontSize = 13.sp, color = Color(0xFF909399))
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                placeholder = { Text("当前密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isLoading
            )

            // New password — matches iOS Section "新密码"
            Text("新密码", fontSize = 13.sp, color = Color(0xFF909399))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = { Text("新密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isLoading
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("确认密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isLoading
            )

            // Hint — matches iOS footer "密码长度不能少于6位"
            Text("密码长度不能少于6位", fontSize = 12.sp, color = Color(0xFF909399))

            state.error?.let {
                Text(it, color = Color(0xFFF56C6C), fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Confirm button — matches iOS toolbar "确定" button
            Button(
                onClick = {
                    when {
                        oldPassword.isBlank() || newPassword.isBlank() -> alertMessage = "请填写完整信息"
                        newPassword != confirmPassword -> alertMessage = "两次输入的密码不一致"
                        newPassword.length < 6 -> alertMessage = "密码长度不能少于6位"
                        else -> viewModel.changePassword(
                            ChangePasswordRequest(old_password = oldPassword, new_password = newPassword)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuraTokens.Primary),
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("确定", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Alert — matches iOS .alert("提示")
    alertMessage?.let { msg ->
        val isSuccess = msg == "密码修改成功"
        StyledInfoDialog(
            icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
            title = "提示",
            message = msg,
            confirmLabel = "确定",
            iconColor = if (isSuccess) Color(0xFF67C23A) else AuraTokens.Primary,
            onDismiss = {
                alertMessage = null
                if (isSuccess) onBack()
            }
        )
    }
}
