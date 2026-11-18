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

package com.davils.kaml.document.redact.rule

internal object DoubleQuoted {
    private const val SPACE: Int = 0x20
    private const val DELETE: Int = 0x7F
    private const val LAST_CONTROL: Int = 0x9F
    private const val HEX_RADIX: Int = 16
    private const val SHORT_DIGITS: Int = 2
    private const val LONG_DIGITS: Int = 4
    private val separators: Set<Int> = setOf(0x2028, 0x2029, 0xFEFF)

    private val escapes: Map<Char, String> = mapOf(
        '"' to "\\\"",
        '\\' to "\\\\",
        '\n' to "\\n",
        '\r' to "\\r",
        '\t' to "\\t",
    )

    fun of(text: String): String = text.map(::escaped).joinToString("", prefix = "\"", postfix = "\"")

    private fun escaped(character: Char): String = escapes[character] ?: control(character) ?: character.toString()

    private fun control(character: Char): String? {
        val code = character.code
        val isShort = code < SPACE || code in DELETE..LAST_CONTROL
        val isLong = code in separators
        val short = "\\x" + code.toString(HEX_RADIX).padStart(SHORT_DIGITS, '0')
        val long = "\\u" + code.toString(HEX_RADIX).padStart(LONG_DIGITS, '0')
        return short.takeIf { isShort } ?: long.takeIf { isLong }
    }
}
