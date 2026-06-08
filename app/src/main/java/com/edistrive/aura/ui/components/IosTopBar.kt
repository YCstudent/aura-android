package com.edistrive.aura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edistrive.aura.ui.theme.AuraTokens

/**
 * iOS-style large-style nav bar:
 *  - status-bar inset baked in
 *  - 44dp content row, white background, hairline 0.5dp divider
 *  - centered title, chevron-left back button on the leading edge
 *  - optional trailing slot for actions
 *
 *  Use this in place of Material3 [TopAppBar] across the app.
 */
@Composable
fun IosTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    background: Color = Color.White,
    titleColor: Color = AuraExtraColors.DarkText,
    tintColor: Color = AuraTokens.Primary,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            // Leading
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            ) {
                if (leading != null) {
                    leading()
                } else if (onBack != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "返回",
                            tint = tintColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Text("返回", color = tintColor, fontSize = 17.sp)
                    }
                }
            }

            // Title — centered
            Text(
                text = title,
                color = titleColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )

            // Trailing
            if (trailing != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    trailing()
                }
            }
        }
        if (showDivider) {
            Divider(
                color = AuraExtraColors.GrayLightest.copy(alpha = 0.6f),
                thickness = 0.5.dp
            )
        }
    }
}
