package com.edistrive.aura.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edistrive.aura.data.model.Hospital
import com.edistrive.aura.ui.components.AmapView
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.util.LocationProvider
import com.edistrive.aura.util.MapService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalMapDetailScreen(
    hospital: Hospital,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showNavigationSheet by remember { mutableStateOf(false) }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }

    val coordinate = remember(hospital.location) { parseCoordinate(hospital.location) }

    LaunchedEffect(Unit) {
        val location = withContext(Dispatchers.IO) {
            LocationProvider.getCurrentLocation(context)
        }
        if (location != null) {
            userLat = location.latitude
            userLng = location.longitude
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // AMap — fills top half
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (coordinate != null) {
                AmapView(
                    modifier = Modifier.fillMaxSize(),
                    lat = coordinate.second,
                    lng = coordinate.first,
                    title = hospital.name ?: "",
                    userLat = userLat,
                    userLng = userLng
                )
            } else {
                // No coordinate — show placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF0F2F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Map,
                            null,
                            tint = Color(0xFFC0C4CC),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("暂无位置信息", fontSize = 14.sp, color = Color(0xFF909399))
                    }
                }
            }
        }

        // Back arrow overlay on map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.ArrowBack, "返回", tint = Color(0xFF303133), modifier = Modifier.size(20.dp))
            }
        }

        // Bottom card — info + navigation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hospital name + level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            hospital.name ?: "未知医院",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF303133)
                        )
                    }
                    if (!hospital.level.isNullOrBlank()) {
                        Text(
                            hospital.level,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(AuraTokens.Primary, AuraTokens.Primary2),
                                        Offset.Zero, Offset.Infinite
                                    )
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Rating + distance
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    if ((hospital.rating ?: 0.0) > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFE6A23C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                String.format("%.1f", hospital.rating),
                                fontSize = 15.sp,
                                color = Color(0xFF303133)
                            )
                        }
                    }
                    if (hospital.distance != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = AuraTokens.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(formatDistanceText(hospital.distance), fontSize = 15.sp, color = Color(0xFF303133))
                        }
                    }
                }

                Divider(color = Color(0xFFF0F0F0))

                // Address
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Place, null, tint = Color(0xFF909399), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        hospital.address ?: "",
                        fontSize = 14.sp,
                        color = Color(0xFF606266),
                        lineHeight = 20.sp
                    )
                }

                // Phone
                if (!hospital.phone.isNullOrBlank() && hospital.phone != "暂无电话") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Call, null, tint = Color(0xFF909399), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            hospital.phone,
                            fontSize = 14.sp,
                            color = Color(0xFF606266),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hospital.phone}"))
                            context.startActivity(intent)
                        }) {
                            Text("拨打", color = AuraTokens.Primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Navigation button
                Button(
                    onClick = { showNavigationSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
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
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("到这去", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Safe area for nav bar
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    if (showNavigationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNavigationSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(36.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFD1D1D6))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header — matches iOS exactly
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "选择导航方式",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "使用您偏好的地图应用",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { showNavigationSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "关闭",
                            tint = Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MapService.entries.forEach { service ->
                        NavigationOptionCard(
                            service = service,
                            isInstalled = service.isInstalled(context),
                            onClick = {
                                if (!service.isInstalled(context)) {
                                    service.openOrDownload(context)
                                } else {
                                    MapService.setPreferred(context, service)
                                    val coord = coordinate
                                    if (coord != null) {
                                        service.navigate(
                                            context,
                                            lat = coord.second,
                                            lng = coord.first,
                                            name = hospital.name ?: "医院"
                                        )
                                    }
                                }
                                showNavigationSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationOptionCard(
    service: MapService,
    isInstalled: Boolean,
    onClick: () -> Unit
) {
    val serviceColor = Color(service.color)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF2F2F7)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(serviceColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    null,
                    tint = serviceColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    service.displayName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF303133)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isInstalled) "已安装 · 点击导航" else "未安装 · 点击下载",
                    fontSize = 13.sp,
                    color = if (isInstalled) Color(0xFF34C759) else Color(0xFFFF9500)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun parseCoordinate(location: String?): Pair<Double, Double>? {
    if (location.isNullOrBlank()) return null
    val parts = location.split(",").map { it.trim() }
    if (parts.size != 2) return null
    val lng = parts[0].toDoubleOrNull() ?: return null
    val lat = parts[1].toDoubleOrNull() ?: return null
    return Pair(lng, lat)
}

private fun formatDistanceText(meters: Int): String {
    return if (meters < 1000) "${meters}米" else String.format("%.1f公里", meters / 1000.0)
}
