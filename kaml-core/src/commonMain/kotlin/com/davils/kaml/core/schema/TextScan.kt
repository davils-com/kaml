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

internal class TextScan(private val text: String) {
    private var index = 0

    val isAtEnd: Boolean get() = index == text.length

    fun accept(character: Char): Boolean {
        if (text.getOrNull(index) != character) return false
        index++
        return true
    }

    fun acceptAny(characters: String): Boolean {
        val next = text.getOrNull(index)
        val isAccepted = next != null && next in characters
        if (isAccepted) index++
        return isAccepted
    }

    fun acceptWord(word: String): Boolean {
        if (!text.startsWith(word, index)) return false
        index += word.length
        return true
    }

    fun sign(): Boolean = acceptAny(SIGNS)

    fun count(isAccepted: (Char) -> Boolean): Int {
        val start = index
        while (text.getOrNull(index)?.let(isAccepted) == true) {
            index++
        }
        return index - start
    }

    fun countBetween(least: Int, most: Int, isAccepted: (Char) -> Boolean): Boolean {
        val taken = take(most, isAccepted)
        return taken.length >= least
    }

    fun take(most: Int, isAccepted: (Char) -> Boolean): String {
        val start = index
        while (index - start < most && text.getOrNull(index)?.let(isAccepted) == true) {
            index++
        }
        return text.substring(startIndex = start, endIndex = index)
    }

    companion object {
        const val SIGNS: String = "-+"
        const val BLANKS: String = " \t"

        fun isDigit(character: Char): Boolean = character in '0'..'9'

        fun isDigitOrUnderscore(character: Char): Boolean = isDigit(character) || character == '_'

        fun isHexDigit(character: Char): Boolean = isDigit(character) || character in 'a'..'f' || character in 'A'..'F'

        fun isOctalDigit(character: Char): Boolean = character in '0'..'7'

        fun isBinaryDigit(character: Char): Boolean = character == '0' || character == '1'

        fun isBlank(character: Char): Boolean = character in BLANKS
    }
}
