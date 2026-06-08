package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.edistrive.aura.ui.theme.AuraTokens
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.state.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoRegister: () -> Unit,
    onResetPassword: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var loginType by remember { mutableIntStateOf(0) } // 0=password, 1=code

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var contact by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    var codeRemainingSeconds by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(codeRemainingSeconds) {
        if (codeRemainingSeconds <= 0L) return@LaunchedEffect
        while (codeRemainingSeconds > 0L) {
            kotlinx.coroutines.delay(1000)
            codeRemainingSeconds -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        BrandSection()
        Spacer(modifier = Modifier.height(30.dp))

        LoginTypeTabs(
            selected = loginType,
            onSelect = { loginType = it }
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (loginType == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InputField(
                    icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray) },
                    label = "用户名",
                    value = username,
                    onValueChange = { username = it }
                )
                InputField(
                    icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray) },
                    label = "密码",
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                tint = androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "忘记密码？",
                        fontSize = 13.sp,
                        color = AuraTokens.TextSecondary,
                        modifier = Modifier.clickable { onResetPassword() }
                    )
                }

                GradientPrimaryButton(
                    text = if (isLoading) "登录中..." else "登录",
                    enabled = !isLoading,
                    onClick = {
                        viewModel.passwordLogin(
                            username = username.trim(),
                            password = password,
                            onSuccess = onLoginSuccess,
                            onError = { errorMessage = it }
                        )
                    }
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InputField(
                    icon = { Icon(Icons.Filled.Email, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray) },
                    label = "手机号或邮箱",
                    value = contact,
                    onValueChange = { contact = it },
                    keyboardType = KeyboardType.Email
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        InputField(
                            icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray) },
                            label = "验证码",
                            value = code,
                            onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
                            keyboardType = KeyboardType.Number
                        )
                    }
                    Button(
                        onClick = {
                            if (codeRemainingSeconds > 0L) return@Button
                            viewModel.sendLoginCode(
                                contact = contact.trim(),
                                onSuccess = { codeRemainingSeconds = 60L },
                                onError = { errorMessage = it }
                            )
                        },
                        enabled = !isLoading && codeRemainingSeconds == 0L && contact.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.White),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = if (codeRemainingSeconds > 0L) "${codeRemainingSeconds}s" else "获取",
                            color = AuraTokens.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                GradientPrimaryButton(
                    text = if (isLoading) "登录中..." else "登录",
                    enabled = !isLoading,
                    onClick = {
                        viewModel.codeLogin(
                            contact = contact.trim(),
                            code = code.trim(),
                            onSuccess = onLoginSuccess,
                            onError = { errorMessage = it }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "还没有账号？", color = androidx.compose.ui.graphics.Color.Gray)
            Text(
                text = "立即注册",
                color = AuraTokens.Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clickable { onGoRegister() }
            )
        }
    }

    if (errorMessage != null) {
        StyledInfoDialog(
            icon = Icons.Default.Error,
            title = "登录失败",
            message = errorMessage.orEmpty(),
            confirmLabel = "知道了",
            iconColor = Color(0xFFF56C6C),
            onDismiss = { errorMessage = null }
        )
    }
}

@Composable
fun BrandSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = com.edistrive.aura.R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier
                .height(80.dp)
                .shadow(10.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "优雅素问",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = AuraTokens.TextPrimary
        )
        Text(
            text = "健康守护·安心无忧",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = AuraTokens.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LoginTypeTabs(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuraTokens.SurfaceAlt, RoundedCornerShape(AuraTokens.Radius12))
            .padding(4.dp)
    ) {
        TabChip(
            title = "密码登录",
            selected = selected == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f)
        )
        TabChip(
            title = "验证码登录",
            selected = selected == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f)
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
                color = if (selected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) AuraTokens.Primary else AuraTokens.TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun InputField(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = label, color = AuraTokens.TextSecondary) },
        leadingIcon = icon,
        trailingIcon = trailingIcon,
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AuraTokens.SurfaceAlt,
            unfocusedContainerColor = AuraTokens.SurfaceAlt,
            disabledContainerColor = AuraTokens.SurfaceAlt,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        shape = RoundedCornerShape(AuraTokens.Radius12),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GradientPrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(AuraTokens.Primary, AuraTokens.Primary2)
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, RoundedCornerShape(AuraTokens.Radius25)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
