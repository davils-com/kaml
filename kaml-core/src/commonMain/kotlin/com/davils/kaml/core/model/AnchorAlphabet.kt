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

@file:OptIn(DavilsInternal::class)

package com.davils.kaml.core.model

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val FLOW_INDICATORS: String = ",[]{}"
private const val SPACE: Int = 0x20
private const val NO_BREAK_SPACE: Int = 0xA0
private const val OGHAM_SPACE: Int = 0x1680
private const val FIRST_TYPOGRAPHIC_SPACE: Int = 0x2000
private const val LAST_TYPOGRAPHIC_SPACE: Int = 0x200A
private const val NARROW_NO_BREAK_SPACE: Int = 0x202F
private const val MATHEMATICAL_SPACE: Int = 0x205F
private const val IDEOGRAPHIC_SPACE: Int = 0x3000
private const val FIRST_SURROGATE: Int = 0xD800
private const val LAST_SURROGATE: Int = 0xDFFF
private const val SUPPLEMENTARY_START: Int = 0x10000
private const val SURROGATE_BITS: Int = 10
private const val SUPPLEMENTARY_CHARS: Int = 2

private val spaces: List<IntRange> = listOf(
    SPACE..SPACE,
    NO_BREAK_SPACE..NO_BREAK_SPACE,
    OGHAM_SPACE..OGHAM_SPACE,
    FIRST_TYPOGRAPHIC_SPACE..LAST_TYPOGRAPHIC_SPACE,
    NARROW_NO_BREAK_SPACE..NARROW_NO_BREAK_SPACE,
    MATHEMATICAL_SPACE..MATHEMATICAL_SPACE,
    IDEOGRAPHIC_SPACE..IDEOGRAPHIC_SPACE,
)

internal object AnchorAlphabet {
    fun accepts(name: String): Boolean {
        var index = 0
        var isAccepted = true
        while (isAccepted && index < name.length) {
            val codePoint = codePointAt(name, index)
            isAccepted = isAllowed(codePoint)
            index += charCountOf(codePoint)
        }
        return isAccepted
    }

    private fun isAllowed(codePoint: Int): Boolean {
        val isSpace = spaces.any { range -> codePoint in range }
        val isFlowIndicator = FLOW_INDICATORS.any { indicator -> indicator.code == codePoint }
        val isSurrogate = codePoint in FIRST_SURROGATE..LAST_SURROGATE
        return SafeText.isSafe(codePoint) && !isSpace && !isFlowIndicator && !isSurrogate
    }

    private fun codePointAt(text: String, index: Int): Int {
        val first = text[index]
        val isPair = first.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()
        if (!isPair) return first.code
        val high = first.code - FIRST_SURROGATE
        val low = text[index + 1].code - Char.MIN_LOW_SURROGATE.code
        return SUPPLEMENTARY_START + (high shl SURROGATE_BITS) + low
    }

    private fun charCountOf(codePoint: Int): Int = SUPPLEMENTARY_CHARS.takeIf { codePoint >= SUPPLEMENTARY_START } ?: 1
}
