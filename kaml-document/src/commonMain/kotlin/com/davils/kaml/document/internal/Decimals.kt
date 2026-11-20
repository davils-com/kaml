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

package com.davils.kaml.document.internal

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

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
private const val LIMB: Long = 1_000_000_000L
private const val LIMB_DIGITS: Int = 9
private const val LIMB_SHIFT: Int = 30

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

    fun compareIntegral(decimal: String, whole: Double, budget: WorkBudget): Int {
        val byMagnitude = magnitudeOrder(decimal, whole)
        if (byMagnitude != null) return byMagnitude
        budget.chargeOrFail(expansionCost(whole))
        return compare(left = decimal, right = ofIntegral(whole))
    }

    fun ofIntegral(value: Double): String {
        if (abs(value) < LONG_RANGE) return value.toLong().toString()
        val bits = value.toRawBits()
        val shifted = bits ushr MANTISSA_BITS
        val exponent = (shifted and EXPONENT_MASK).toInt() - EXPONENT_BIAS
        val fraction = bits and FRACTION_MASK
        val mantissa = fraction or IMPLICIT_BIT
        val sign = MINUS_TEXT.takeIf { value < 0 }.orEmpty()
        return sign + doubled(mantissa, exponent)
    }

    private fun magnitudeOrder(decimal: String, whole: Double): Int? {
        if (abs(whole) < LONG_RANGE) return null
        val isNegative = decimal.startsWith(MINUS)
        val bySign = signOrder(isNegative = isNegative, isWholeNegative = whole < 0)
        return bySign ?: lengthOrder(decimal, whole, isNegative)
    }

    private fun signOrder(isNegative: Boolean, isWholeNegative: Boolean): Int? {
        if (isNegative == isWholeNegative) return null
        return (-1).takeIf { isNegative } ?: 1
    }

    private fun lengthOrder(decimal: String, whole: Double, isNegative: Boolean): Int? {
        val digits = decimal.length - (1.takeIf { isNegative } ?: 0)
        val wholeDigits = digitsOf(whole)
        val shorter = (-1).takeIf { digits < wholeDigits - 1 }
        val magnitudes = shorter ?: 1.takeIf { digits > wholeDigits + 1 }
        return magnitudes?.let { order -> (-order).takeIf { isNegative } ?: order }
    }

    private fun digitsOf(whole: Double): Int = floor(log10(abs(whole))).toInt() + 1

    private fun expansionCost(whole: Double): Long {
        if (abs(whole) < LONG_RANGE) return 0L
        val limbs = digitsOf(whole).toLong() / LIMB_DIGITS + 1L
        return limbs * limbs
    }

    private fun compareMagnitudes(left: String, right: String): Int {
        val byLength = left.length.compareTo(right.length)
        return byLength.takeIf { it != 0 } ?: left.compareTo(right).coerceIn(minimumValue = -1, maximumValue = 1)
    }

    private fun doubled(start: Long, times: Int): String {
        val limbs = limbsOf(start)
        var remaining = times
        while (remaining > 0) {
            val shift = minOf(a = remaining, b = LIMB_SHIFT)
            shifted(limbs, shift)
            remaining -= shift
        }
        val highest = limbs.last().toString()
        val lower = limbs.subList(fromIndex = 0, toIndex = limbs.lastIndex).asReversed()
        return highest + lower.joinToString("") { limb -> limb.toString().padStart(LIMB_DIGITS, '0') }
    }

    private fun limbsOf(value: Long): MutableList<Long> {
        val limbs = ArrayList<Long>()
        var rest = value
        while (rest > 0) {
            limbs.add(rest % LIMB)
            rest /= LIMB
        }
        return limbs
    }

    private fun shifted(limbs: MutableList<Long>, shift: Int) {
        var carry = 0L
        for (index in limbs.indices) {
            val value = (limbs[index] shl shift) + carry
            limbs[index] = value % LIMB
            carry = value / LIMB
        }
        while (carry > 0) {
            limbs.add(carry % LIMB)
            carry /= LIMB
        }
    }
}
