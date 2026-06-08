package com.edistrive.aura.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.data.model.Hospital
import com.edistrive.aura.ui.state.HospitalsMapViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import com.edistrive.aura.ui.components.AuraLoadingView
import com.edistrive.aura.ui.components.IosTopBar
import com.edistrive.aura.util.LocationProvider
import android.Manifest

private val POPULAR_CITIES = listOf("北京", "上海", "广州", "深圳", "杭州", "成都")
private val HOSPITAL_LEVELS = listOf("全部" to null, "三甲" to "三甲", "二甲" to "二甲", "其他" to "其他")
private val RADIUS_OPTIONS = listOf(1, 3, 5, 10, 20, 50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalsMapScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Hospital) -> Unit = {},
    viewModel: HospitalsMapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showCitySheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var cityInput by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            refreshTrigger++
        } else {
            viewModel.loadNearbyHospitals(39.9042, 116.4074)
        }
    }

    LaunchedEffect(Unit) {
        if (LocationProvider.isPermissionGranted(context)) {
            refreshTrigger++
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger == 0) return@LaunchedEffect
        viewModel.setLoading(true)
        val loc = LocationProvider.getCurrentLocation(context)
        if (loc != null) {
            viewModel.loadNearbyHospitals(loc.latitude, loc.longitude)
        } else {
            viewModel.loadNearbyHospitals(39.9042, 116.4074)
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            IosTopBar(
                title = "医院导航",
                onBack = onBack,
                background = Color(0xFFF5F7FA)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                ControlsCard(
                    isLoading = state.isLoading,
                    manualLocationName = state.manualLocationName,
                    selectedLevel = state.selectedLevel,
                    searchText = state.searchText,
                    onSearchTextChange = { viewModel.setSearchText(it) },
                    onOpenCitySheet = { showCitySheet = true },
                    onOpenFilterSheet = { showFilterSheet = true },
                    onRetryCurrentLocation = { refreshTrigger++ }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    state.isLoading && state.hospitals.isEmpty() -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = AuraTokens.Primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("正在定位附近医院...", fontSize = 14.sp, color = Color(0xFF909399))
                                }
                            }
                        }
                    }
                    state.error != null && state.hospitals.isEmpty() -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            EmptyListView(
                                icon = Icons.Default.ErrorOutline,
                                title = "加载失败",
                                subtitle = state.error ?: "",
                                onRefresh = { refreshTrigger++ }
                            )
                        }
                    }
                    state.filteredHospitals.isEmpty() && !state.isLoading -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            EmptyListView(
                                icon = Icons.Default.Map,
                                title = "暂时未获取到医院列表",
                                subtitle = if (state.searchText.isNotBlank() || state.selectedLevel != null)
                                    "未找到匹配的医院，请尝试其他筛选条件"
                                else "请稍后重试，或按城市搜索医院",
                                onRefresh = { refreshTrigger++ }
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            items(state.filteredHospitals, key = { it.id ?: it.hashCode().toString() }) { hospital ->
                                HospitalListCard(
                                    hospital = hospital,
                                    onClick = { onNavigateToDetail(hospital) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // City picker bottom sheet
    if (showCitySheet) {
        CityPickerSheet(
            cityInput = cityInput,
            onCityInputChange = { cityInput = it },
            onSearchCity = { city ->
                viewModel.geocodeAndSetCity(city)
                showCitySheet = false
                cityInput = ""
            },
            onDismiss = { showCitySheet = false }
        )
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        FilterSheet(
            selectedLevel = state.selectedLevel,
            selectedRadius = state.selectedRadius,
            onSelectLevel = { viewModel.setSelectedLevel(it) },
            onSelectRadius = { viewModel.setSelectedRadius(it) },
            onReset = {
                viewModel.setSelectedLevel(null)
                viewModel.setSelectedRadius(10)
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

// MARK: - Controls Card (matches iOS controlsCard)

@Composable
private fun ControlsCard(
    isLoading: Boolean,
    manualLocationName: String?,
    selectedLevel: String?,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onOpenCitySheet: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    onRetryCurrentLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row with location info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = AuraTokens.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "位置与筛选",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (isLoading) "正在根据当前位置查找"
                            else if (manualLocationName != null) "按城市：$manualLocationName"
                            else "按当前位置查找",
                            fontSize = 11.sp,
                            color = AuraTokens.Primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AuraTokens.Primary.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        if (selectedLevel != null) {
                            Text(
                                "等级：$selectedLevel",
                                fontSize = 11.sp,
                                color = Color(0xFF606266),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F2F5))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Switch back to location button
            if (!isLoading && manualLocationName != null) {
                TextButton(
                    onClick = onRetryCurrentLocation,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("切换回当前位置", fontSize = 11.sp)
                }
            }

            // Two action buttons — matches iOS button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // City search button (gradient)
                Button(
                    onClick = onOpenCitySheet,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
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
                                RoundedCornerShape(22.dp)
                            )
                    ) {
                        Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("按城市选择位置", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }

                // Filter button (outlined)
                OutlinedButton(
                    onClick = onOpenFilterSheet,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraTokens.Primary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraTokens.Primary.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("筛选医院", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Hospital name search
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F7FA)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = Color(0xFF909399), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchText,
                        onValueChange = onSearchTextChange,
                        placeholder = { Text("搜索医院名称", fontSize = 15.sp, color = Color(0xFFC0C4CC)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { onSearchTextChange("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "清除", modifier = Modifier.size(16.dp), tint = Color(0xFFC0C4CC))
                        }
                    }
                }
            }
        }
    }
}

// MARK: - City Picker Sheet (matches iOS cityPickerSheet)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityPickerSheet(
    cityInput: String,
    onCityInputChange: (String) -> Unit,
    onSearchCity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .wrapContentHeight()
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "按城市搜索",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color(0xFF606266))
                    }
                }

                // Search input
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F7FA)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color(0xFF909399), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = cityInput,
                            onValueChange = onCityInputChange,
                            placeholder = { Text("例如：北京、上海、杭州市西湖区", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        if (cityInput.isNotEmpty()) {
                            IconButton(onClick = { onCityInputChange("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = Color(0xFFC0C4CC))
                            }
                        }
                    }
                }

                // Popular cities
                Text(
                    "热门城市",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )

                var rowIndex = 0
                while (rowIndex * 2 < POPULAR_CITIES.size) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (i in 0 until 2) {
                            val idx = rowIndex * 2 + i
                            if (idx < POPULAR_CITIES.size) {
                                val city = POPULAR_CITIES[idx]
                                val isSelected = cityInput == city
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Brush.linearGradient(
                                                listOf(AuraTokens.Primary, AuraTokens.Primary2),
                                                Offset.Zero, Offset.Infinite
                                            ) else SolidColor(Color(0xFFF5F7FA)),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.Transparent else Color(0xFFE4E7ED),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onCityInputChange(city) }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        city,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White else AuraTokens.Primary
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    rowIndex++
                }

                // Search button
                Button(
                    onClick = {
                        val q = cityInput.trim()
                        if (q.isNotEmpty()) onSearchCity(q)
                    },
                    enabled = cityInput.isNotBlank(),
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
                                if (cityInput.isNotBlank())
                                    Brush.linearGradient(
                                        listOf(AuraTokens.Primary, AuraTokens.Primary2),
                                        Offset.Zero, Offset.Infinite
                                    )
                                else Brush.linearGradient(listOf(Color(0xFFC0C4CC), Color(0xFFC0C4CC))),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("搜索医院", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// MARK: - Filter Sheet (matches iOS filterSheet)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    selectedLevel: String?,
    selectedRadius: Int,
    onSelectLevel: (String?) -> Unit,
    onSelectRadius: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .wrapContentHeight()
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        onReset()
                        onDismiss()
                    }) {
                        Text("重置", fontSize = 14.sp, color = Color(0xFF606266))
                    }
                    Text(
                        "筛选条件",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onDismiss) {
                        Text("完成", fontSize = 14.sp, color = AuraTokens.Primary, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Hospital level selection
                Text(
                    "医院等级",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HOSPITAL_LEVELS.forEach { (label, value) ->
                        val isSelected = if (label == "全部") selectedLevel == null
                        else if (label == "其他") selectedLevel != null && selectedLevel != "三甲" && selectedLevel != "二甲"
                        else selectedLevel == value

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) AuraTokens.Primary.copy(alpha = 0.08f)
                                    else Color(0xFFF5F7FA)
                                )
                                .clickable {
                                    onSelectLevel(
                                        when (label) {
                                            "全部" -> null
                                            "其他" -> "其他"
                                            else -> value
                                        }
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) AuraTokens.Primary else Color(0xFF2C3E50)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = AuraTokens.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, Color(0xFFE4E7ED), CircleShape)
                                )
                            }
                        }
                    }
                }

                // Radius selection
                Text(
                    "搜索半径",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RADIUS_OPTIONS.forEach { radius ->
                        val isSelected = selectedRadius == radius
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) AuraTokens.Primary.copy(alpha = 0.08f)
                                    else Color(0xFFF5F7FA)
                                )
                                .clickable { onSelectRadius(radius) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${radius}km以内",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) AuraTokens.Primary else Color(0xFF2C3E50)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = AuraTokens.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, Color(0xFFE4E7ED), CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Hospital List Card (matches iOS HospitalListCard)

@Composable
private fun HospitalListCard(hospital: Hospital, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top: name + level + map icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        hospital.name ?: "未知医院",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF303133),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!hospital.level.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            hospital.level,
                            fontSize = 12.sp,
                            color = AuraTokens.Primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    Icons.Default.Map,
                    null,
                    tint = AuraTokens.Primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Middle: rating + distance
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if ((hospital.rating ?: 0.0) > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFE6A23C), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            String.format("%.1f", hospital.rating),
                            fontSize = 14.sp,
                            color = Color(0xFF303133)
                        )
                    }
                }
                if (hospital.distance != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = AuraTokens.Primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            formatDistance(hospital.distance),
                            fontSize = 14.sp,
                            color = Color(0xFF303133)
                        )
                    }
                }
                if (!hospital.phone.isNullOrBlank() && hospital.phone != "暂无电话") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Call, null, tint = Color(0xFF909399), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(hospital.phone, fontSize = 13.sp, color = Color(0xFF606266))
                    }
                }
            }

            // Bottom: address
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Place, null, tint = Color(0xFF909399), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    hospital.address ?: "",
                    fontSize = 13.sp,
                    color = Color(0xFF909399),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// MARK: - Empty List View

@Composable
private fun EmptyListView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onRefresh: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color(0xFFC0C4CC), modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF909399))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            subtitle,
            fontSize = 13.sp,
            color = Color(0xFFC0C4CC),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(50)) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("重新加载", fontSize = 14.sp)
        }
    }
}

private fun formatDistance(meters: Int): String {
    return if (meters < 1000) "${meters}米"
    else String.format("%.1f公里", meters / 1000.0)
}
