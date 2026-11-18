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

package com.davils.kaml.document.path.regexp

private val categories: Map<Int, String> = mapOf(
    Character.UPPERCASE_LETTER.toInt() to "Lu",
    Character.LOWERCASE_LETTER.toInt() to "Ll",
    Character.TITLECASE_LETTER.toInt() to "Lt",
    Character.MODIFIER_LETTER.toInt() to "Lm",
    Character.OTHER_LETTER.toInt() to "Lo",
    Character.NON_SPACING_MARK.toInt() to "Mn",
    Character.COMBINING_SPACING_MARK.toInt() to "Mc",
    Character.ENCLOSING_MARK.toInt() to "Me",
    Character.DECIMAL_DIGIT_NUMBER.toInt() to "Nd",
    Character.LETTER_NUMBER.toInt() to "Nl",
    Character.OTHER_NUMBER.toInt() to "No",
    Character.CONNECTOR_PUNCTUATION.toInt() to "Pc",
    Character.DASH_PUNCTUATION.toInt() to "Pd",
    Character.START_PUNCTUATION.toInt() to "Ps",
    Character.END_PUNCTUATION.toInt() to "Pe",
    Character.INITIAL_QUOTE_PUNCTUATION.toInt() to "Pi",
    Character.FINAL_QUOTE_PUNCTUATION.toInt() to "Pf",
    Character.OTHER_PUNCTUATION.toInt() to "Po",
    Character.MATH_SYMBOL.toInt() to "Sm",
    Character.CURRENCY_SYMBOL.toInt() to "Sc",
    Character.MODIFIER_SYMBOL.toInt() to "Sk",
    Character.OTHER_SYMBOL.toInt() to "So",
    Character.SPACE_SEPARATOR.toInt() to "Zs",
    Character.LINE_SEPARATOR.toInt() to "Zl",
    Character.PARAGRAPH_SEPARATOR.toInt() to "Zp",
    Character.CONTROL.toInt() to "Cc",
    Character.FORMAT.toInt() to "Cf",
    Character.SURROGATE.toInt() to "Cs",
    Character.PRIVATE_USE.toInt() to "Co",
    Character.UNASSIGNED.toInt() to "Cn",
)

internal actual fun categoryTest(category: String): (Int) -> Boolean = { codePoint ->
    val actual = categories[Character.getType(codePoint)].orEmpty()
    actual.startsWith(category)
}
