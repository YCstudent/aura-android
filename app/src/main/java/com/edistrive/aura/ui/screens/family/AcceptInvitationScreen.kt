package com.edistrive.aura.ui.screens.family

import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.PreviewMemberInfo
import com.edistrive.aura.ui.components.AvatarWithGenderBadge
import com.edistrive.aura.ui.components.StyledInfoDialog
import com.edistrive.aura.ui.state.FamilyViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.util.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptInvitationScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    var code by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<PreviewMemberInfo?>(null) }
    var creatorName by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var accepting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            com.edistrive.aura.ui.components.IosTopBar(
                title = "接受邀请",
                onBack = onBack,
                background = Color(0xFFF5F7FA)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card — matching iOS style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AuraTokens.Primary.copy(alpha = 0.1f),
                                AuraTokens.Primary2.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        AuraTokens.Primary.copy(alpha = 0.2f),
                                        AuraTokens.Primary2.copy(alpha = 0.2f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = AuraTokens.Primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "接受邀请",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "输入邀请码，关联家人的健康档案",
                        fontSize = 14.sp,
                        color = Color(0xFF909399),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (preview == null) {
                // Input card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "邀请码",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF606266)
                        )

                        OutlinedTextField(
                            value = code,
                            onValueChange = { if (it.length <= 40) code = it.trim() },
                            placeholder = { Text("请输入32位邀请码", fontSize = 15.sp, color = Color(0xFFC0C4CC)) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = null,
                                    tint = Color(0xFF909399),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraTokens.Primary,
                                unfocusedBorderColor = Color(0xFFE4E7ED),
                                focusedContainerColor = Color(0xFFF5F7FA),
                                unfocusedContainerColor = Color(0xFFF5F7FA)
                            )
                        )

                        Text(
                            "请向家人索取邀请码，或从消息中粘贴",
                            fontSize = 12.sp,
                            color = Color(0xFF909399)
                        )

                        Button(
                            onClick = {
                                if (code.length != 32) {
                                    message = "邀请码长度为32位"
                                    return@Button
                                }
                                checking = true
                                preview = null
                                creatorName = null
                                viewModel.previewInvitation(code) { resp, errMsg ->
                                    checking = false
                                    if (resp?.success == true) {
                                        preview = resp.member
                                        creatorName = resp.creator_name
                                    } else {
                                        message = errMsg ?: "邀请码无效或已过期"
                                    }
                                }
                            },
                            enabled = code.isNotBlank() && !checking,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(AuraTokens.Primary, AuraTokens.Primary2),
                                            start = Offset.Zero, end = Offset.Infinite
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (checking) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    if (checking) "查询中..." else "查询邀请信息",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Help card — matching iOS 4-step instructions
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Help,
                                contentDescription = null,
                                tint = AuraTokens.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "如何获取邀请码？",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2C3E50)
                            )
                        }

                        HelpStep("1", "向家人索取", "让家人在\"家庭成员详情\"页面，点击\"邀请关联账号\"生成邀请码")
                        HelpStep("2", "从消息中获取", "家人可能通过微信、短信等方式发送给您")
                        HelpStep("3", "输入邀请码", "将32位邀请码粘贴到上方输入框，点击查询")
                        HelpStep("4", "确认关联", "核对信息无误后，点击\"确认关联\"完成关联")
                    }
                }
            }

            // Preview card — matching iOS style
            preview?.let { p ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Success header
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF0F9EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF67C23A),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "找到匹配的邀请",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "来自 ${creatorName ?: "家人"} 的家庭成员关联邀请",
                                fontSize = 14.sp,
                                color = Color(0xFF909399)
                            )
                        }

                        // Member info preview
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8F9FA))
                                .padding(16.dp)
                        ) {
                            AvatarWithGenderBadge(
                                avatarUrl = p.avatar,
                                initial = p.name?.firstOrNull()?.toString() ?: "家",
                                gender = p.gender,
                                sizeDp = 72,
                                badgeDp = 24
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    p.name.orEmpty(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C3E50)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when (p.gender) {
                                        "male" -> InfoTag("男", AuraTokens.Primary, Color.White)
                                        "female" -> InfoTag("女", AuraTokens.Primary, Color.White)
                                    }
                                    p.age?.let { InfoTag("${it}岁", Color(0xFFE4E7ED), Color(0xFF606266)) }
                                }
                                val details = listOfNotNull(
                                    p.birth_date?.let { DateFormat.chineseFromIso(it) }.takeIf { it != null && it != "-" },
                                    p.phone?.takeIf { it.isNotBlank() }
                                )
                                if (details.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        details.joinToString(" · "),
                                        fontSize = 12.sp,
                                        color = Color(0xFF909399)
                                    )
                                }
                            }
                        }

                        // Benefits card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuraTokens.Primary.copy(alpha = 0.05f))
                                .padding(16.dp)
                        ) {
                            Text(
                                "关联后您可以：",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AuraTokens.Primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            BenefitItem(Icons.Default.Visibility, "查看健康档案")
                            Spacer(modifier = Modifier.height(6.dp))
                            BenefitItem(Icons.Default.Description, "添加病历记录")
                            Spacer(modifier = Modifier.height(6.dp))
                            BenefitItem(Icons.Default.TrendingUp, "生成健康报告")
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    preview = null
                                    creatorName = null
                                    code = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFE4E7ED),
                                    contentColor = Color(0xFF606266)
                                )
                            ) {
                                Text("取消", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    accepting = true
                                    viewModel.acceptInvitation(code) { ok, msg ->
                                        accepting = false
                                        message = msg ?: if (ok) "关联成功！" else "关联失败"
                                        if (ok) {
                                            onSuccess()
                                        }
                                    }
                                },
                                enabled = !accepting,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(AuraTokens.Primary, AuraTokens.Primary2),
                                                start = Offset.Zero, end = Offset.Infinite
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    if (accepting) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        if (accepting) "处理中..." else "确认关联",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (message != null) {
        StyledInfoDialog(
            icon = Icons.Default.Info,
            title = "提示",
            message = message.orEmpty(),
            confirmLabel = "确定",
            onDismiss = { message = null }
        )
    }
}

@Composable
private fun HelpStep(number: String, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AuraTokens.Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                description,
                fontSize = 13.sp,
                color = Color(0xFF909399),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun BenefitItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = AuraTokens.Primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AuraTokens.Primary
        )
    }
}

@Composable
private fun InfoTag(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 12.sp, color = fg)
    }
}
