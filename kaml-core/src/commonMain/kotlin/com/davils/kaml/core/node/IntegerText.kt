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
private const val LARGEST_FACTOR: Long = 1L shl 31
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
        magnitude.addPlaces(digits.map(::asciiDigitValue), radix)
        return decimal(isNegative, magnitude.toDecimal())
    }

    fun ofSexagesimal(isNegative: Boolean, leading: String, places: List<String>): String {
        val magnitude = Magnitude()
        magnitude.addPlaces(leading.map(::asciiDigitValue), DECIMAL_RADIX)
        magnitude.addPlaces(places.map(String::toInt), SEXAGESIMAL_RADIX)
        return decimal(isNegative, magnitude.toDecimal())
    }

    private fun asciiDigitValue(digit: Char): Int = HEX_DIGITS.indexOf(digit.lowercaseChar())

    private class Magnitude {
        private var limbs = LongArray(1)
        private var size = 1

        fun addPlaces(places: List<Int>, radix: Int) {
            val group = groupOf(radix)
            var start = 0
            while (start < places.size) {
                val end = minOf(a = start + group, b = places.size)
                var factor = 1L
                var addend = 0L
                for (index in start until end) {
                    factor *= radix
                    addend = addend * radix + places[index]
                }
                multiplyAdd(factor, addend)
                start = end
            }
        }

        fun toDecimal(): String = buildString {
            append(limbs[size - 1])
            for (index in size - 2 downTo 0) {
                append(limbs[index].toString().padStart(LIMB_DIGITS, '0'))
            }
        }

        private fun groupOf(radix: Int): Int {
            var group = 0
            var factor = 1L
            while (factor * radix <= LARGEST_FACTOR) {
                factor *= radix
                group++
            }
            return group
        }

        private fun multiplyAdd(factor: Long, addend: Long) {
            var carry = addend
            for (index in 0 until size) {
                val product = limbs[index] * factor + carry
                limbs[index] = product % LIMB_BASE
                carry = product / LIMB_BASE
            }
            while (carry > 0) {
                append(carry % LIMB_BASE)
                carry /= LIMB_BASE
            }
        }

        private fun append(limb: Long) {
            if (size == limbs.size) limbs = limbs.copyOf(size * 2)
            limbs[size] = limb
            size++
        }
    }
}
