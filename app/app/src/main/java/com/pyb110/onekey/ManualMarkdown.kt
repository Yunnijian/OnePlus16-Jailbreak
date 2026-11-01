package com.pyb110.onekey

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class ListItem(val marker: String, val text: String) : MdBlock
    data class Callout(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MdBlock
}

private val orderedItem = Regex("""^(\d+)\.\s+(.*)$""")
private val tableSeparatorCell = Regex("""^:?-{3,}:?$""")

internal fun parseManualMarkdown(source: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()
    val lines = source.lines()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MdBlock.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }
    }

    fun splitRow(line: String): List<String> =
        line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

    fun isTableRow(line: String): Boolean = line.trimStart().startsWith("|")

    var index = 0
    while (index < lines.size) {
        val line = lines[index].trimEnd()
        val ordered = orderedItem.find(line)
        when {
            line.isBlank() -> {
                flushParagraph()
                index++
            }
            line.trimStart().startsWith("```") -> {
                flushParagraph()
                index++
                val code = StringBuilder()
                while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[index].trimEnd())
                    index++
                }
                if (index < lines.size) index++
                blocks += MdBlock.Code(code.toString())
            }
            line.trimStart().startsWith(">") -> {
                flushParagraph()
                val quoted = mutableListOf<String>()
                while (index < lines.size && lines[index].trimStart().startsWith(">")) {
                    quoted += lines[index].trimStart().removePrefix(">").trim()
                    index++
                }
                blocks += MdBlock.Callout(quoted.joinToString("\n").trim())
            }
            isTableRow(line) -> {
                flushParagraph()
                val rows = mutableListOf<List<String>>()
                while (index < lines.size && isTableRow(lines[index])) {
                    rows += splitRow(lines[index])
                    index++
                }
                val hasHeader = rows.size >= 2 && rows[1].isNotEmpty() &&
                    rows[1].all { tableSeparatorCell.matches(it) }
                if (hasHeader) {
                    blocks += MdBlock.Table(rows.first(), rows.drop(2))
                } else {
                    blocks += MdBlock.Table(emptyList(), rows)
                }
            }
            line.startsWith("### ") -> {
                flushParagraph()
                blocks += MdBlock.Heading(3, line.removePrefix("### ").trim())
                index++
            }
            line.startsWith("## ") -> {
                flushParagraph()
                blocks += MdBlock.Heading(2, line.removePrefix("## ").trim())
                index++
            }
            line.startsWith("# ") -> {
                flushParagraph()
                blocks += MdBlock.Heading(1, line.removePrefix("# ").trim())
                index++
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                flushParagraph()
                blocks += MdBlock.ListItem("•", line.substring(2).trim())
                index++
            }
            ordered != null -> {
                flushParagraph()
                blocks += MdBlock.ListItem("${ordered.groupValues[1]}.", ordered.groupValues[2].trim())
                index++
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(line.trim())
                index++
            }
        }
    }
    flushParagraph()
    return blocks
}

internal fun markdownInline(text: String, codeBackground: Color): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBackground
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    var j = i + 1
                    while (j < text.length && !text.startsWith("**", j) && text[j] != '`') j++
                    append(text.substring(i, j))
                    i = j
                }
            }
        }
    }

@Composable
private fun MdHeading(block: MdBlock.Heading, codeBackground: Color) {
    val color = COUITheme.colorScheme.onSurface
    if (block.level == 1) {
        Column {
            Text(
                text = markdownInline(block.text, codeBackground),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = color.copy(alpha = 0.15f))
        }
    } else {
        Text(
            text = markdownInline(block.text, codeBackground),
            modifier = Modifier.padding(top = if (block.level == 2) 16.dp else 10.dp),
            fontSize = if (block.level == 2) 22.sp else 18.sp,
            fontWeight = if (block.level == 2) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun MdCallout(block: MdBlock.Callout, codeBackground: Color) {
    val accent = COUITheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(color = accent.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Text(
                text = markdownInline(block.text, codeBackground),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = COUITheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MdCode(block: MdBlock.Code) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(
            color = COUITheme.colorScheme.onSurface.copy(alpha = 0.06f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = block.text,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = COUITheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MdTable(block: MdBlock.Table, codeBackground: Color) {
    val border = COUITheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val columnCount = maxOf(block.headers.size, block.rows.maxOfOrNull { it.size } ?: 0)
    val weights = (0 until columnCount).map { column ->
        val longest = (listOf(block.headers) + block.rows)
            .mapNotNull { it.getOrNull(column) }
            .maxOfOrNull { it.length } ?: 6
        longest.coerceIn(8, 40).toFloat()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(
            color = COUITheme.colorScheme.onSurface.copy(alpha = 0.03f)
        )
    ) {
        Column {
            if (block.headers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(COUITheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    block.headers.forEachIndexed { column, header ->
                        Text(
                            text = markdownInline(header, codeBackground),
                            modifier = Modifier
                                .weight(weights.getOrElse(column) { 1f })
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = COUITheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider(color = border)
            }
            block.rows.forEachIndexed { rowIndex, row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { column, cell ->
                        Text(
                            text = markdownInline(cell, codeBackground),
                            modifier = Modifier
                                .weight(weights.getOrElse(column) { 1f })
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = COUITheme.colorScheme.onSurface
                        )
                    }
                }
                if (rowIndex != block.rows.lastIndex) {
                    HorizontalDivider(color = border)
                }
            }
        }
    }
}

@Composable
internal fun MdBlockView(block: MdBlock, codeBackground: Color) {
    when (block) {
        is MdBlock.Heading -> MdHeading(block, codeBackground)
        is MdBlock.Paragraph -> Text(
            text = markdownInline(block.text, codeBackground),
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = COUITheme.colorScheme.onSurface
        )
        is MdBlock.ListItem -> Row(modifier = Modifier.padding(start = 2.dp)) {
            Text(
                text = block.marker,
                modifier = Modifier.width(24.dp),
                fontSize = 15.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = COUITheme.colorScheme.primary
            )
            Text(
                text = markdownInline(block.text, codeBackground),
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = COUITheme.colorScheme.onSurface
            )
        }
        is MdBlock.Callout -> MdCallout(block, codeBackground)
        is MdBlock.Code -> MdCode(block)
        is MdBlock.Table -> MdTable(block, codeBackground)
    }
}
