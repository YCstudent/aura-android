package com.edistrive.aura.ui.screens.digital

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TableBorder = Color(0xFFD0D5DD)
private val TableHeaderBg = Color(0xFFF5F7FA)
private val TableCellBg = Color.White
private val TextColor = Color(0xFF2C3E50)
private val CodeBg = Color(0xFFF0F0F0)
private val HeadingColor = Color(0xFF1A8080)

/**
 * Markdown renderer handling: # headings, **bold**, `code`, ```code blocks```,
 * bullet/numbered lists, | tables |, [links](url).
 */
@Composable
fun MarkdownText(
    content: String,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier,
    fontSize: Int = 16
) {
    if (isStreaming || content.isBlank()) {
        Text(
            text = content.ifBlank { "..." },
            fontSize = fontSize.sp,
            color = TextColor,
            lineHeight = (fontSize + 5).sp,
            modifier = modifier
        )
        return
    }

    val blocks = parseBlocks(content)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> HeadingBlock(block, fontSize)
                is MdBlock.CodeBlock -> CodeBlockView(block, fontSize)
                is MdBlock.Table -> TableBlock(block, fontSize)
                is MdBlock.Paragraph -> ParagraphBlock(block, fontSize)
                is MdBlock.Separator -> Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ---- Block model ----

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class CodeBlock(val lines: List<String>) : MdBlock()
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data object Separator : MdBlock()
}

// ---- Parser ----

private fun parseBlocks(text: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = text.split("\n")
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block
        if (line.trimStart().startsWith("```")) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            i++ // skip closing ```
            if (codeLines.isNotEmpty()) {
                blocks.add(MdBlock.CodeBlock(codeLines))
            }
            continue
        }

        // Heading
        if (line.trimStart().startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val headingText = line.dropWhile { it == '#' }.trim()
            if (headingText.isNotBlank()) {
                blocks.add(MdBlock.Heading(level.coerceAtMost(6), headingText))
            }
            i++
            continue
        }

        // Table detection: line contains | and the next line looks like a separator
        if (line.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
            val header = parseTableRow(line)
            i += 2 // skip separator
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].contains("|") && !lines[i].trimStart().startsWith("#")) {
                rows.add(parseTableRow(lines[i]))
                i++
            }
            if (header.isNotEmpty()) {
                blocks.add(MdBlock.Table(header, rows))
            }
            continue
        }

        // Skip standalone table separators
        if (isTableSeparator(line)) {
            i++
            continue
        }

        // Empty line -> separator
        if (line.isBlank()) {
            if (blocks.isNotEmpty() && blocks.last() !is MdBlock.Separator) {
                blocks.add(MdBlock.Separator)
            }
            i++
            continue
        }

        // Paragraph — collect consecutive non-empty, non-special lines
        val paraLines = mutableListOf<String>()
        while (i < lines.size &&
            lines[i].isNotBlank() &&
            !lines[i].trimStart().startsWith("#") &&
            !lines[i].trimStart().startsWith("```") &&
            !(lines[i].contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) &&
            !isTableSeparator(lines[i])
        ) {
            paraLines.add(lines[i])
            i++
        }
        if (paraLines.isNotEmpty()) {
            blocks.add(MdBlock.Paragraph(paraLines.joinToString("\n")))
        }
    }

    // Merge consecutive paragraphs and remove redundant separators
    return compactBlocks(blocks)
}

private fun compactBlocks(blocks: List<MdBlock>): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    for (b in blocks) {
        if (b is MdBlock.Separator && (out.isEmpty() || out.last() is MdBlock.Separator)) continue
        if (b is MdBlock.Paragraph && out.lastOrNull() is MdBlock.Paragraph) {
            val prev = out.removeAt(out.lastIndex) as MdBlock.Paragraph
            out.add(MdBlock.Paragraph(prev.text + "\n" + b.text))
            continue
        }
        out.add(b)
    }
    // Trim trailing separators
    while (out.lastOrNull() is MdBlock.Separator) out.removeAt(out.lastIndex)
    return out
}

private fun isTableSeparator(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.isNotEmpty() && trimmed.all { it == '|' || it == '-' || it == ':' || it == ' ' }
}

private fun parseTableRow(line: String): List<String> {
    val cells = line.split("|")
    return cells.map { it.trim() }
        .let { if (it.first().isEmpty()) it.drop(1) else it }
        .let { if (it.last().isEmpty()) it.dropLast(1) else it }
}

// ---- Block composables ----

@Composable
private fun HeadingBlock(block: MdBlock.Heading, baseFontSize: Int) {
    val size = when (block.level) {
        1 -> baseFontSize + 4
        2 -> baseFontSize + 3
        else -> baseFontSize + 2
    }
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = HeadingColor)) {
                append(block.text)
            }
        },
        fontSize = size.sp,
        lineHeight = (size + 4).sp
    )
}

@Composable
private fun CodeBlockView(block: MdBlock.CodeBlock, baseFontSize: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CodeBg)
            .padding(12.dp)
    ) {
        Text(
            text = block.lines.joinToString("\n"),
            fontFamily = FontFamily.Monospace,
            fontSize = (baseFontSize - 1).sp,
            color = TextColor,
            lineHeight = (baseFontSize + 3).sp
        )
    }
}

