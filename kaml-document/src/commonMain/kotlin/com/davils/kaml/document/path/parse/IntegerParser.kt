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

internal class IntegerParser(private val cursor: PathCursor) {
    fun optional(): Long? {
        val current = cursor.current
        val isInteger = current == '-' || current in '0'..'9'
        if (!isInteger) return null
        return integer()
    }

    private fun integer(): Long {
        val isNegative = cursor.accept("-")
        val digits = cursor.takeWhile { it in '0'..'9' }
        val isZero = digits == "0" && !isNegative
        val isPositive = digits.isNotEmpty() && !digits.startsWith("0")
        val isCanonical = isZero || isPositive
        if (!isCanonical) cursor.fail("expected an integer without leading zeros")
        val magnitude = digits.toLongOrNull()?.takeIf { it <= MAXIMUM } ?: cursor.fail("the integer is out of range")
        val sign = (-1L).takeIf { isNegative } ?: 1L
        return sign * magnitude
    }

    private companion object {
        const val MAXIMUM: Long = 9_007_199_254_740_991L
    }
}
