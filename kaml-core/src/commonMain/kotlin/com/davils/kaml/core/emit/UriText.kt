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

package com.davils.kaml.core.emit

internal object UriText {
    const val LONE_SURROGATE: String =
        "A tag or a tag prefix cannot contain a lone surrogate, which YAML cannot represent"

    private const val HEX: String = "0123456789ABCDEF"
    private const val NIBBLE_BITS: Int = 4
    private const val NIBBLE_MASK: Int = 0xF
    private const val BYTE_MASK: Int = 0xFF
    private const val ESCAPE_LENGTH: Int = 3
    private const val PERCENT: Char = '%'
    private const val URI_EXTRAS: String = "#;/?:@&=+$,_.!~*'()[]-"
    private const val FLOW_INDICATORS: String = ",[]{}"
    private const val TAG_EXCLUDED: String = "!$FLOW_INDICATORS"

    fun suffix(text: String): String = encoded(text) { index -> isTagCharacter(text[index]) }

    fun verbatim(text: String): String = encoded(text) { index -> isUriCharacter(text, index) }

    fun prefix(text: String): String = encoded(text) { index ->
        val isFlowStart = index == 0 && text[index] in FLOW_INDICATORS
        isUriCharacter(text, index) && !isFlowStart
    }

    private fun encoded(text: String, isKept: (Int) -> Boolean): String {
        val result = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val codePoint = UnsafeCharacters.codePointAt(text, index)
            val width = UnsafeCharacters.widthOf(codePoint)
            require(!text[index].isSurrogate() || width == 2) { LONE_SURROGATE }
            val isLiteral = isKept(index)
            if (isLiteral) result.append(text[index])
            if (!isLiteral) percentEncode(text.substring(startIndex = index, endIndex = index + width), result)
            index += width
        }
        return result.toString()
    }

    private fun percentEncode(character: String, result: StringBuilder) {
        character.encodeToByteArray().forEach { byte ->
            val value = byte.toInt() and BYTE_MASK
            result.append(PERCENT)
            result.append(HEX[value shr NIBBLE_BITS])
            result.append(HEX[value and NIBBLE_MASK])
        }
    }

    private fun isTagCharacter(character: Char): Boolean {
        val isTagExtra = character in URI_EXTRAS && character !in TAG_EXCLUDED
        return isWordCharacter(character) || isTagExtra
    }

    private fun isUriCharacter(text: String, index: Int): Boolean {
        val character = text[index]
        val isEscape = character == PERCENT && isEscapeAt(text, index)
        return isWordCharacter(character) || character in URI_EXTRAS || isEscape
    }

    private fun isEscapeAt(text: String, index: Int): Boolean {
        val digits = text.substring(
            startIndex = index + 1,
            endIndex = minOf(a = index + ESCAPE_LENGTH, b = text.length),
        )
        return digits.length == 2 && digits.all { digit -> digit.uppercaseChar() in HEX }
    }

    private fun isWordCharacter(character: Char): Boolean {
        val isLetter = character in 'a'..'z' || character in 'A'..'Z'
        return isLetter || character in '0'..'9'
    }
}
