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

package com.davils.kaml.core.syntax.edit

import com.davils.kaml.core.model.VisibleCodePoints

internal object QuotedText {
    private const val HEX_RADIX: Int = 16
    private const val BYTE_DIGITS: Int = 2
    private const val UNIT_DIGITS: Int = 4
    private const val POINT_DIGITS: Int = 8
    private const val LAST_BYTE: Int = 0xFF
    private const val LAST_UNIT: Int = 0xFFFF
    private const val SURROGATE_OFFSET: Int = 0x35FDC00
    private const val HIGH_SHIFT: Int = 10

    private val named: Map<Int, String> = mapOf(
        '\n'.code to "\\n",
        '\t'.code to "\\t",
        '\r'.code to "\\r",
        '"'.code to "\\\"",
        '\\'.code to "\\\\",
    )

    fun of(text: String): String {
        val quoted = StringBuilder(text.length + 2)
        quoted.append('"')
        forEachCodePoint(text) { codePoint, raw -> quoted.append(escaped(codePoint, raw)) }
        quoted.append('"')
        return quoted.toString()
    }

    fun isShownAsIs(text: String): Boolean = isEvery(text, ::isRaw)

    fun isVisible(text: String): Boolean = VisibleCodePoints.isVisibleText(text)

    private fun escaped(codePoint: Int, raw: String): String {
        val name = named[codePoint]
        if (name != null) return name
        return raw.takeIf { isRaw(codePoint) } ?: hexEscape(codePoint)
    }

    private fun isRaw(codePoint: Int): Boolean = VisibleCodePoints.isVisible(codePoint) && codePoint !in named

    private fun isEvery(text: String, predicate: (Int) -> Boolean): Boolean {
        var isEvery = true
        forEachCodePoint(text) { codePoint, _ -> isEvery = isEvery && predicate(codePoint) }
        return isEvery
    }

    private fun hexEscape(codePoint: Int): String {
        val digits = codePoint.toString(HEX_RADIX).uppercase()
        val isByte = codePoint <= LAST_BYTE
        val isUnit = codePoint <= LAST_UNIT
        val prefix = "\\x".takeIf { isByte } ?: "\\u".takeIf { isUnit } ?: "\\U"
        val width = BYTE_DIGITS.takeIf { isByte } ?: UNIT_DIGITS.takeIf { isUnit } ?: POINT_DIGITS
        return prefix + digits.padStart(width, '0')
    }

    private fun forEachCodePoint(text: String, action: (Int, String) -> Unit) {
        var index = 0
        while (index < text.length) {
            val high = text[index]
            val low = text.getOrNull(index + 1)
            val isPair = high.isHighSurrogate() && low != null && low.isLowSurrogate()
            val count = 2.takeIf { isPair } ?: 1
            val lowCode = low?.code.takeIf { isPair }
            val codePoint = lowCode?.let { (high.code shl HIGH_SHIFT) + it - SURROGATE_OFFSET } ?: high.code
            action(codePoint, text.substring(startIndex = index, endIndex = index + count))
            index += count
        }
    }
}
