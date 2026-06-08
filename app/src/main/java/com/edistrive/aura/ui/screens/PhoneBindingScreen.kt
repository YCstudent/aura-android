package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.state.ProfileViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import kotlinx.coroutines.delay

private val Teal = Color(0xFF1A8080)
private val Teal2 = Color(0xFF2D9C9C)
private val DarkText = Color(0xFF2C3E50)
private val SubText = Color(0xFF909399)
private val BgGray = Color(0xFFF5F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneBindingScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(countdown) {
        if (countdown <= 0L) return@LaunchedEffect
        while (countdown > 0L) {
            delay(1000)
            countdown -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("绑定手机号", color = DarkText, fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, null, tint = DarkText)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Phone input
            TextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = { Text("请输入手机号", fontSize = 14.sp, color = SubText) },
                leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BgGray,
                    unfocusedContainerColor = BgGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Code + send button
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = code,
                    onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
                    placeholder = { Text("验证码", fontSize = 14.sp, color = SubText) },
                    leadingIcon = { Icon(Icons.Default.Key, null, tint = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BgGray,
                        unfocusedContainerColor = BgGray,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        viewModel.sendChangePhoneCode(
                            phone = phone.trim(),
                            onSuccess = { msg ->
                                countdown = 60L
                                alertMessage = msg
                            },
                            onError = { alertMessage = it }
                        )
                    },
                    enabled = countdown == 0L && phone.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(48.dp).padding(top = 6.dp)
                ) {
                    Text(
                        text = if (countdown > 0L) "${countdown}s" else "获取",
                        color = if (countdown > 0L) Color.Gray else Teal,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // Bind button
            Button(
                onClick = {
                    isLoading = true
                    viewModel.changePhone(
                        newPhone = phone.trim(),
                        code = code.trim(),
                        onSuccess = {
                            isLoading = false
                            isSuccess = true
                            alertMessage = "手机号绑定成功"
                            viewModel.loadProfile()
                        },
                        onError = { msg ->
                            isLoading = false
                            alertMessage = msg
                        }
                    )
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Teal, Teal2)),
                            RoundedCornerShape(25.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("绑定手机号", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (alertMessage != null) {
        StyledInfoDialog(
            icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
            title = if (isSuccess) "成功" else "提示",
            message = alertMessage.orEmpty(),
            confirmLabel = "确定",
            iconColor = if (isSuccess) Color(0xFF67C23A) else AuraTokens.Primary,
            onDismiss = {
                alertMessage = null
                if (isSuccess) onBack()
            }
        )
    }
}
