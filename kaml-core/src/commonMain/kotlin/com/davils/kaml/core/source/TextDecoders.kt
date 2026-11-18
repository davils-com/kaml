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
private const val ASCII_END: Int = 0x80
private const val CONTINUATION_MASK: Int = 0xC0
private const val CONTINUATION_BITS: Int = 0x3F
private const val CONTINUATION_SHIFT: Int = 6
private const val TWO_BYTE_START: Int = 0xC2
private const val THREE_BYTE_START: Int = 0xE0
private const val FOUR_BYTE_START: Int = 0xF0
private const val FOUR_BYTE_END: Int = 0xF4
private const val TWO_BYTE_LEAD_BITS: Int = 0x1F
private const val THREE_BYTE_LEAD_BITS: Int = 0x0F
private const val FOUR_BYTE_LEAD_BITS: Int = 0x07
private const val SMALLEST_THREE_BYTE: Int = 0x800
private const val SMALLEST_FOUR_BYTE: Int = 0x10000
private const val UTF16_UNIT: Int = 2
private const val UTF32_UNIT: Int = 4
private const val BITS_PER_BYTE: Int = 8
private const val SUPPLEMENTARY_START: Int = 0x10000
private const val HIGH_SURROGATE_START: Int = 0xD800
private const val LOW_SURROGATE_START: Int = 0xDC00
private const val LAST_SURROGATE: Int = 0xDFFF
private const val SURROGATE_BITS: Int = 10
private const val LOW_SURROGATE_MASK: Int = 0x3FF
private const val LAST_CODE_POINT: Int = 0x10FFFF
private const val REPLACEMENT: Int = 0xFFFD

internal object TextDecoders {
    private val shapes: List<Pair<IntRange, Shape>> = listOf(
        TWO_BYTE_START until THREE_BYTE_START to
            Shape(continuations = 1, leadBits = TWO_BYTE_LEAD_BITS, smallest = ASCII_END),
        THREE_BYTE_START until FOUR_BYTE_START to
            Shape(continuations = 2, leadBits = THREE_BYTE_LEAD_BITS, smallest = SMALLEST_THREE_BYTE),
        FOUR_BYTE_START..FOUR_BYTE_END to
            Shape(continuations = 3, leadBits = FOUR_BYTE_LEAD_BITS, smallest = SMALLEST_FOUR_BYTE),
    )

    fun decode(bytes: ByteArray, encoding: YamlEncoding, maximumLength: Int = Int.MAX_VALUE): Decoded {
        val output = Output(capacity = bytes.size / unitOf(encoding), maximumLength = maximumLength)
        when (encoding) {
            YamlEncoding.Utf8 -> utf8(bytes, output)
            YamlEncoding.Utf16LittleEndian -> utf16(bytes, isBigEndian = false, output = output)
            YamlEncoding.Utf16BigEndian -> utf16(bytes, isBigEndian = true, output = output)
            YamlEncoding.Utf32LittleEndian -> utf32(bytes, isBigEndian = false, output = output)
            YamlEncoding.Utf32BigEndian -> utf32(bytes, isBigEndian = true, output = output)
        }
        return output.finish()
    }

    private fun unitOf(encoding: YamlEncoding): Int = when (encoding) {
        YamlEncoding.Utf8 -> 1
        YamlEncoding.Utf16LittleEndian, YamlEncoding.Utf16BigEndian -> UTF16_UNIT
        YamlEncoding.Utf32LittleEndian, YamlEncoding.Utf32BigEndian -> UTF32_UNIT
    }

    private fun utf8(bytes: ByteArray, output: Output) {
        var index = 0
        while (index < bytes.size && output.isOpen) {
            val sequence = utf8Sequence(bytes, index)
            output.add(codePoint = sequence.codePoint, byteIndex = index)
            index += sequence.length
        }
    }

    private fun utf8Sequence(bytes: ByteArray, index: Int): Sequence {
        val lead = bytes[index].toInt() and BYTE_MASK
        val shape = shapes.firstOrNull { (leads, _) -> lead in leads }?.second
            ?: return Sequence(codePoint = lead.takeIf { it < ASCII_END } ?: Sequence.INVALID, length = 1)
        return continued(bytes = bytes, index = index, lead = lead, shape = shape)
    }

    private fun continued(bytes: ByteArray, index: Int, lead: Int, shape: Shape): Sequence {
        val available = continuationCount(bytes, index, shape.continuations)
        if (available < shape.continuations) return Sequence.invalid(available + 1)
        val codePoint = (1..shape.continuations).fold(lead and shape.leadBits) { value, offset ->
            value shl CONTINUATION_SHIFT or (bytes[index + offset].toInt() and CONTINUATION_BITS)
        }
        val isValid = codePoint >= shape.smallest && codePoint <= LAST_CODE_POINT && !isSurrogate(codePoint)
        return Sequence(codePoint = codePoint.takeIf { isValid } ?: Sequence.INVALID, length = shape.continuations + 1)
    }

