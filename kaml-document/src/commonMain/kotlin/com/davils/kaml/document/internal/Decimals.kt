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

package com.davils.kaml.document.internal

import kotlin.math.abs

private const val MINUS: Char = '-'
private const val PLUS: Char = '+'
private const val SEPARATOR: Char = '_'
private const val ZERO: String = "0"
private const val LONG_RANGE: Double = 9.223372036854775807E18
private const val MINUS_TEXT: String = "-"
private const val MANTISSA_BITS: Int = 52
private const val EXPONENT_MASK: Long = 0x7FFL
private const val FRACTION_MASK: Long = 0xFFFFFFFFFFFFFL
private const val IMPLICIT_BIT: Long = 0x10000000000000L
private const val EXPONENT_BIAS: Int = 1075
private const val DECIMAL_RADIX: Int = 10

internal object Decimals {
    fun canonical(text: String): String? {
        val sign = text.firstOrNull()?.takeIf { it == MINUS || it == PLUS }
        val unsigned = text.substring(sign?.let { 1 } ?: 0)
        val digits = unsigned.filter { it != SEPARATOR }
        val isDecimal = digits.isNotEmpty() && digits.all { it in '0'..'9' }
        val magnitude = digits.trimStart('0').ifEmpty { ZERO }
        val isNegative = sign == MINUS && magnitude != ZERO
        val prefix = MINUS_TEXT.takeIf { isNegative }.orEmpty()
        return (prefix + magnitude).takeIf { isDecimal }
    }

    fun compare(left: String, right: String): Int {
        val isLeftNegative = left.startsWith(MINUS)
        val isRightNegative = right.startsWith(MINUS)
        if (isLeftNegative != isRightNegative) return (-1).takeIf { isLeftNegative } ?: 1
        val magnitudes = compareMagnitudes(left = left.trimStart(MINUS), right = right.trimStart(MINUS))
        val negated = (-magnitudes).takeIf { isLeftNegative }
        return negated ?: magnitudes
    }

    fun ofIntegral(value: Double): String {
        if (abs(value) < LONG_RANGE) return value.toLong().toString()
        val bits = value.toRawBits()
        val shifted = bits ushr MANTISSA_BITS
        val exponent = (shifted and EXPONENT_MASK).toInt() - EXPONENT_BIAS
        val fraction = bits and FRACTION_MASK
        val mantissa = fraction or IMPLICIT_BIT
        val sign = MINUS_TEXT.takeIf { value < 0 }.orEmpty()
        return sign + doubled(mantissa.toString(), exponent)
    }

    private fun compareMagnitudes(left: String, right: String): Int {
        val byLength = left.length.compareTo(right.length)
        return byLength.takeIf { it != 0 } ?: left.compareTo(right).coerceIn(minimumValue = -1, maximumValue = 1)
    }

    private fun doubled(start: String, times: Int): String {
        val reversed = start.reversed()
        val digits = reversed.mapTo(ArrayList()) { it - '0' }
        repeat(times) {
            var carry = 0
            for (index in digits.indices) {
                val value = digits[index] * 2 + carry
                digits[index] = value % DECIMAL_RADIX
                carry = value / DECIMAL_RADIX
            }
            if (carry > 0) digits.add(carry)
        }
        return digits.reversed().joinToString("")
    }
}
