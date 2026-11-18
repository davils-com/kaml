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

package com.davils.kaml.document.path.parse

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlTagged

internal class NumberLiteralParser(private val cursor: PathCursor, private val maximumLength: Int) {
    private val signs = listOf("+", "-")

    fun read(): YamlNode {
        val sign = "-".takeIf { cursor.accept("-") }.orEmpty()
        val integer = digits()
        val isPositive = integer.isNotEmpty() && !integer.startsWith("0")
        val isCanonical = integer == "0" || isPositive
        if (!isCanonical) cursor.fail("expected a number without leading zeros")
        val fraction = fraction()
        val exponent = exponent()
        val text = sign + integer + fraction + exponent
        if (text.length > maximumLength) cursor.fail("the number is too long")
        val isInteger = fraction.isEmpty() && exponent.isEmpty()
        if (isInteger) return integer(text)
        return real(text.toDouble())
    }

    private fun integer(text: String): YamlNode = text.toLongOrNull()?.let(
        ::yamlScalar,
    ) ?: yamlTagged(text, YamlTag.INTEGER)

    private fun real(value: Double): YamlNode {
        if (!value.isFinite()) cursor.fail("the number is out of range")
        return yamlScalar(value)
    }

    private fun fraction(): String {
        if (!cursor.accept(".")) return ""
        return "." + requiredDigits()
    }

    private fun exponent(): String {
        val isExponent = cursor.accept("e") || cursor.accept("E")
        if (!isExponent) return ""
        val sign = signs.firstOrNull(cursor::accept)
        return "e" + sign.orEmpty() + requiredDigits()
    }

    private fun requiredDigits(): String = digits().ifEmpty { cursor.fail("expected a digit") }

    private fun digits(): String = cursor.takeWhile { it in '0'..'9' }
}
