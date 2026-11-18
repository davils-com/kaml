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

package com.davils.kaml.core.syntax.edit.fixture

import com.davils.kaml.core.syntax.fixture.hostileTexts
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter

private const val FIRST_NONCHARACTER: Char = '\uFFFE'

private val unwritablePieces: List<String> = listOf("\uD800", "\uDBFF", "\uDC00", "\uDFFF", "\uFFFE", "\uFFFF")

internal val writableTexts: Arb<String> = hostileTexts.filter(::isRepresentable)

private val withUnwritablePiece: Arb<String> =
    Arb.bind(genA = hostileTexts, genB = Arb.element(unwritablePieces), genC = hostileTexts) { before, piece, after ->
        before + piece + after
    }

internal val unwritableTexts: Arb<String> = withUnwritablePiece.filter { text -> !isRepresentable(text) }

internal fun isRepresentable(text: String): Boolean {
    val codePoints = text.indices.filterNot { index -> text[index].isLowSurrogate() && isPairedAt(text, index - 1) }
    return codePoints.all { index -> isRepresentableAt(text, index) }
}

private fun isRepresentableAt(text: String, index: Int): Boolean {
    val character = text[index]
    val isLone = character.isSurrogate() && !isPairedAt(text, index)
    return !isLone && character < FIRST_NONCHARACTER
}

private fun isPairedAt(text: String, index: Int): Boolean {
    val high = text.getOrNull(index)
    val low = text.getOrNull(index + 1)
    return high != null && low != null && high.isHighSurrogate() && low.isLowSurrogate()
}
