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

internal const val END: Char = '\uFFFF'
internal const val NONCHARACTER: Char = '\uFFFE'
internal const val BYTE_ORDER_MARK: Char = '\uFEFF'
internal const val LINE_FEED: Char = '\n'
internal const val CARRIAGE_RETURN: Char = '\r'
internal const val SPACE: Char = ' '
internal const val TAB: Char = '\t'

private const val INDICATORS: String = "-?:,[]{}#&*!|>'\"%@`"
private const val FLOW_INDICATORS: String = ",[]{}"
private const val WORD_EXTRAS: String = "-"
private const val URI_EXTRAS: String = "#;/?:@&=+$,_.!~*'()[]%"
private const val LAST_ASCII_PRINTABLE: Char = '~'
private const val NEXT_LINE: Char = '\u0085'
private const val FIRST_LATIN_PRINTABLE: Char = '\u00A0'
private const val LAST_BEFORE_SURROGATES: Char = '\uD7FF'
private const val FIRST_PRIVATE_USE: Char = '\uE000'
private const val LAST_PRINTABLE: Char = '\uFFFD'
private const val SURROGATE_PAIR: Int = 2

internal fun Char.isBreak(): Boolean = this == LINE_FEED || this == CARRIAGE_RETURN

internal fun Char.isWhite(): Boolean = this == SPACE || this == TAB

internal fun Char.isBlankOrEnd(): Boolean = isWhite() || isBreak() || this == END

internal fun Char.isFlowIndicator(): Boolean = this in FLOW_INDICATORS

internal fun Char.isIndicator(): Boolean = this in INDICATORS

internal fun Char.isPrintable(): Boolean = isAsciiPrintable() || isWideningPrintable()

internal fun CharSequence.printableLengthAt(index: Int): Int {
    val character = this[index]
    if (character.isPrintable()) return 1
    val isPair = character.isHighSurrogate() && getOrNull(index + 1)?.isLowSurrogate() == true
    return SURROGATE_PAIR.takeIf { isPair } ?: 0
}

internal fun Char.isContent(): Boolean = this != END && !isWhite() && !isBreak() && this != BYTE_ORDER_MARK

internal fun Char.isPlainSafe(inFlow: Boolean): Boolean = isContent() && !(inFlow && isFlowIndicator())

internal fun Char.isAnchorCharacter(): Boolean = isContent() && !isFlowIndicator()

internal fun Char.isWordCharacter(): Boolean = isAsciiLetterOrDigit() || this in WORD_EXTRAS

internal fun Char.isUriCharacter(): Boolean = isWordCharacter() || this in URI_EXTRAS

internal fun Char.isTagCharacter(): Boolean = isUriCharacter() && this != '!' && !isFlowIndicator()

internal fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun Char.isAsciiLetterOrDigit(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

private fun Char.isAsciiPrintable(): Boolean = this == TAB || isBreak() || this in SPACE..LAST_ASCII_PRINTABLE

private fun Char.isWideningPrintable(): Boolean = this == NEXT_LINE ||
    this in FIRST_LATIN_PRINTABLE..LAST_BEFORE_SURROGATES ||
    this in FIRST_PRIVATE_USE..LAST_PRINTABLE
