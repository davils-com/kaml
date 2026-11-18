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

internal class FlowScalarValues private constructor(private val text: String, private val mode: Mode) {
    private val result = StringBuilder(text.length)
    private val pending = StringBuilder()
    private var index = 0

    private fun read(): String {
        while (index < text.length) {
            step()
        }
        flush()
        return result.toString()
    }

    private fun step() {
        val character = text[index]
        val isHandled = escaped(character) || quoteEscaped(character) || folded(character) || white(character)
        if (isHandled) return
        flush()
        result.append(character)
        index++
    }

    private fun escaped(character: Char): Boolean {
        val isEscape = mode == Mode.DOUBLE && character == BACKSLASH && index + 1 < text.length
        if (!isEscape) return false
        flush()
        val isEscapedBreak = text[index + 1].isLineBreak()
        if (isEscapedBreak) index = afterEmptyLines(skipBreak(index + 1))
        if (!isEscapedBreak) index = EscapeDecoder.decode(text, index, result)
        return true
    }

    private fun quoteEscaped(character: Char): Boolean {
        val isEscape = mode == Mode.SINGLE && character == SINGLE_QUOTE && text.getOrNull(index + 1) == SINGLE_QUOTE
        if (!isEscape) return false
        flush()
        result.append(SINGLE_QUOTE)
        index += 2
        return true
    }

    private fun folded(character: Char): Boolean {
        if (!character.isLineBreak()) return false
        pending.clear()
        index = skipBreak(index)
        val before = result.length
        index = afterEmptyLines(index)
        if (result.length == before) result.append(' ')
        return true
    }

    private fun white(character: Char): Boolean {
        if (character != ' ' && character != '\t') return false
        pending.append(character)
        index++
        return true
    }

    private fun afterEmptyLines(from: Int): Int {
        var position = skipWhite(from)
        while (position < text.length && text[position].isLineBreak()) {
            result.append('\n')
            position = skipWhite(skipBreak(position))
        }
        return position
    }

    private fun skipWhite(from: Int): Int {
        var position = from
        while (position < text.length && (text[position] == ' ' || text[position] == '\t')) {
            position++
        }
        return position
    }

    private fun skipBreak(from: Int): Int {
        val isPair = text[from] == '\r' && text.getOrNull(from + 1) == '\n'
        return from + (2.takeIf { isPair } ?: 1)
    }

    private fun flush() {
        result.append(pending)
        pending.clear()
    }

    private fun Char.isLineBreak(): Boolean = this == '\n' || this == '\r'

    private enum class Mode {
        PLAIN,
        SINGLE,
        DOUBLE,
    }

    companion object {
        private const val BACKSLASH: Char = '\\'
        private const val SINGLE_QUOTE: Char = '\''

        fun plain(text: String): String = FlowScalarValues(text, Mode.PLAIN).read()

        fun singleQuoted(text: String): String = FlowScalarValues(innerOf(text, SINGLE_QUOTE), Mode.SINGLE).read()

        fun doubleQuoted(text: String): String = FlowScalarValues(innerOf(text, '"'), Mode.DOUBLE).read()

        private fun innerOf(text: String, quote: Char): String {
            val isClosed = text.length >= 2 && text.last() == quote && !endsWithEscape(text, quote)
            return text.substring(startIndex = 1, endIndex = text.length - (1.takeIf { isClosed } ?: 0))
        }

        private fun endsWithEscape(text: String, quote: Char): Boolean {
            if (quote != '"') return false
            val backslashes = text.dropLast(1).takeLastWhile { it == BACKSLASH }.length
            return backslashes % 2 == 1
        }
    }
}
