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

package com.davils.kaml.serialization.json

internal enum class JsonNumberShape {
    Integer,
    Real,
    None,
    ;

    companion object {
        fun of(text: String): JsonNumberShape {
            val scan = Scan(text)
            scan.accept('-')
            val hasInteger = scan.accept('0') || scan.digits(leading = true)
            val hasFraction = scan.accept('.')
            val isFractionValid = !hasFraction || scan.digits(leading = false)
            val hasExponent = scan.accept('e') || scan.accept('E')
            val isExponentValid = !hasExponent || scan.exponent()
            val isValid = hasInteger && isFractionValid && isExponentValid && scan.isAtEnd
            val isInteger = !hasFraction && !hasExponent
            return Integer.takeIf { isValid && isInteger } ?: Real.takeIf { isValid } ?: None
        }
    }

    private class Scan(private val text: String) {
        private var index = 0

        val isAtEnd: Boolean get() = index == text.length

        fun accept(character: Char): Boolean {
            val isAccepted = text.getOrNull(index) == character
            if (isAccepted) index++
            return isAccepted
        }

        fun digits(leading: Boolean): Boolean {
            val first = text.getOrNull(index)
            val isFirstAllowed = first != null && first in '0'..'9' && (!leading || first != '0')
            if (!isFirstAllowed) return false
            while (text.getOrNull(index)?.let { character -> character in '0'..'9' } == true) index++
            return true
        }

        fun exponent(): Boolean {
            if (!accept('+')) accept('-')
            return digits(leading = false)
        }
    }
}
