package com.edistrive.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edistrive.aura.ui.components.IosTopBar

// MARK: - Data Models

enum class PolicyType(val title: String, val fileName: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PRIVACY("隐私政策", "PrivacyPolicy.md", "我们重视您的隐私保护", Icons.Filled.Security),
    USER_AGREEMENT("用户服务协议", "UserAgreement.md", "使用服务前请仔细阅读", Icons.Filled.Description)
}

data class MarkdownSection(
    val level: Int,
    val type: SectionType,
    val content: String
)

enum class SectionType {
    HEADING, PARAGRAPH, LIST_ITEM, BOLD, DIVIDER, TABLE
}

data class SectionGroup(
    val heading: MarkdownSection?,
    val items: List<MarkdownSection>
)

// MARK: - Main Composable

@Composable
fun PolicyViewScreen(title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val policyType = if (title == "隐私政策") PolicyType.PRIVACY else PolicyType.USER_AGREEMENT

    val sections = remember {
        parseMarkdown(context, policyType.fileName)
    }

    val groups = remember(sections) {
        groupSections(sections)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        IosTopBar(title = title, onBack = onBack, background = Color(0xFFF5F7FA))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(
                icon = policyType.icon,
                title = title,
                subtitle = policyType.subtitle
            )

            if (sections.isEmpty()) {
                EmptyState()
            } else {
                groups.forEach { group ->
                    SectionGroupCard(group = group)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// MARK: - Markdown Parsing

private fun parseMarkdown(context: android.content.Context, fileName: String): List<MarkdownSection> {
    val content = try {
        context.assets.open(fileName).bufferedReader().readText()
    } catch (e: Exception) {
        return emptyList()
    }

    val sections = mutableListOf<MarkdownSection>()
    val lines = content.split("\n")
    var i = 0

    while (i < lines.size) {
        val trimmed = lines[i].trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // Table detection
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            val tableRows = mutableListOf<List<String>>()
            while (i < lines.size) {
                val row = lines[i].trim()
                if (!(row.startsWith("|") && row.endsWith("|"))) break

                val separatorClean = row.replace("|", "").replace("-", "").replace(" ", "").replace(":", "")
                if (separatorClean.isEmpty()) {
                    i++
                    continue
                }

                val cells = row.split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                tableRows.add(cells)
                i++
            }

            if (tableRows.isNotEmpty()) {
                val tableContent = tableRows.joinToString("\n") { it.joinToString("||") }
                sections.add(MarkdownSection(0, SectionType.TABLE, tableContent))
            }
            continue
        }

        when {
            trimmed.startsWith("### ") -> {
                val text = trimmed.removePrefix("### ")
                sections.add(MarkdownSection(3, SectionType.HEADING, text))
            }
            trimmed.startsWith("## ") -> {
                val text = trimmed.removePrefix("## ")
                sections.add(MarkdownSection(2, SectionType.HEADING, text))
            }
            trimmed.startsWith("# ") -> {
                val text = trimmed.removePrefix("# ")
                sections.add(MarkdownSection(1, SectionType.HEADING, text))
            }
            trimmed.startsWith("- ") -> {
                val text = trimmed.removePrefix("- ").replace("**", "")
                sections.add(MarkdownSection(0, SectionType.LIST_ITEM, text))
            }
            trimmed.startsWith("---") || trimmed.startsWith("***") -> {
                sections.add(MarkdownSection(0, SectionType.DIVIDER, ""))
            }
            trimmed.startsWith("**") && trimmed.endsWith("**") -> {
                val text = trimmed.removeSurrounding("**")
                sections.add(MarkdownSection(0, SectionType.BOLD, text))
            }
            else -> {
                val text = trimmed.replace("**", "")
                sections.add(MarkdownSection(0, SectionType.PARAGRAPH, text))
            }
        }
        i++
    }

    return sections
}

// MARK: - Section Grouping

private fun groupSections(sections: List<MarkdownSection>): List<SectionGroup> {
    val groups = mutableListOf<SectionGroup>()
    var currentGroup: SectionGroup? = null

    for (section in sections) {
        if (section.type == SectionType.HEADING && section.level == 2) {
            if (currentGroup != null) {
                groups.add(currentGroup)
            }
            currentGroup = SectionGroup(section, emptyList())
        } else if (section.type == SectionType.HEADING && section.level == 1) {
            if (currentGroup != null) {
                groups.add(currentGroup)
            }
            groups.add(SectionGroup(null, listOf(section)))
            currentGroup = null
        } else {
            if (currentGroup != null) {
                currentGroup = currentGroup.copy(items = currentGroup.items + section)
            } else {
                currentGroup = SectionGroup(null, listOf(section))
            }
        }
    }

    if (currentGroup != null) {
        groups.add(currentGroup)
    }

    return groups
}

// MARK: - Header Card

@Composable
private fun HeaderCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A8080), Color(0xFF2AB5B5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(24.dp), tint = Color.White)
            }

            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF303133))
            Text(subtitle, fontSize = 14.sp, color = Color(0xFF909399))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Filled.Schedule, null, Modifier.size(11.dp), tint = Color(0xFFB0B5BD))
                Text("最后更新：2025年1月", fontSize = 12.sp, color = Color(0xFFB0B5BD))
            }
        }
    }
}

