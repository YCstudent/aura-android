package com.edistrive.aura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val Teal = Color(0xFF1A8080)
private val Teal2 = Color(0xFF2D9C9C)
private val TealLight = Color(0xFFE8F5F5)
private val DarkText = Color(0xFF1A1A1A)
private val SubText = Color(0xFF909399)
private val BorderColor = Color(0xFFE4E7ED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledDatePicker(
    title: String = "选择生日",
    initialDate: LocalDate? = null,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(initialDate ?: today)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = { Box(Modifier.padding(top = 12.dp)) {
            Box(
                Modifier
                    .width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD0D0D0))
            )
        }}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Selected date display
            if (selectedDate != null) {
                Text(
                    selectedDate.toString(),
                    fontSize = 15.sp,
                    color = Teal,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))
            }

            // Month navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year left
                IconButton(onClick = { currentYearMonth = currentYearMonth.minusYears(1) }) {
                    Icon(Icons.Default.ChevronLeft, "上一年", tint = Teal, modifier = Modifier.size(24.dp))
                }
                // Month left
                IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, "上一月", tint = Teal, modifier = Modifier.size(20.dp))
                }

                Text(
                    "${currentYearMonth.year}年 ${currentYearMonth.monthValue}月",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText
                )

                // Month right
                IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, "下一月", tint = Teal, modifier = Modifier.size(20.dp))
                }
                // Year right
                IconButton(onClick = { currentYearMonth = currentYearMonth.plusYears(1) }) {
                    Icon(Icons.Default.ChevronRight, "下一年", tint = Teal, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("一", "二", "三", "四", "五", "六", "日")
                daysOfWeek.forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SubText
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Calendar grid
            val days = generateCalendarDays(currentYearMonth)
            val columns = 7
            val rows = (days.size + columns - 1) / columns

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < days.size) {
                            val day = days[index]
                            CalendarDayCell(
                                day = day,
                                isSelected = day != null && selectedDate != null &&
                                    day == selectedDate!!.dayOfMonth &&
                                    currentYearMonth == YearMonth.from(selectedDate),
                                isToday = day != null &&
                                    day == today.dayOfMonth &&
                                    currentYearMonth == YearMonth.from(today),
                                onClick = {
                                    if (day != null) {
                                        selectedDate = currentYearMonth.atDay(day)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Confirm button — teal gradient
            Button(
                onClick = { selectedDate?.let { onConfirm(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                enabled = selectedDate != null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Teal, Teal2)),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("确定", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Cancel button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text("取消", color = SubText, fontSize = 16.sp)
            }
        }
    }
}

private data class CalendarDay(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean
)

private fun generateCalendarDays(yearMonth: YearMonth): List<Int?> {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    // Monday = 1 ... Sunday = 7; we display Mon-Sun
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Mon, 7=Sun

    val result = mutableListOf<Int?>()
    // Leading empties
    repeat(firstDayOfWeek - 1) { result.add(null) }
    // Days
    for (day in 1..daysInMonth) { result.add(day) }
    return result
}

@Composable
private fun CalendarDayCell(
    day: Int?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .then(
                if (day != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            // Selected background circle
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Teal)
                )
            } else if (isToday) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Teal, CircleShape)
                )
            }

            Text(
                day.toString(),
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isToday -> Teal
                    else -> DarkText
                },
                textAlign = TextAlign.Center
            )
        }
    }
}
