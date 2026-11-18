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

package com.davils.kaml.serialization.decode

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.double
import com.davils.kaml.core.node.long
import com.davils.kaml.core.node.string
import com.davils.kaml.core.node.yamlScalar

internal class NumberReading(private val context: DecodeContext) {
    fun integer(node: YamlNode, location: YamlNode, range: LongRange): Long {
        val value = node.long() ?: integerFailure(node, location, range)
        if (value !in range) outside(location, range)
        return value
    }

    fun long(node: YamlNode, location: YamlNode): Long =
        node.long() ?: integerFailure(node, location, Long.MIN_VALUE..Long.MAX_VALUE)

    fun real(node: YamlNode, location: YamlNode): Double {
        val finite = node.double()
        if (finite != null) return finite
        val nonFinite = nonFiniteOf(node) ?: realFailure(node, location)
        if (node.string() !in specials) tooLarge(location, "Double")
        return nonFinite
    }

    fun float(node: YamlNode, location: YamlNode): Float {
        val value = real(node, location)
        val float = value.toFloat()
        if (float.isInfinite() && value.isFinite()) tooLarge(location, "Float")
        return float
    }

    private fun integerFailure(node: YamlNode, location: YamlNode, range: LongRange): Nothing {
        if (node.tag == YamlTag.INTEGER) outside(location, range)
        context.mismatch(location, "an integer")
    }

    private fun nonFiniteOf(node: YamlNode): Double? =
        nonFiniteValues.firstOrNull { value -> yamlScalar(value) == node }

    private fun realFailure(node: YamlNode, location: YamlNode): Nothing {
        if (node.tag == YamlTag.INTEGER) tooLarge(location, "Double")
        context.mismatch(location, "a number")
    }

    private fun outside(location: YamlNode, range: LongRange): Nothing = context.fail(
        YamlDiagnosticCode.NumberOutOfRange,
        location,
        "The integer is outside ${range.first}..${range.last}",
    )

    private fun tooLarge(location: YamlNode, type: String): Nothing = context.fail(
        YamlDiagnosticCode.NumberOutOfRange,
        location,
        "The number is outside the range of a $type",
    )

    private companion object {
        val nonFiniteValues: List<Double> = listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN)

        val specials: Set<String> = setOf(
            ".inf", ".Inf", ".INF",
            "+.inf", "+.Inf", "+.INF",
            "-.inf", "-.Inf", "-.INF",
            ".nan", ".NaN", ".NAN",
        )
    }
}
