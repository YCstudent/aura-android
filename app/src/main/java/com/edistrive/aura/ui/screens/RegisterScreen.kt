package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.ui.components.StyledInfoDialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.ui.state.AuthViewModel

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onUserAgreement: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var registerType by remember { mutableIntStateOf(0) } // 0=phone, 1=email

    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var codeRemainingSeconds by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var agreedToPrivacy by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(codeRemainingSeconds) {
        if (codeRemainingSeconds <= 0L) return@LaunchedEffect
        while (codeRemainingSeconds > 0L) {
            kotlinx.coroutines.delay(1000)
            codeRemainingSeconds -= 1
        }
    }

    val canSendCode = if (registerType == 0) phone.isNotBlank() else email.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        BrandSection()
        Spacer(modifier = Modifier.height(30.dp))

        RegisterTypeTabs(selected = registerType, onSelect = {
            registerType = it
            code = ""
            codeRemainingSeconds = 0L
        })
        Spacer(modifier = Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            InputField(
                icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray) },
                label = "用户名（3-10位，字母/中文）",
                value = username,
                onValueChange = { username = it.take(10) }
            )

            if (registerType == 0) {
                InputField(
                    icon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray) },
                    label = "手机号",
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(11) },
                    keyboardType = KeyboardType.Phone
                )
            } else {
                InputField(
                    icon = { Icon(Icons.Filled.Email, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray) },
                    label = "邮箱",
                    value = email,
                    onValueChange = { email = it.trim() },
                    keyboardType = KeyboardType.Email
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    InputField(
                        icon = { Icon(Icons.Filled.Key, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray) },
                        label = if (registerType == 0) "短信验证码" else "邮箱验证码",
                        value = code,
                        onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
                        keyboardType = KeyboardType.Number
                    )
                }
                Button(
                    onClick = {
                        if (codeRemainingSeconds > 0L) return@Button
                        val contact = if (registerType == 0) phone.trim() else email.trim()
                        viewModel.sendRegisterCode(
                            contact = contact,
                            onSuccess = { codeRemainingSeconds = 60L },
                            onError = { errorMessage = it }
                        )
                    },
                    enabled = !isLoading && codeRemainingSeconds == 0L && canSendCode,
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

            InputField(
                icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Gray) },
                label = "密码（至少6位）",
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                IconButton(
                    onClick = { agreedToPrivacy = !agreedToPrivacy },
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(
                                1.5.dp,
                                if (agreedToPrivacy) AuraTokens.Primary else androidx.compose.ui.graphics.Color.Gray,
                                RoundedCornerShape(3.dp)
                            )
                            .background(
                                if (agreedToPrivacy) AuraTokens.Primary else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(3.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (agreedToPrivacy) {
                            Text(
                                "✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
                val annotatedText = buildAnnotatedString {
                    withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color.Gray, fontSize = 13.sp)) {
                        append("我已阅读并同意")
                    }
                    pushStringAnnotation(tag = "privacy", annotation = "privacy")
                    withStyle(SpanStyle(color = AuraTokens.Primary, fontSize = 13.sp)) {
                        append("《隐私政策》")
                    }
                    pop()
                    withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color.Gray, fontSize = 13.sp)) {
                        append("和")
                    }
                    pushStringAnnotation(tag = "terms", annotation = "terms")
                    withStyle(SpanStyle(color = AuraTokens.Primary, fontSize = 13.sp)) {
                        append("《用户协议》")
                    }
                    pop()
                }
                androidx.compose.foundation.text.ClickableText(
                    text = annotatedText,
                    modifier = Modifier.padding(start = 4.dp),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations("privacy", offset, offset).firstOrNull()?.let {
                            onPrivacyPolicy()
                        }
                        annotatedText.getStringAnnotations("terms", offset, offset).firstOrNull()?.let {
                            onUserAgreement()
                        }
                    }
                )
            }

            GradientPrimaryButton(
                text = if (isLoading) "注册中..." else "注册",
                enabled = !isLoading && agreedToPrivacy,
                onClick = {
                    viewModel.register(
                        username = username.trim(),
                        password = password,
                        phone = if (registerType == 0) phone.trim().ifBlank { null } else null,
                        email = if (registerType == 1) email.trim().ifBlank { null } else null,
                        code = code.trim(),
                        onSuccess = {
                            showSuccessDialog = true
                            onRegisterSuccess()
                        },
                        onError = { errorMessage = it }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "已有账户？", color = androidx.compose.ui.graphics.Color.Gray)
            Text(
                text = "立即登录",
                color = AuraTokens.Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clickable { onBackToLogin() }
            )
        }
    }

    if (errorMessage != null) {
        StyledInfoDialog(
            icon = Icons.Default.Error,
            title = "注册失败",
            message = errorMessage.orEmpty(),
            confirmLabel = "知道了",
            iconColor = Color(0xFFF56C6C),
            onDismiss = { errorMessage = null }
        )
    }

    if (showSuccessDialog) {
        StyledInfoDialog(
            icon = Icons.Default.CheckCircle,
            title = "注册成功",
            message = "您的账号已注册成功，请登录",
            confirmLabel = "去登录",
            iconColor = Color(0xFF67C23A),
            onDismiss = {
                showSuccessDialog = false
                onBackToLogin()
            }
        )
    }
}

@Composable
private fun RegisterTypeTabs(
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
            title = "手机号注册",
            selected = selected == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f)
        )
        TabChip(
            title = "邮箱注册",
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
