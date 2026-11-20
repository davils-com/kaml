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

package com.davils.kaml.core.node

import com.davils.kore.core.ShortestDecimal
import com.davils.kore.core.annotation.DavilsInternal

private const val NOT_A_NUMBER: String = ".nan"
private const val POSITIVE_INFINITY: String = ".inf"
private const val NEGATIVE_INFINITY: String = "-.inf"
private const val PLAIN_LOWEST_EXPONENT: Int = -3
private const val PLAIN_HIGHEST_EXPONENT: Int = 7
private const val ZERO: String = "0"

internal object FloatText {
    fun of(value: Double): String {
        if (value.isNaN()) return NOT_A_NUMBER
        val infinity = NEGATIVE_INFINITY.takeIf { value < 0 } ?: POSITIVE_INFINITY
        return infinity.takeIf { value.isInfinite() } ?: finite(value)
    }

    private fun finite(value: Double): String {
        val shortest = ShortestDecimal.of(value)
        val sign = "-".takeIf { shortest.isNegative }.orEmpty()
        if (shortest.digits == ZERO) return "${sign}0.0"
        return sign + formatted(shortest)
    }

    private fun formatted(shortest: ShortestDecimal): String {
        val isPlain = shortest.exponent in PLAIN_LOWEST_EXPONENT until PLAIN_HIGHEST_EXPONENT
        if (isPlain) return plain(shortest)
        val fraction = shortest.digits.drop(1).ifEmpty { ZERO }
        return "${shortest.digits.first()}.${fraction}E${shortest.exponent}"
    }

    private fun plain(shortest: ShortestDecimal): String {
        val digits = shortest.digits
        val pointAt = shortest.exponent + 1
        if (pointAt <= 0) return "0." + ZERO.repeat(-pointAt) + digits
        val whole = digits.take(pointAt).padEnd(pointAt, '0')
        val fraction = digits.drop(pointAt).ifEmpty { ZERO }
        return "$whole.$fraction"
    }
}