// MARK: - Empty State

@Composable
private fun EmptyState() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Description,
                null,
                Modifier.size(48.dp),
                tint = Color(0xFF1A8080).copy(alpha = 0.3f)
            )
            Text("无法加载文档", fontSize = 16.sp, color = Color(0xFF909399))
        }
    }
}

// MARK: - Section Group Card (matches iOS contentCards)

@Composable
private fun SectionGroupCard(group: SectionGroup) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Column {
            if (group.heading != null) {
                SectionHeader(section = group.heading)
            }

            val shape = if (group.heading != null) {
                RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            } else {
                RoundedCornerShape(12.dp)
            }

            Column(
                modifier = Modifier.background(Color.White, shape)
            ) {
                group.items.forEachIndexed { index, section ->
                    RenderItem(section)

                    if (index < group.items.size - 1) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFFE8ECF0)
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Section Header (matches iOS sectionHeader)

@Composable
private fun SectionHeader(section: MarkdownSection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1A8080).copy(alpha = 0.08f), Color(0xFF1A8080).copy(alpha = 0.03f)),
                    startX = 0f,
                    endX = Float.POSITIVE_INFINITY
                ),
                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(Color(0xFF1A8080), RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = section.content,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF303133)
        )
    }
}

// MARK: - Item Renderer

@Composable
private fun RenderItem(section: MarkdownSection) {
    when (section.type) {
        SectionType.HEADING -> {
            when (section.level) {
                1 -> TitleView(section.content)
                3 -> SubHeadingView(section.content)
            }
        }
        SectionType.PARAGRAPH -> {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                ParagraphView(section.content)
            }
        }
        SectionType.LIST_ITEM -> {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ListItemView(section.content)
            }
        }
        SectionType.BOLD -> {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                BoldView(section.content)
            }
        }
        SectionType.DIVIDER -> {
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = Color(0xFFE8ECF0)
            )
        }
        SectionType.TABLE -> {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                TableView(section.content)
            }
        }
    }
}

// MARK: - Text Rendering Composables

@Composable
private fun TitleView(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A8080),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubHeadingView(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A8080))
        )

        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF303133)
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ParagraphView(text: String) {
    // Check for URL in text
    val urlRegex = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
    val url = urlRegex.find(text)?.value
    val hasUrl = url != null

    if (hasUrl) {
        val before = text.replace(url!!, "").trim()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (before.isNotEmpty()) {
                Text(
                    text = before,
                    fontSize = 15.sp,
                    color = Color(0xFF4A4A4A),
                    lineHeight = 22.sp
                )
            }
            Text(
                text = "🔗 $url",
                fontSize = 14.sp,
                color = Color(0xFF1A8080),
                modifier = Modifier
                    .background(Color(0xFF1A8080).copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    } else {
        Text(
            text = text,
            fontSize = 15.sp,
            color = Color(0xFF4A4A4A),
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun ListItemView(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A8080).copy(alpha = 0.6f))
        )

        Text(
            text = text,
            fontSize = 15.sp,
            color = Color(0xFF4A4A4A),
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun BoldView(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1A8080),
        lineHeight = 21.sp
    )
}

@Composable
private fun TableView(content: String) {
    val rows = content.split("\n").map { it.split("||") }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE8ECF0), RoundedCornerShape(8.dp))
    ) {
        rows.forEachIndexed { rowIndex, cells ->
            val isHeader = rowIndex == 0

            if (cells.size >= 2) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(if (isHeader) Color(0xFFF5F7FA) else Color.White)
                ) {
                    Text(
                        text = cells[0],
                        fontSize = 14.sp,
                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
                        color = if (isHeader) Color(0xFF303133) else Color(0xFF606266),
                        modifier = Modifier
                            .width(90.dp)
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFE8ECF0))
                    )

                    Text(
                        text = cells[1],
                        fontSize = 14.sp,
                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                        color = if (isHeader) Color(0xFF303133) else Color(0xFF4A4A4A),
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }

                if (rowIndex < rows.size - 1) {
                    Divider(color = Color(0xFFE8ECF0))
                }
            }
        }
    }
}
