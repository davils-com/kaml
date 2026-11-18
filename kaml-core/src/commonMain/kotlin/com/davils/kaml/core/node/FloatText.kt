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
package com.davils.kaml.core.node

import kotlin.math.absoluteValue

private const val NOT_A_NUMBER: String = ".nan"
private const val POSITIVE_INFINITY: String = ".inf"
private const val NEGATIVE_INFINITY: String = "-.inf"
private const val PLAIN_LOWEST_EXPONENT: Int = -3
private const val PLAIN_HIGHEST_EXPONENT: Int = 7
private const val DECIMAL_RADIX: Int = 10
private const val HALF_DIGIT: Int = 5
private const val TWO_DIGITS: Int = 2

internal object FloatText {
    fun of(value: Double): String {
        if (value.isNaN()) return NOT_A_NUMBER
        val infinity = NEGATIVE_INFINITY.takeIf { value < 0 } ?: POSITIVE_INFINITY
        return infinity.takeIf { value.isInfinite() } ?: finite(value)
    }

    private fun finite(value: Double): String {
        val isNegativeZero = value == 0.0 && 1.0 / value < 0
        val sign = "-".takeIf { value < 0 || isNegativeZero }.orEmpty()
        val magnitude = value.absoluteValue
        val shortest = shorterOf(shortestOf(shortestDecimal(magnitude)), magnitude)
        if (shortest.digits == "0") return "${sign}0.0"
        return sign + formatted(shortest)
    }

    private fun formatted(shortest: Shortest): String {
        val isPlain = shortest.exponent in PLAIN_LOWEST_EXPONENT until PLAIN_HIGHEST_EXPONENT
        if (isPlain) return plain(shortest)
        val fraction = shortest.digits.drop(1).ifEmpty { "0" }
        return "${shortest.digits.first()}.${fraction}E${shortest.exponent}"
    }

    private fun plain(shortest: Shortest): String {
        val digits = shortest.digits
        val pointAt = shortest.exponent + 1
        if (pointAt <= 0) return "0." + "0".repeat(-pointAt) + digits
        val whole = digits.take(pointAt).padEnd(pointAt, '0')
        val fraction = digits.drop(pointAt).ifEmpty { "0" }
        return "$whole.$fraction"
    }

    private fun shortestOf(text: String): Shortest {
        val exponentAt = text.uppercase().indexOf('E')
        val mantissaEnd = exponentAt.takeIf { it >= 0 } ?: text.length
        val mantissa = text.take(mantissaEnd)
        val written = text.drop(mantissaEnd + 1).removePrefix("+")
        val exponent = written.toIntOrNull() ?: 0
        val whole = mantissa.substringBefore('.')
        val fraction = mantissa.substringAfter('.', missingDelimiterValue = "")
        val all = whole + fraction
        val unpadded = all.trimStart('0')
        val significant = unpadded.trimEnd('0').ifEmpty { "0" }
        val leadingZeros = all.length - unpadded.length
        return Shortest(digits = significant, exponent = exponent + whole.length - 1 - leadingZeros)
    }

    private fun shorterOf(written: Shortest, magnitude: Double): Shortest {
        if (written.digits.length != TWO_DIGITS) return written
        val rounded = (written.digits.toInt() + HALF_DIGIT) / DECIMAL_RADIX
        val isCarry = rounded == DECIMAL_RADIX
        val carry = 1.takeIf { isCarry } ?: 0
        val digit = "1".takeIf { isCarry } ?: rounded.toString()
        val candidate = Shortest(digits = digit, exponent = written.exponent + carry)
        return candidate.takeIf { isValueOf(it, magnitude) } ?: written
    }

    private fun isValueOf(shortest: Shortest, magnitude: Double): Boolean =
        NumberTexts.finiteOrNull("${shortest.digits}e${shortest.exponent}") == magnitude

    private data class Shortest(val digits: String, val exponent: Int)
}
