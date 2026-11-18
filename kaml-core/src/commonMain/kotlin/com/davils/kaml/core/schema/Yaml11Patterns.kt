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

package com.davils.kaml.core.schema

private const val YEAR_DIGITS: Int = 4
private const val TWO_DIGITS: Int = 2
private const val HIGHEST_SEXAGESIMAL_TENS: Char = '5'
private const val NON_ZERO_DIGITS: String = "123456789"

internal object Yaml11Patterns {
    fun isBinary(text: String): Boolean = prefixed(text, "0b", TextScan::isBinaryDigit)

    fun isHex(text: String): Boolean = prefixed(text, "0x", TextScan::isHexDigit)

    fun isOctal(text: String): Boolean {
        val scan = TextScan(text)
        scan.sign()
        val hasZero = scan.accept('0')
        val digits = scan.count { character -> TextScan.isOctalDigit(character) || character == '_' }
        return hasZero && digits > 0 && scan.isAtEnd
    }

    fun isDecimal(text: String): Boolean {
        val scan = TextScan(text)
        scan.sign()
        if (scan.accept('0')) return scan.isAtEnd
        val hasLeadingDigit = scan.acceptAny(NON_ZERO_DIGITS)
        scan.count(TextScan::isDigitOrUnderscore)
        return hasLeadingDigit && scan.isAtEnd
    }

    fun isSexagesimal(text: String): Boolean {
        val scan = TextScan(text)
        scan.sign()
        val hasLeadingDigit = scan.acceptAny(NON_ZERO_DIGITS)
        scan.count(TextScan::isDigitOrUnderscore)
        return hasLeadingDigit && places(scan) && scan.isAtEnd
    }

    fun isReal(text: String): Boolean {
        val scan = TextScan(text)
        scan.sign()
        val hasInteger = scan.acceptAny("0$NON_ZERO_DIGITS")
        if (hasInteger) scan.count(TextScan::isDigitOrUnderscore)
        val hasPoint = scan.accept('.')
        val hasFraction = scan.count(TextScan::isDigit) > 0
        val isWellFormed = hasPoint && (hasInteger || hasFraction)
        return isWellFormed && NumberForms.exponent(scan, isSignRequired = true) && scan.isAtEnd
    }

    fun isSexagesimalReal(text: String): Boolean {
        val scan = TextScan(text)
        scan.sign()
        val hasLeadingDigit = scan.acceptAny("0$NON_ZERO_DIGITS")
        scan.count(TextScan::isDigitOrUnderscore)
        val hasPlaces = hasLeadingDigit && places(scan)
        val hasPoint = scan.accept('.')
        scan.count(TextScan::isDigitOrUnderscore)
        return hasPlaces && hasPoint && scan.isAtEnd
    }

    fun isDate(text: String): Boolean {
        val scan = TextScan(text)
        return date(scan, isExact = true) && scan.isAtEnd
    }

    fun isTimestamp(text: String): Boolean {
        val scan = TextScan(text)
        val hasDate = date(scan, isExact = false)
        val hasSeparator = scan.acceptAny("Tt") || scan.count(TextScan::isBlank) > 0
        val hasTime = hasDate && hasSeparator && time(scan)
        return hasTime && zone(scan) && scan.isAtEnd
    }

    private fun prefixed(text: String, prefix: String, isDigit: (Char) -> Boolean): Boolean {
        val scan = TextScan(text)
        scan.sign()
        val hasPrefix = scan.acceptWord(prefix)
        val digits = scan.count { character -> isDigit(character) || character == '_' }
        return hasPrefix && digits > 0 && scan.isAtEnd
    }

    private fun places(scan: TextScan): Boolean {
        var count = 0
        while (scan.accept(':')) {
            val place = scan.take(TWO_DIGITS, TextScan::isDigit)
            val isTwoDigitPlace = place.length == TWO_DIGITS && place[0] <= HIGHEST_SEXAGESIMAL_TENS
            val isPlace = place.length == 1 || isTwoDigitPlace
            if (!isPlace) return false
            count++
        }
        return count > 0
    }

    private fun date(scan: TextScan, isExact: Boolean): Boolean {
        val least = TWO_DIGITS.takeIf { isExact } ?: 1
        val hasYear = scan.countBetween(least = YEAR_DIGITS, most = YEAR_DIGITS, isAccepted = TextScan::isDigit)
        val hasMonth = hasYear && scan.accept('-') && upToTwoDigits(scan, least)
        return hasMonth && scan.accept('-') && upToTwoDigits(scan, least)
    }

    private fun time(scan: TextScan): Boolean {
        val hasHour = upToTwoDigits(scan, least = 1)
        val hasMinute = hasHour && scan.accept(':') && upToTwoDigits(scan, least = TWO_DIGITS)
        val hasSecond = hasMinute && scan.accept(':') && upToTwoDigits(scan, least = TWO_DIGITS)
        if (hasSecond && scan.accept('.')) scan.count(TextScan::isDigit)
        return hasSecond
    }

    private fun zone(scan: TextScan): Boolean {
        val blanks = scan.count(TextScan::isBlank)
        if (scan.isAtEnd) return blanks == 0
        return scan.accept('Z') || offset(scan)
    }

    private fun offset(scan: TextScan): Boolean {
        val hasHours = scan.sign() && upToTwoDigits(scan, least = 1)
        val hasMinutes = !scan.accept(':') || upToTwoDigits(scan, least = TWO_DIGITS)
        return hasHours && hasMinutes
    }

    private fun upToTwoDigits(scan: TextScan, least: Int): Boolean =
        scan.countBetween(least = least, most = TWO_DIGITS, isAccepted = TextScan::isDigit)
}
