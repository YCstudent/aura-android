package com.edistrive.aura.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import com.edistrive.aura.ui.screens.HomeScreen
import com.edistrive.aura.ui.screens.MeScreen
import com.edistrive.aura.ui.screens.InboxScreen
import com.edistrive.aura.ui.screens.ScheduleScreen
import com.edistrive.aura.ui.state.AppStateViewModel
import com.edistrive.aura.ui.theme.AuraTokens
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.clickable

@Composable
private fun TabItem(
    title: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) AuraTokens.Primary else Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                color = if (selected) AuraTokens.Primary else Color.Black.copy(alpha = 0.55f),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 10.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun MainTabsScreen(
    navController: NavHostController,
    onLogout: () -> Unit,
    appStateViewModel: AppStateViewModel = hiltViewModel()
) {
    val uiState by appStateViewModel.uiState.collectAsState()
    val selectedTab = uiState.selectedTab

    fun selectTab(newTab: Int) {
        appStateViewModel.setSelectedTab(newTab)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(bottom = 2.dp)
                    .padding(horizontal = 0.dp, vertical = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                val frosted = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF7F9FC).copy(alpha = 0.92f),
                        Color(0xFFFFFFFF).copy(alpha = 0.86f)
                    )
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .height(84.dp)
                        .shadow(18.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(frosted)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TabItem(
                        title = "主页",
                        selected = selectedTab == 0,
                        icon = Icons.Filled.Home,
                        onClick = { selectTab(0) }
                    )
                    TabItem(
                        title = "收件箱",
                        selected = selectedTab == 1,
                        icon = Icons.Filled.Chat,
                        onClick = { selectTab(1) }
                    )
                    TabItem(
                        title = "日程",
                        selected = selectedTab == 2,
                        icon = Icons.Filled.CalendarMonth,
                        onClick = { selectTab(2) }
                    )
                    TabItem(
                        title = "我的",
                        selected = selectedTab == 3,
                        icon = Icons.Filled.Person,
                        onClick = { selectTab(3) }
                    )
                    TabItem(
                        title = "医小智",
                        selected = false,
                        icon = Icons.Filled.AutoAwesome,
                        onClick = { navController.navigate(Routes.DIGITAL_HUMAN) }
                    )
                }
            }
        }
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            when (selectedTab) {
                0 -> HomeTabScreen(modifier = Modifier.fillMaxSize(), navController = navController)
                1 -> InboxTabScreen(modifier = Modifier.fillMaxSize(), navController = navController)
                2 -> ScheduleTabScreen(modifier = Modifier.fillMaxSize())
                3 -> MeTabScreen(modifier = Modifier.fillMaxSize(), navController = navController, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun HomeTabScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    HomeScreen(modifier = modifier, navController = navController)
}

@Composable
private fun InboxTabScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    InboxScreen(modifier = modifier, navController = navController)
}

@Composable
private fun ScheduleTabScreen(modifier: Modifier = Modifier) {
    ScheduleScreen(modifier = modifier)
}

@Composable
private fun MeTabScreen(modifier: Modifier = Modifier, navController: NavHostController, onLogout: () -> Unit) {
    MeScreen(modifier = modifier, navController = navController, onLogout = onLogout)
}
