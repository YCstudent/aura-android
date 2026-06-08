package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.state.AuthViewModel
import kotlinx.coroutines.delay

private val Teal = Color(0xFF1A8080)
private val Teal2 = Color(0xFF2D9C9C)
private val DarkText = Color(0xFF2C3E50)
private val SubText = Color(0xFF909399)
private val BorderColor = Color(0xFFE4E7ED)
private val BgGray = Color(0xFFF5F7FA)

@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var resetType by remember { mutableIntStateOf(0) } // 0=phone, 1=email
    var contact by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var countdown by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(countdown) {
        if (countdown <= 0L) return@LaunchedEffect
        while (countdown > 0L) {
            delay(1000)
            countdown -= 1
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronLeft, null, tint = Teal)
                }
                Text("返回", color = Teal, fontSize = 15.sp)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                // Brand
                BrandSection()

                Spacer(Modifier.height(20.dp))
                Text("重置密码", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                Spacer(Modifier.height(16.dp))

                // Type tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgGray, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    TabChip("手机号找回", resetType == 0, { resetType = 0; code = ""; countdown = 0 }, Modifier.weight(1f))
                    TabChip("邮箱找回", resetType == 1, { resetType = 1; code = ""; countdown = 0 }, Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // Form
                InputField(
                    icon = { Icon(if (resetType == 0) Icons.Default.Phone else Icons.Default.Email, null, tint = Color.Gray) },
                    label = if (resetType == 0) "请输入手机号" else "请输入邮箱",
                    value = contact,
                    onValueChange = { contact = it },
                    keyboardType = if (resetType == 0) KeyboardType.Phone else KeyboardType.Email
                )

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        InputField(
                            icon = { Icon(Icons.Default.Key, null, tint = Color.Gray) },
                            label = "验证码",
                            value = code,
                            onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
                            keyboardType = KeyboardType.Number
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.sendResetPasswordCode(
                                contact = contact.trim(),
                                onSuccess = { countdown = 60L },
                                onError = { errorMessage = it }
                            )
                        },
                        enabled = countdown == 0L && contact.isNotBlank() && !isLoading,
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

                Spacer(Modifier.height(12.dp))

                InputField(
                    icon = { Icon(Icons.Default.Lock, null, tint = Color.Gray) },
                    label = "新密码（至少6位）",
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                Spacer(Modifier.height(20.dp))

                // Reset button
                Button(
                    onClick = {
                        viewModel.resetPassword(
                            contact = contact.trim(),
                            code = code.trim(),
                            newPassword = newPassword,
                            onSuccess = { successMessage = "密码已重置，请使用新密码登录" },
                            onError = { errorMessage = it }
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
                            Text("重置密码", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(30.dp))

                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Divider(color = BgGray)
                }
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("记起密码了？", color = Color.Gray, fontSize = 14.sp)
                    Text(
                        "返回登录",
                        color = Teal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp).clickable(onClick = onBack)
                    )
                }

                Spacer(Modifier.height(30.dp))
            }
        }
    }

    // Dialogs
    if (errorMessage != null) {
        StyledInfoDialog(
            icon = Icons.Default.Error,
            title = "重置失败",
            message = errorMessage.orEmpty(),
            confirmLabel = "确定",
            iconColor = Color(0xFFF56C6C),
            onDismiss = { errorMessage = null }
        )
    }

    if (successMessage != null) {
        StyledInfoDialog(
            icon = Icons.Default.CheckCircle,
            title = "重置成功",
            message = successMessage.orEmpty(),
            confirmLabel = "去登录",
            iconColor = Color(0xFF67C23A),
            onDismiss = {
                successMessage = null
                onSuccess()
            }
        )
    }
}

@Composable
private fun TabChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (selected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Teal else SubText,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun InputField(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    var visible by remember { mutableStateOf(!isPassword) }
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = label, color = SubText, fontSize = 14.sp) },
        leadingIcon = icon,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null,
                        tint = Color.Gray
                    )
                }
            }
        } else null,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
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
}
