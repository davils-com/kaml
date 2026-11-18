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

package com.davils.kaml.document.path.parse

private val simpleEscapes: Map<Char, String> = mapOf(
    'b' to "\b",
    'f' to "\u000C",
    'n' to "\n",
    'r' to "\r",
    't' to "\t",
    '/' to "/",
    '\\' to "\\",
)

internal class StringLiteralParser(private val cursor: PathCursor) {
    fun string(quote: Char): String {
        cursor.advance()
        val result = StringBuilder()
        while (cursor.current != quote) result.append(nextCharacter(quote))
        cursor.advance()
        return result.toString()
    }

    private fun nextCharacter(quote: Char): String {
        val current = cursor.current ?: cursor.fail("the string is not terminated")
        if (current.code < SPACE) cursor.fail("a string must not contain control characters")
        if (current == '\\') return escaped(quote)
        return plain(current)
    }

    private fun escaped(quote: Char): String {
        cursor.advance()
        return escape(quote)
    }

    private fun plain(current: Char): String {
        if (current.isSurrogate()) return surrogatePair(current)
        cursor.advance()
        return current.toString()
    }

    private fun surrogatePair(high: Char): String {
        val low = cursor.peek(1)
        val isPair = high.isHighSurrogate() && low != null && low.isLowSurrogate()
        if (!isPair) cursor.fail("a string must not contain a lone surrogate")
        cursor.advance(2)
        return "$high$low"
    }

    private fun escape(quote: Char): String {
        val current = cursor.current ?: cursor.fail("the escape is not complete")
        cursor.advance()
        if (current == 'u') return unicode()
        val quoted = current.toString().takeIf { current == quote }
        return simpleEscapes[current] ?: quoted ?: cursor.fail("the escape is not valid")
    }

    private fun unicode(): String {
        val first = hexadecimal()
        if (first.isLowSurrogate()) cursor.fail("a low surrogate needs a high surrogate before it")
        if (!first.isHighSurrogate()) return first.toString()
        return pairedWith(first)
    }

    private fun pairedWith(high: Char): String {
        cursor.expect("\\u")
        val low = hexadecimal()
        if (!low.isLowSurrogate()) cursor.fail("a high surrogate needs a low surrogate after it")
        return "$high$low"
    }

    private fun hexadecimal(): Char {
        val digits = (0 until HEX_LENGTH).map { ahead -> cursor.peek(ahead) }
        val isHexadecimal = digits.all { digit -> digit != null && isAsciiHexadecimal(digit) }
        if (!isHexadecimal) cursor.fail("expected four hexadecimal digits")
        cursor.advance(HEX_LENGTH)
        val code = digits.joinToString("").toInt(HEX_RADIX)
        return code.toChar()
    }

    private fun isAsciiHexadecimal(digit: Char): Boolean = digit in '0'..'9' || digit in 'a'..'f' || digit in 'A'..'F'

    private companion object {
        const val SPACE: Int = 0x20
        const val HEX_LENGTH: Int = 4
        const val HEX_RADIX: Int = 16
    }
}
