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
package com.davils.kaml.core.parser

import com.davils.kaml.core.source.TextDecoders
import com.davils.kaml.core.source.YamlEncoding

internal object TagEscapes {
    private const val HEX_RADIX: Int = 16
    private const val ESCAPE_LENGTH: Int = 3
    private const val PERCENT: Char = '%'

    fun decode(text: String): TextDecoders.Decoded {
        if (PERCENT !in text) return TextDecoders.Decoded(text = text, firstInvalidByte = null)
        val bytes = ArrayList<Byte>()
        var index = 0
        while (index < text.length) {
            index = decodeAt(text, index, bytes)
        }
        return TextDecoders.decode(bytes.toByteArray(), YamlEncoding.Utf8)
    }

    private fun decodeAt(text: String, index: Int, bytes: MutableList<Byte>): Int {
        val end = minOf(a = index + ESCAPE_LENGTH, b = text.length)
        val digits = text.substring(startIndex = minOf(a = index + 1, b = end), endIndex = end)
        val value = digits.toIntOrNull(HEX_RADIX)?.takeIf { text[index] == PERCENT && digits.length == 2 }
        if (value != null) {
            bytes.add(value.toByte())
            return index + ESCAPE_LENGTH
        }
        val encoded = text[index].toString().encodeToByteArray()
        bytes.addAll(encoded.toList())
        return index + 1
    }
}
