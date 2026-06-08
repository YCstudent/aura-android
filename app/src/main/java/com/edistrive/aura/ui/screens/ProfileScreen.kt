package com.edistrive.aura.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.edistrive.aura.ui.state.ProfileViewModel
import com.edistrive.aura.ui.components.StyledConfirmDialog
import com.edistrive.aura.ui.components.StyledInputDialog
import com.edistrive.aura.ui.components.StyledDatePicker
import com.edistrive.aura.ui.components.StyledOptionsDialog
import java.time.LocalDate

// iOS exact colors
private val Teal = Color(0xFF1A8080)
private val Teal2 = Color(0xFF2D9C9C)
private val DarkText = Color(0xFF2C3E50)
private val BodyText = Color(0xFF606266)
private val SubText = Color(0xFF909399)
private val BorderColor = Color(0xFFE4E7ED)
private val BgGray = Color(0xFFF5F7FA)
private val FieldBg = Color(0xFFF8F9FA)
private val Red = Color(0xFFFF3B30)
private val DangerColor = Color(0xFFF56C6C)
private val WarningColor = Color(0xFFE6A23C)
private val ChevronGray = Color(0xFFC0C4CC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onEmailBinding: () -> Unit = {},
    onPhoneBinding: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBirthDatePicker by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var showBloodTypeMenu by remember { mutableStateOf(false) }
    var showEditUsername by remember { mutableStateOf(false) }
    var showEditSignature by remember { mutableStateOf(false) }
    var editUsernameText by remember { mutableStateOf("") }
    var editSignatureText by remember { mutableStateOf("") }
    var cropImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { cropImageUri = it }
    }

    // Crop screen takes over the entire composable when active (matching iOS fullScreenCover)
    if (cropImageUri != null) {
        ImageCropperScreen(
            imageUri = cropImageUri!!,
            onCrop = { croppedFile ->
                cropImageUri = null
                viewModel.uploadAvatar(croppedFile)
            },
            onDismiss = { cropImageUri = null }
        )
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(BgGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronLeft, null, tint = Teal, modifier = Modifier.size(20.dp))
                }
                Text(
                    "个人中心",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning banner
                if (uiState.user?.profile_completed != true) {
                    Surface(
                        color = Color(0xFFFFF7E6),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = WarningColor, modifier = Modifier.size(18.dp))
                            Text("个人资料未完善，请填写完整信息", fontSize = 13.sp, color = WarningColor)
                        }
                    }
                }

                // 1. Basic Info
                IosSectionHeader(Icons.Default.Person, "基本信息", Teal)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        // Avatar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { imagePicker.launch("image/*") }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Photo, null, tint = SubText, modifier = Modifier.size(20.dp))
                            Text("头像", fontSize = 15.sp, color = DarkText)
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                val avatarUrl = uiState.user?.avatar?.let {
                                    if (it.startsWith("http")) it else "https://zgjcyl.com$it"
                                }
                                if (!avatarUrl.isNullOrBlank()) {
                                    key(uiState.avatarVersion) {
                                        val imageRequest = ImageRequest.Builder(context)
                                            .data(avatarUrl)
                                            .memoryCachePolicy(CachePolicy.DISABLED)
                                            .diskCachePolicy(CachePolicy.DISABLED)
                                            .build()
                                        AsyncImage(
                                            model = imageRequest, contentDescription = null,
                                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(BgGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, null, tint = Teal, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(start = 48.dp), color = BorderColor)

                        InfoRow(Icons.Default.Person, "用户名", uiState.user?.username ?: "", onClick = {
                            editUsernameText = uiState.user?.username ?: ""
                            showEditUsername = true
                        })
                        Divider(modifier = Modifier.padding(start = 48.dp), color = BorderColor)

                        InfoRow(Icons.Default.Edit, "个性签名", uiState.user?.signature?.ifBlank { "未设置" } ?: "未设置", onClick = {
                            editSignatureText = uiState.user?.signature ?: ""
                            showEditSignature = true
                        })
                        Divider(modifier = Modifier.padding(start = 48.dp), color = BorderColor)

                        InfoRow(Icons.Default.CalendarToday, "生日", uiState.user?.birth_date?.ifBlank { "未设置" } ?: "未设置", onClick = { showBirthDatePicker = true })
                        Divider(modifier = Modifier.padding(start = 48.dp), color = BorderColor)

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showGenderMenu = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Transgender, null, tint = SubText, modifier = Modifier.size(20.dp))
                            Text("性别", fontSize = 15.sp, color = DarkText)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(genderDisplayText(uiState.user?.gender), fontSize = 14.sp, color = SubText)
                            Icon(Icons.Default.ChevronRight, null, tint = ChevronGray, modifier = Modifier.size(14.dp))
                        }

                        if (!uiState.user?.created_at.isNullOrBlank()) {
                            Divider(modifier = Modifier.padding(start = 48.dp), color = BorderColor)
                            InfoRow(Icons.Default.DateRange, "注册时间", uiState.user?.created_at?.substringBefore("T") ?: "")
                        }
                    }
                }

                // 2. Account Binding
                IosSectionHeader(Icons.Default.Lock, "账号绑定", Teal)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        InfoRow(Icons.Default.Email, "邮箱", uiState.user?.email?.ifBlank { "未绑定" } ?: "未绑定", onClick = { onEmailBinding() })
                        Divider(modifier = Modifier.padding(start = 48.dp), color = BorderColor)
                        InfoRow(Icons.Default.Phone, "手机号", uiState.user?.phone?.ifBlank { "未绑定" } ?: "未绑定", onClick = { onPhoneBinding() })
                    }
                }

                // 3. Health Profile
                IosSectionHeader(Icons.Default.LocalHospital, "健康档案", Teal)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LabeledField("身高 (cm)", true, uiState.height, { viewModel.updateHeight(it) }, Modifier.weight(1f))
                            LabeledField("体重 (kg)", true, uiState.weight, { viewModel.updateWeight(it) }, Modifier.weight(1f))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("血型", fontSize = 13.sp, color = BodyText)
                                Text("*", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DangerColor)
                            }
                            Box {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { showBloodTypeMenu = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = FieldBg,
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            uiState.bloodType.ifBlank { "请选择" },
                                            fontSize = 14.sp,
                                            color = if (uiState.bloodType.isBlank()) SubText else DarkText
                                        )
                                        Icon(Icons.Default.ArrowDropDown, null, tint = SubText, modifier = Modifier.size(18.dp))
                                    }
                                }
                                DropdownMenu(expanded = showBloodTypeMenu, onDismissRequest = { showBloodTypeMenu = false }) {
                                    listOf("A", "B", "AB", "O", "unknown").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = { viewModel.updateBloodType(type); showBloodTypeMenu = false }
                                        )
                                    }
                                }
                            }
                        }

                        HealthField("既往疾病史", "若无请填\"无\"", true, uiState.medicalHistory) { viewModel.updateMedicalHistory(it) }
                        HealthField("过敏史", "如：花粉过敏，若无请填\"无\"", true, uiState.allergyHistory) { viewModel.updateAllergyHistory(it) }
                        HealthField("慢性疾病", "如：高血压、糖尿病等，若无请填\"无\"", true, uiState.chronicDiseases) { viewModel.updateChronicDiseases(it) }
                        HealthField("手术史", "若无请填\"无\"", true, uiState.surgeryHistory) { viewModel.updateSurgeryHistory(it) }
                        HealthField("长期用药史", "若无请填\"无\"", true, uiState.medicationHistory) { viewModel.updateMedicationHistory(it) }
                        HealthField("备注", "其他需要说明的健康信息", false, uiState.notes) { viewModel.updateNotes(it) }

                        Button(
                            onClick = { viewModel.saveHealthProfile() },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("保存健康档案", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // 4. Accept Invitation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, Teal.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Teal.copy(alpha = 0.08f), Teal.copy(alpha = 0.04f))))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Link, null, tint = Teal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("接受邀请", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Teal)
                    }
                }

                // 5. Logout
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, null, tint = DangerColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("退出登录", color = DangerColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                // 6. Delete account
                TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, null, tint = SubText, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("注销账号", color = SubText, fontSize = 14.sp)
                }

                Spacer(Modifier.height(30.dp))
            }
        }
    }

    // ---- Dialogs ----

    if (showGenderMenu) {
        StyledOptionsDialog(
            icon = Icons.Filled.Person,
            title = "选择性别",
            options = listOf("男" to "male", "女" to "female", "其他" to "other"),
            onSelect = { viewModel.updateBasicProfile(gender = it); showGenderMenu = false },
            onDismiss = { showGenderMenu = false }
        )
    }

    if (showBirthDatePicker) {
        val currentBirthDate = uiState.user?.birth_date?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }
        StyledDatePicker(
            title = "选择生日",
            initialDate = currentBirthDate,
            onConfirm = { date ->
                viewModel.updateBasicProfile(birthDate = date.toString())
                showBirthDatePicker = false
            },
            onDismiss = { showBirthDatePicker = false }
        )
    }

    if (showEditUsername) {
        StyledInputDialog(
            icon = Icons.Filled.Person,
            title = "修改用户名",
            value = editUsernameText,
            onValueChange = { editUsernameText = it },
            placeholder = "请输入新用户名",
            onConfirm = { viewModel.updateBasicProfile(username = editUsernameText); showEditUsername = false },
            onDismiss = { showEditUsername = false }
        )
    }

    if (showEditSignature) {
        StyledInputDialog(
            icon = Icons.Filled.Edit,
            title = "修改个性签名",
            value = editSignatureText,
            onValueChange = { editSignatureText = it },
            placeholder = "请输入个性签名",
            onConfirm = { viewModel.updateBasicProfile(signature = editSignatureText); showEditSignature = false },
            onDismiss = { showEditSignature = false }
        )
    }

    if (showLogoutDialog) {
        StyledConfirmDialog(
            icon = Icons.Filled.ExitToApp,
            title = "退出登录",
            message = "确定要退出当前账号吗？",
            confirmLabel = "退出",
            onConfirm = { showLogoutDialog = false; onLogout() },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showDeleteDialog) {
        StyledConfirmDialog(
            icon = Icons.Filled.DeleteForever,
            title = "注销账号",
            message = "注销后所有数据将被永久删除，无法恢复。确定要继续吗？",
            confirmLabel = "确定注销",
            onConfirm = { showDeleteDialog = false; viewModel.deleteAccount(); onDeleteAccount() },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// ---- Reusable Components ----

@Composable
private fun IosSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText)
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SubText, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 15.sp, color = DarkText)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = SubText)
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = ChevronGray, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    required: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 13.sp, color = BodyText)
            if (required) Text("*", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DangerColor)
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text("请输入", fontSize = 14.sp, color = SubText) },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = DarkText),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Teal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun HealthField(
    title: String,
    placeholder: String,
    required: Boolean,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 13.sp, color = BodyText)
            if (required) Text("*", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DangerColor)
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            minLines = 2,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = SubText) },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = DarkText),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Teal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
        )
    }
}

private fun genderDisplayText(gender: String?): String {
    return when (gender) {
        "male" -> "男"
        "female" -> "女"
        "other" -> "其他"
        else -> gender?.ifBlank { "未设置" } ?: "未设置"
    }
}
