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

package com.davils.kaml.core.source

private const val BYTE_MASK: Int = 0xFF
private const val BITS_PER_BYTE: Int = 8
private const val UTF16_WIDTH: Int = 2
private const val UTF32_WIDTH: Int = 4
private const val SUPPLEMENTARY_START: Int = 0x10000
private const val HIGH_SURROGATE_START: Int = 0xD800
private const val LOW_SURROGATE_START: Int = 0xDC00
private const val SURROGATE_BITS: Int = 10

internal fun String.encodedAs(encoding: YamlEncoding): ByteArray = when (encoding) {
    YamlEncoding.Utf8 -> encodeToByteArray()
    YamlEncoding.Utf16LittleEndian -> units(map { it.code }, UTF16_WIDTH, isBigEndian = false)
    YamlEncoding.Utf16BigEndian -> units(map { it.code }, UTF16_WIDTH, isBigEndian = true)
    YamlEncoding.Utf32LittleEndian -> units(codePointList(), UTF32_WIDTH, isBigEndian = false)
    YamlEncoding.Utf32BigEndian -> units(codePointList(), UTF32_WIDTH, isBigEndian = true)
}

internal fun utf16Units(values: List<Int>, isBigEndian: Boolean): ByteArray = units(values, UTF16_WIDTH, isBigEndian)

internal fun utf32Units(values: List<Int>, isBigEndian: Boolean): ByteArray = units(values, UTF32_WIDTH, isBigEndian)

internal fun bytesOf(vararg values: Int): ByteArray = ByteArray(values.size) { index -> values[index].toByte() }

private fun String.codePointList(): List<Int> = buildList {
    var index = 0
    while (index < length) {
        val high = this@codePointList[index]
        val low = this@codePointList.getOrNull(index + 1)
        val isPair = high.isHighSurrogate() && low != null && low.isLowSurrogate()
        if (isPair) {
            val upper = high.code - HIGH_SURROGATE_START shl SURROGATE_BITS
            add(SUPPLEMENTARY_START + upper + (low.code - LOW_SURROGATE_START))
            index += 2
            continue
        }
        add(high.code)
        index++
    }
}

private fun units(values: List<Int>, width: Int, isBigEndian: Boolean): ByteArray {
    val bytes = ByteArray(values.size * width)
    values.forEachIndexed { unit, value ->
        for (position in 0 until width) {
            val shift = BITS_PER_BYTE * (width - 1 - position).takeIf { isBigEndian }.let { it ?: position }
            bytes[unit * width + position] = (value shr shift and BYTE_MASK).toByte()
        }
    }
    return bytes
}
