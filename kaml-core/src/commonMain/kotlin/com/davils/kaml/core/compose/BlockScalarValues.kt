/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kaml.core.compose

internal object BlockScalarValues {
    private const val LITERAL: Char = '|'
    private const val KEEP: Char = '+'
    private const val STRIP: Char = '-'

    fun value(header: String, content: String, parentIndent: Int): String {
        val lines = linesOf(content)
        val explicit = header.firstOrNull { it.isDigit() }?.digitToInt() ?: 0
        val indent = (explicit + maxOf(a = parentIndent, b = 0)).takeIf { explicit > 0 } ?: detectedIndent(lines)
        val mapped = lines.map { line -> contentOf(line, indent) }
        val last = lines.indices.lastOrNull { index -> !isEmptyAt(lines[index], indent) } ?: -1
        val body = bodyOf(header.first() == LITERAL, mapped.take(last + 1))
        return body + chomped(header, lines, last)
    }

    private fun bodyOf(isLiteral: Boolean, lines: List<String>): String {
        if (isLiteral) return lines.joinToString("\n")
        return folded(lines)
    }

    private fun chomped(header: String, lines: List<Line>, last: Int): String {
        val finalBreak = "\n".takeIf { last >= 0 && STRIP !in header }.orEmpty()
        val trailing = "\n".repeat(lines.size - last - 1)
        val kept = trailing.takeIf { KEEP in header }.orEmpty()
        return finalBreak + kept
    }

    private fun contentOf(line: Line, indent: Int): String {
        if (isEmptyAt(line, indent)) return ""
        return line.text.drop(indent)
    }

    private fun isEmptyAt(line: Line, indent: Int): Boolean =
        line.spaces == line.text.length && line.text.length <= indent

    private fun folded(lines: List<String>): String {
        val result = StringBuilder()
        var empties = 0
        var isPreviousText = false
        var isStarted = false
        lines.forEach { line ->
            if (line.isEmpty()) {
                empties++
                return@forEach
            }
            val isText = line.first() != ' ' && line.first() != '\t'
            result.append(separator(started = isStarted, folds = isPreviousText && isText, empties = empties))
            result.append(line)
            isPreviousText = isText
            isStarted = true
            empties = 0
        }
        return result.toString()
    }

    private fun separator(started: Boolean, folds: Boolean, empties: Int): String {
        if (!started) return "\n".repeat(empties)
        val joined = " ".takeIf { folds && empties == 0 } ?: "\n".repeat(empties)
        return joined.takeIf { folds } ?: "\n".repeat(empties + 1)
    }

    private fun detectedIndent(lines: List<Line>): Int {
        val first = lines.firstOrNull { line -> line.text.isNotBlankOfSpaces() }
        return first?.spaces ?: lines.maxOfOrNull(Line::spaces) ?: 0
    }

    private fun String.isNotBlankOfSpaces(): Boolean = any { it != ' ' }

    private fun linesOf(content: String): List<Line> {
        val lines = ArrayList<Line>()
        var start = 0
        while (start < content.length) {
            val end = lineEnd(content, start)
            lines.add(Line(content.substring(startIndex = start, endIndex = end)))
            start = afterBreak(content, end)
        }
        return lines
    }

    private fun lineEnd(content: String, start: Int): Int {
        var end = start
        while (end < content.length && content[end] != '\n' && content[end] != '\r') {
            end++
        }
        return end
    }

    private fun afterBreak(content: String, end: Int): Int {
        if (end >= content.length) return end
        val isPair = content[end] == '\r' && content.getOrNull(end + 1) == '\n'
        return end + (2.takeIf { isPair } ?: 1)
    }

    private data class Line(val text: String) {
        val spaces: Int get() = text.takeWhile { it == ' ' }.length
    }
}
