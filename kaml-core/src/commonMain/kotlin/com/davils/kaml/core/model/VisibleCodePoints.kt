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

package com.davils.kaml.core.model

internal object VisibleCodePoints {
    private const val HEX_RADIX: Int = 16
    private const val LAST_CODE_POINT: Int = 0x10FFFF
    private const val RANGE_SEPARATOR: String = ".."
    private const val SURROGATE_BITS: Int = 10
    private const val SURROGATE_OFFSET: Int = 0x35FDC00
    private const val CONTROLS: String = "0000..001F 007F..009F"
    private const val FORMATS: String = "00AD 0600..0605 061C 06DD 070F 0890..0891 08E2 180E 200B..200F 202A..202E " +
        "2060..2064 2066..206F FEFF FFF9..FFFB 110BD 110CD 13430..1343F 1BCA0..1BCA3 1D173..1D17A"
    private const val SEPARATORS: String = "2028..2029"
    private const val INVISIBLES: String = "034F 115F..1160 17B4..17B5 180B..180F 3164 FE00..FE0F FFA0"
    private const val SURROGATES_AND_PRIVATE_USE: String = "D800..F8FF"
    private const val NONCHARACTERS: String = "FDD0..FDEF FFF0..FFF8 FFFE..FFFF 1FFFE..1FFFF 2FFFE..2FFFF"
    private const val BEYOND_ASSIGNED_PLANES: String = "323B0..10FFFF"
    private const val HIDDEN: String = "$CONTROLS $FORMATS $SEPARATORS $INVISIBLES $SURROGATES_AND_PRIVATE_USE " +
        "$NONCHARACTERS $BEYOND_ASSIGNED_PLANES"

    private val hidden: List<IntRange> = HIDDEN.split(" ").map(::rangeOf)

    fun isVisible(codePoint: Int): Boolean =
        codePoint in 0..LAST_CODE_POINT && hidden.none { range -> codePoint in range }

    fun isVisibleText(text: String): Boolean {
        var index = 0
        var isVisible = true
        while (isVisible && index < text.length) {
            val high = text[index]
            val low = text.getOrNull(index + 1)
            val isPair = high.isHighSurrogate() && low != null && low.isLowSurrogate()
            val codePoint = low?.takeIf { isPair }?.let { (high.code shl SURROGATE_BITS) + it.code - SURROGATE_OFFSET }
            isVisible = isVisible(codePoint ?: high.code)
            index += 2.takeIf { isPair } ?: 1
        }
        return isVisible
    }

    private fun rangeOf(entry: String): IntRange {
        val bounds = entry.split(RANGE_SEPARATOR).map { bound -> bound.toInt(HEX_RADIX) }
        return bounds.first()..bounds.last()
    }
}