@Composable
private fun TableBlock(block: MdBlock.Table, baseFontSize: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TableBorder, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TableHeaderBg)
        ) {
            block.header.forEach { cell ->
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    TableCellText(cell, baseFontSize, isHeader = true)
                }
            }
        }
        // Rows
        block.rows.forEachIndexed { idx, row ->
            if (idx > 0 || block.header.isNotEmpty()) {
                DividerLine()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TableCellBg)
            ) {
                val padded = if (row.size < block.header.size)
                    row + List(block.header.size - row.size) { "" }
                else row
                padded.forEach { cell ->
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        TableCellText(cell, baseFontSize, isHeader = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCellText(text: String, baseFontSize: Int, isHeader: Boolean) {
    val annotated = buildAnnotatedString {
        val baseStyle = SpanStyle(
            fontSize = (baseFontSize - 1).sp,
            color = if (isHeader) Color(0xFF333333) else TextColor,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
        )
        var remaining = text

        while (remaining.isNotEmpty()) {
            // Bold: **text**
            if (remaining.startsWith("**")) {
                val end = remaining.indexOf("**", startIndex = 2)
                if (end > 2) {
                    withStyle(baseStyle.copy(fontWeight = FontWeight.Bold)) {
                        append(remaining.substring(2, end))
                    }
                    remaining = remaining.substring(end + 2)
                    continue
                }
            }
            // Inline code: `text`
            if (remaining.startsWith("`")) {
                val end = remaining.indexOf("`", startIndex = 1)
                if (end > 1) {
                    withStyle(baseStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        background = CodeBg,
                        fontWeight = FontWeight.Normal
                    )) {
                        append(remaining.substring(1, end))
                    }
                    remaining = remaining.substring(end + 1)
                    continue
                }
            }
            // Regular character
            withStyle(baseStyle) {
                append(remaining[0])
            }
            remaining = remaining.substring(1)
        }
    }
    Text(
        text = annotated,
        fontSize = (baseFontSize - 1).sp,
        lineHeight = (baseFontSize + 3).sp
    )
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(TableBorder)
    )
}

@Composable
private fun ParagraphBlock(block: MdBlock.Paragraph, baseFontSize: Int) {
    ParagraphText(text = block.text, baseFontSize = baseFontSize)
}

@Composable
private fun ParagraphText(text: String, baseFontSize: Int) {
    // Render each line within a paragraph, handling lists and inline formatting
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEach { line ->
            val annotated = buildAnnotatedString {
                parseInlineMarkdown(line, baseFontSize)
            }
            Text(
                text = annotated,
                fontSize = baseFontSize.sp,
                color = TextColor,
                lineHeight = (baseFontSize + 5).sp
            )
        }
    }
}

// ---- Inline parser (used by ParagraphBlock) ----

private fun AnnotatedString.Builder.parseInlineMarkdown(line: String, baseFontSize: Int) {
    var remaining = line
    var atLineStart = true
    val baseStyle = SpanStyle(fontSize = baseFontSize.sp, color = TextColor)

    while (remaining.isNotEmpty()) {
        // Bold: **text**
        if (remaining.startsWith("**")) {
            val end = remaining.indexOf("**", startIndex = 2)
            if (end > 2) {
                withStyle(baseStyle.copy(fontWeight = FontWeight.SemiBold)) {
                    append(remaining.substring(2, end))
                }
                remaining = remaining.substring(end + 2)
                continue
            }
        }

        // Italic: *text*
        if (remaining.startsWith("*") && !remaining.startsWith("**")) {
            val end = remaining.indexOf("*", startIndex = 1)
            if (end > 1) {
                withStyle(baseStyle.copy(fontStyle = FontStyle.Italic)) {
                    append(remaining.substring(1, end))
                }
                remaining = remaining.substring(end + 1)
                continue
            }
        }

        // Inline code: `text`
        if (remaining.startsWith("`")) {
            val end = remaining.indexOf("`", startIndex = 1)
            if (end > 1) {
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = (baseFontSize - 1).sp,
                    background = CodeBg
                )) {
                    append(remaining.substring(1, end))
                }
                remaining = remaining.substring(end + 1)
                continue
            }
        }

        // Bullet prefix: "- " or "* "
        if (atLineStart && (remaining.startsWith("- ") || remaining.startsWith("* "))) {
            append("  •  ")
            remaining = remaining.substring(2)
            atLineStart = false
            continue
        }

        // Numbered list prefix: "1. "
        val numberedMatch = Regex("^(\\d+)\\. ").find(remaining)
        if (atLineStart && numberedMatch != null) {
            append("  ${numberedMatch.groupValues[1]}.  ")
            remaining = remaining.substring(numberedMatch.value.length)
            atLineStart = false
            continue
        }

        // Link: [text](url)
        val linkMatch = Regex("^\\[(.+?)\\]\\(.+?\\)").find(remaining)
        if (linkMatch != null) {
            withStyle(baseStyle.copy(color = HeadingColor)) {
                append(linkMatch.groupValues[1])
            }
            remaining = remaining.substring(linkMatch.value.length)
            continue
        }

        // Regular character
        atLineStart = false
        withStyle(baseStyle) {
            append(remaining[0])
        }
        remaining = remaining.substring(1)
    }
}