    private fun continuationCount(bytes: ByteArray, index: Int, most: Int): Int {
        var count = 0
        while (count < most && isContinuation(bytes.getOrNull(index + count + 1))) {
            count++
        }
        return count
    }

    private fun isContinuation(byte: Byte?): Boolean {
        val value = byte?.toInt() ?: return false
        return value and BYTE_MASK and CONTINUATION_MASK == ASCII_END
    }

    private fun utf16(bytes: ByteArray, isBigEndian: Boolean, output: Output) {
        var index = 0
        while (index < bytes.size && output.isOpen) {
            val sequence = utf16Sequence(bytes, index, isBigEndian)
            output.add(codePoint = sequence.codePoint, byteIndex = index)
            index += sequence.length
        }
    }

    private fun utf16Sequence(bytes: ByteArray, index: Int, isBigEndian: Boolean): Sequence {
        val unit = unitAt(bytes = bytes, index = index, width = UTF16_UNIT, isBigEndian = isBigEndian)
            ?: return Sequence.invalid(bytes.size - index)
        val single = Sequence(codePoint = unit, length = UTF16_UNIT).takeIf { !isSurrogate(unit) }
        return single ?: surrogatePair(bytes = bytes, index = index, high = unit, isBigEndian = isBigEndian)
    }

    private fun surrogatePair(bytes: ByteArray, index: Int, high: Int, isBigEndian: Boolean): Sequence {
        val low = unitAt(bytes = bytes, index = index + UTF16_UNIT, width = UTF16_UNIT, isBigEndian = isBigEndian)
        val isLow = low != null && low in LOW_SURROGATE_START..LAST_SURROGATE
        if (high >= LOW_SURROGATE_START || low == null || !isLow) return Sequence.invalid(UTF16_UNIT)
        val upper = high - HIGH_SURROGATE_START shl SURROGATE_BITS
        return Sequence(codePoint = SUPPLEMENTARY_START + upper + (low - LOW_SURROGATE_START), length = UTF16_UNIT * 2)
    }

    private fun utf32(bytes: ByteArray, isBigEndian: Boolean, output: Output) {
        var index = 0
        while (index < bytes.size && output.isOpen) {
            val codePoint = unitAt(bytes = bytes, index = index, width = UTF32_UNIT, isBigEndian = isBigEndian)
            val isValid = codePoint != null && codePoint in 0..LAST_CODE_POINT && !isSurrogate(codePoint)
            output.add(codePoint = codePoint?.takeIf { isValid } ?: Sequence.INVALID, byteIndex = index)
            index += UTF32_UNIT
        }
    }

    private fun unitAt(bytes: ByteArray, index: Int, width: Int, isBigEndian: Boolean): Int? {
        if (index + width > bytes.size) return null
        var unit = 0
        for (position in 0 until width) {
            val reversed = width - 1 - position
            val byteIndex = index + (position.takeIf { isBigEndian } ?: reversed)
            unit = unit shl BITS_PER_BYTE or (bytes[byteIndex].toInt() and BYTE_MASK)
        }
        return unit
    }

    private fun isSurrogate(codePoint: Int): Boolean = codePoint in HIGH_SURROGATE_START..LAST_SURROGATE

    internal data class Decoded(val text: String, val firstInvalidByte: Int?)

    private data class Shape(val continuations: Int, val leadBits: Int, val smallest: Int)

    private data class Sequence(val codePoint: Int, val length: Int) {
        companion object {
            const val INVALID: Int = -1

            fun invalid(length: Int): Sequence = Sequence(codePoint = INVALID, length = maxOf(a = 1, b = length))
        }
    }

    private class Output(capacity: Int, private val maximumLength: Int) {
        private val text = StringBuilder(minOf(a = capacity, b = maximumLength) + 2)
        private var firstInvalidByte: Int? = null

        val isOpen: Boolean get() = text.length <= maximumLength

        fun add(codePoint: Int, byteIndex: Int) {
            if (codePoint == Sequence.INVALID && firstInvalidByte == null) firstInvalidByte = byteIndex
            val shown = codePoint.takeIf { it != Sequence.INVALID } ?: REPLACEMENT
            if (shown < SUPPLEMENTARY_START) {
                text.append(shown.toChar())
                return
            }
            val offset = shown - SUPPLEMENTARY_START
            text.append((HIGH_SURROGATE_START + (offset ushr SURROGATE_BITS)).toChar())
            text.append((LOW_SURROGATE_START + (offset and LOW_SURROGATE_MASK)).toChar())
        }

        fun finish(): Decoded = Decoded(text.toString(), firstInvalidByte)
    }
}
