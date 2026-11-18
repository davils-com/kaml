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

import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kore.core.text.TextRange

internal object SourceDecoding {
    private const val ZERO: Byte = 0
    private const val BOM_FIRST: Byte = 0xFE.toByte()
    private const val BOM_SECOND: Byte = 0xFF.toByte()
    private const val WIDE: Int = 4
    private const val NARROW: Int = 2

    fun decode(source: YamlSource): DecodedSource {
        val text = source.text
        if (text != null) return DecodedSource(text, YamlEncoding.Utf8, emptyList())
        return decode(source.bytes)
    }

    fun encodingOf(bytes: ByteArray): YamlEncoding {
        val head = bytes.copyOf(WIDE)
        return wide(head, bytes.size) ?: narrow(head, bytes.size) ?: YamlEncoding.Utf8
    }

    private fun decode(bytes: ByteArray): DecodedSource {
        val encoding = encodingOf(bytes)
        val decoded = TextDecoders.decode(bytes, encoding)
        val diagnostics = listOfNotNull(decoded.firstInvalidByte?.let { offset -> invalid(encoding, offset) })
        return DecodedSource(decoded.text, encoding, diagnostics)
    }

    private fun invalid(encoding: YamlEncoding, offset: Int): YamlDiagnostic = YamlDiagnostic(
        code = YamlDiagnosticCode.InvalidEncoding,
        range = TextRange.at(0),
        message = "The source is not valid $encoding from byte $offset on",
    )

    private fun wide(head: ByteArray, size: Int): YamlEncoding? {
        if (size < WIDE) return null
        val isBigEndian = head[0] == ZERO && head[1] == ZERO && (head[2] != ZERO || head[3] != ZERO)
        val isLittleEndian = head[2] == ZERO && head[3] == ZERO && (head[0] != ZERO || head[1] != ZERO)
        val isLittleMark = head[0] == BOM_SECOND && head[1] == BOM_FIRST && head[2] == ZERO && head[3] == ZERO
        val little = YamlEncoding.Utf32LittleEndian.takeIf { isLittleEndian || isLittleMark }
        return YamlEncoding.Utf32BigEndian.takeIf { isBigEndian } ?: little
    }

    private fun narrow(head: ByteArray, size: Int): YamlEncoding? {
        if (size < NARROW) return null
        val isBigMark = head[0] == BOM_FIRST && head[1] == BOM_SECOND
        val isBigPattern = head[0] == ZERO && head[1] != ZERO
        val isLittleMark = head[0] == BOM_SECOND && head[1] == BOM_FIRST
        val isLittlePattern = head[0] != ZERO && head[1] == ZERO
        val isBigEndian = isBigMark || isBigPattern
        val isLittleEndian = isLittleMark || isLittlePattern
        val little = YamlEncoding.Utf16LittleEndian.takeIf { isLittleEndian }
        return YamlEncoding.Utf16BigEndian.takeIf { isBigEndian } ?: little
    }
}
