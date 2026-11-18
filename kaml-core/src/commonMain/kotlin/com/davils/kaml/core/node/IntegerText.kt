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

private const val LIMB_BASE: Long = 1_000_000_000L
private const val LIMB_DIGITS: Int = 9
private const val DECIMAL_RADIX: Int = 10
private const val SEXAGESIMAL_RADIX: Int = 60
private const val ZERO: String = "0"
private const val MINUS: String = "-"
private const val HEX_DIGITS: String = "0123456789abcdef"

internal object IntegerText {
    fun decimal(isNegative: Boolean, digits: String): String {
        val significant = digits.trimStart('0')
        if (significant.isEmpty()) return ZERO
        return MINUS.takeIf { isNegative }.orEmpty() + significant
    }

    fun ofRadix(isNegative: Boolean, digits: String, radix: Int): String {
        val magnitude = Magnitude()
        digits.forEach { digit -> magnitude.multiplyAdd(factor = radix, addend = asciiDigitValue(digit)) }
        return decimal(isNegative, magnitude.toDecimal())
    }

    fun ofSexagesimal(isNegative: Boolean, leading: String, places: List<String>): String {
        val magnitude = Magnitude()
        leading.forEach { digit -> magnitude.multiplyAdd(factor = DECIMAL_RADIX, addend = asciiDigitValue(digit)) }
        places.forEach { place -> magnitude.multiplyAdd(factor = SEXAGESIMAL_RADIX, addend = place.toInt()) }
        return decimal(isNegative, magnitude.toDecimal())
    }

    private fun asciiDigitValue(digit: Char): Int = HEX_DIGITS.indexOf(digit.lowercaseChar())

    private class Magnitude {
        private val limbs = ArrayList<Long>().apply { add(0L) }

        fun multiplyAdd(factor: Int, addend: Int) {
            var carry = addend.toLong()
            for (index in limbs.indices) {
                val product = limbs[index] * factor + carry
                limbs[index] = product % LIMB_BASE
                carry = product / LIMB_BASE
            }
            while (carry > 0) {
                limbs.add(carry % LIMB_BASE)
                carry /= LIMB_BASE
            }
        }

        fun toDecimal(): String = buildString {
            append(limbs.last())
            for (index in limbs.size - 2 downTo 0) {
                append(limbs[index].toString().padStart(LIMB_DIGITS, '0'))
            }
        }
    }
}
