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

package com.davils.kaml.core.emit

internal object UnsafeCharacters {
    const val NONE: Int = -1

    private const val HEX_RADIX: Int = 16
    private const val RANGE_SEPARATOR: String = ".."
    private const val TAB: Int = 0x09
    private const val LINE_FEED: Int = 0x0A
    private const val FIRST_PRINTABLE: Int = 0x20
    private const val LAST_ASCII_PRINTABLE: Int = 0x7E
    private const val FIRST_SURROGATE: Int = 0xD800
    private const val LAST_SURROGATE: Int = 0xDFFF
    private const val SUPPLEMENTARY_START: Int = 0x10000
    private const val SURROGATE_BITS: Int = 10
    private const val HIDDEN: String = "007F..009F 00AD 034F 061C 115F..1160 17B4..17B5 180B..180F 200B..200F " +
        "2028..202E 2060..206F 3164 FE00..FE0F FEFF FFA0 FFF0..FFFB FFFE..FFFF 1BCA0..1BCA3 1D173..1D17A E0000..E0FFF"

    private val hiddenRanges: List<IntRange> = HIDDEN.split(" ").map(::rangeOf)

    fun firstUnsafeIndex(text: String): Int {
        var index = 0
        while (index < text.length) {
            val codePoint = codePointAt(text, index)
            if (isUnsafe(codePoint)) return index
            index += widthOf(codePoint)
        }
        return NONE
    }

    fun hasLoneSurrogate(text: String): Boolean {
        var index = 0
        var isLone = false
        while (!isLone && index < text.length) {
            val codePoint = codePointAt(text, index)
            isLone = isSurrogate(codePoint)
            index += widthOf(codePoint)
        }
        return isLone
    }

    fun isUnsafe(codePoint: Int): Boolean {
        if (codePoint in FIRST_PRINTABLE..LAST_ASCII_PRINTABLE) return false
        val isControl = codePoint < FIRST_PRINTABLE && codePoint != TAB && codePoint != LINE_FEED
        val isHidden = hiddenRanges.any { range -> codePoint in range }
        return isControl || isHidden || isSurrogate(codePoint)
    }

    fun codePointAt(text: String, index: Int): Int {
        val high = text[index]
        val low = text.getOrNull(index + 1)
        val isPair = high.isHighSurrogate() && low != null && low.isLowSurrogate()
        if (!isPair) return high.code
        val upper = high.code - FIRST_SURROGATE shl SURROGATE_BITS
        return SUPPLEMENTARY_START + upper + (low.code - Char.MIN_LOW_SURROGATE.code)
    }

    fun widthOf(codePoint: Int): Int = 2.takeIf { codePoint >= SUPPLEMENTARY_START } ?: 1

    fun notationOf(codePoint: Int): String {
        val digits = codePoint.toString(HEX_RADIX).uppercase()
        return "U+" + digits.padStart(length = 4, padChar = '0')
    }

    private fun isSurrogate(codePoint: Int): Boolean = codePoint in FIRST_SURROGATE..LAST_SURROGATE

    private fun rangeOf(entry: String): IntRange {
        val bounds = entry.split(RANGE_SEPARATOR).map { bound -> bound.toInt(HEX_RADIX) }
        return bounds.first()..bounds.last()
    }
}
