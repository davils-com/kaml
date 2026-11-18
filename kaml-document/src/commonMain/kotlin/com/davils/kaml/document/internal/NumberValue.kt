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

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.double
import com.davils.kaml.core.node.long
import kotlin.math.floor

internal class NumberValue private constructor(private val integer: String?, private val real: Double) {
    fun compareTo(other: NumberValue): Int? {
        val left = integer
        val right = other.integer
        if (left != null && right != null) return Decimals.compare(left, right)
        val leftFirst = left?.let { decimal -> withReal(decimal, other.real) }
        val rightFirst = right?.let { decimal -> withReal(decimal, real) }?.let { order -> -order }
        return leftFirst ?: rightFirst ?: reals(other.real)
    }

    private fun reals(other: Double): Int? {
        if (real.isNaN() || other.isNaN()) return null
        val isLess = real < other
        val isGreater = real > other
        return (-1).takeIf { isLess } ?: 1.takeIf { isGreater } ?: 0
    }

    private fun withReal(decimal: String, value: Double): Int? {
        if (value.isNaN()) return null
        val above = (-1).takeIf { value == Double.POSITIVE_INFINITY }
        val below = 1.takeIf { value == Double.NEGATIVE_INFINITY }
        return above ?: below ?: withFinite(decimal, value)
    }

    private fun withFinite(decimal: String, value: Double): Int {
        val whole = floor(value)
        val order = Decimals.compare(left = decimal, right = Decimals.ofIntegral(whole))
        val isFraction = whole != value
        return (-1).takeIf { order == 0 && isFraction } ?: order
    }

    companion object {
        private const val INFINITY: String = ".inf"

        fun of(node: YamlNode.Scalar): NumberValue? {
            if (node.tag == YamlTag.INTEGER) return integerOf(node)
            return NumberValue(null, node.double() ?: nonFinite(node.value)).takeIf { node.tag == YamlTag.FLOAT }
        }

        private fun nonFinite(text: String): Double {
            val lowered = text.lowercase()
            val isPositive = lowered == INFINITY || lowered == "+$INFINITY"
            val isNegative = lowered == "-$INFINITY"
            return Double.POSITIVE_INFINITY.takeIf { isPositive } ?: Double.NEGATIVE_INFINITY.takeIf { isNegative }
                ?: Double.NaN
        }

        private fun integerOf(node: YamlNode.Scalar): NumberValue {
            val decimal = node.long()?.toString() ?: Decimals.canonical(node.value)
            return NumberValue(decimal, node.double() ?: Double.NaN)
        }
    }
}
