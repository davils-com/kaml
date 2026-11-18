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

package com.davils.kaml.core.schema

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.parseDecimalOrNull

internal object NumberForms {
    private val infinities: Set<String> = setOf(".inf", ".Inf", ".INF")
    private val notANumbers: Set<String> = setOf(".nan", ".NaN", ".NAN")

    fun isJsonNumber(text: String): Boolean {
        val scan = TextScan(text)
        scan.accept('-')
        val hasInteger = scan.accept('0') || nonZeroDigits(scan)
        if (scan.accept('.')) scan.count(TextScan::isDigit)
        return hasInteger && exponent(scan, isSignRequired = false) && scan.isAtEnd
    }

    fun isJsonInteger(text: String): Boolean {
        val scan = TextScan(text)
        scan.accept('-')
        val hasInteger = scan.accept('0') || nonZeroDigits(scan)
        return hasInteger && scan.isAtEnd
    }

    fun isCoreDecimal(text: String): Boolean {
        val scan = TextScan(text)
        scan.sign()
        return scan.count(TextScan::isDigit) > 0 && scan.isAtEnd
    }

    fun isCoreFloat(text: String): Boolean = specialFloat(text) != null || isDecimalFloat(text)

    fun specialFloat(text: String): Double? {
        if (text in notANumbers) return Double.NaN
        val isNegative = text.startsWith("-")
        val unsigned = text.removePrefix("-").takeIf { isNegative } ?: text.removePrefix("+")
        val infinity = Double.NEGATIVE_INFINITY.takeIf { isNegative } ?: Double.POSITIVE_INFINITY
        return infinity.takeIf { unsigned in infinities }
    }

    fun isRadix(text: String, prefix: String, isDigit: (Char) -> Boolean): Boolean {
        val digits = text.removePrefix(prefix)
        return text.startsWith(prefix) && digits.isNotEmpty() && digits.all(isDigit)
    }

    fun finite(decimal: String): Double? {
        val unsigned = decimal.removePrefix("+")
        if (unsigned.length > Limits.MAX_DECIMAL_TEXT_LENGTH) return null
        val overflow = Double.NEGATIVE_INFINITY.takeIf { unsigned.startsWith("-") } ?: Double.POSITIVE_INFINITY
        return parseDecimalOrNull(unsigned) ?: overflow
    }

    private fun isDecimalFloat(text: String): Boolean {
        val scan = TextScan(text)
        scan.sign()
        val integer = scan.count(TextScan::isDigit)
        val hasPoint = scan.accept('.')
        val fraction = scan.count(TextScan::isDigit).takeIf { hasPoint } ?: 0
        val hasMantissa = integer > 0 || fraction > 0
        return hasMantissa && exponent(scan, isSignRequired = false) && scan.isAtEnd
    }

    fun exponent(scan: TextScan, isSignRequired: Boolean): Boolean {
        if (!scan.acceptAny("eE")) return true
        val hasSign = scan.sign()
        return (hasSign || !isSignRequired) && scan.count(TextScan::isDigit) > 0
    }

    private fun nonZeroDigits(scan: TextScan): Boolean {
        if (!scan.acceptAny("123456789")) return false
        scan.count(TextScan::isDigit)
        return true
    }
}
