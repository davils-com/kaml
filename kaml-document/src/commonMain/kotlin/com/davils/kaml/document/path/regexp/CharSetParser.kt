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

private const val LAST_CODE_POINT: Int = 0x10FFFF
private const val FIRST_SURROGATE: Int = 0xD800
private const val LAST_SURROGATE: Int = 0xDFFF
private const val UNIVERSAL: String = "L"

private val specials: Set<Int> = "()*+.?[\\]{|}".map(Char::code).toSet()
private val singleEscapes: Set<Int> = "()*+-.?[\\]^nrt{|}".map(Char::code).toSet()
private val escapedValues: Map<Int, Int> = mapOf('n'.code to '\n'.code, 'r'.code to '\r'.code, 't'.code to '\t'.code)
private val categories: Set<String> = setOf(
    "L", "Ll", "Lm", "Lo", "Lt", "Lu", "M", "Mc", "Me", "Mn", "N", "Nd", "Nl", "No",
    "P", "Pc", "Pd", "Pe", "Pf", "Pi", "Po", "Ps", "Z", "Zl", "Zp", "Zs",
    "S", "Sc", "Sk", "Sm", "So", "C", "Cc", "Cf", "Cn", "Co",
)

internal class CharSetParser(private val cursor: RegexpCursor) {
    private val handlers: Map<Int, () -> CharSet> = mapOf(
        '.'.code to { CharSet.AnyButLineBreak },
        '\\'.code to ::escape,
        '['.code to ::charClass,
    )

    fun charSetOf(codePoint: Int): CharSet {
        val handler = handlers[codePoint] ?: return normal(codePoint)
        return handler()
    }

    private fun normal(codePoint: Int): CharSet {
        cursor.require(isNormal(codePoint))
        return CharSet.Single(codePoint)
    }

    private fun escape(): CharSet {
        cursor.require(!cursor.isAtEnd)
        val codePoint = cursor.next()
        if (codePoint == 'p'.code || codePoint == 'P'.code) return category(isNegated = codePoint == 'P'.code)
        cursor.require(codePoint in singleEscapes)
        return CharSet.Single(escapedValues[codePoint] ?: codePoint)
    }

    private fun category(isNegated: Boolean): CharSet {
        cursor.expect('{')
        val name = buildString {
            while (!cursor.isAtEnd && cursor.peek() != '}'.code) append(cursor.next().toChar())
        }
        cursor.expect('}')
        val isKnown = name in categories
        cursor.require(isKnown)
        return CharSet.Category(categoryTest(name.takeIf { isKnown } ?: UNIVERSAL), isNegated)
    }

    private fun charClass(): CharSet {
        val isNegated = cursor.accept('^')
        val members = ArrayList<CharSet>()
        if (cursor.accept('-')) members.add(CharSet.Single('-'.code))
        while (cursor.isValid && !cursor.isAtEnd && cursor.peek() != ']'.code) members.add(classMember())
        cursor.expect(']')
        cursor.require(members.isNotEmpty())
        return CharSet.Union(members, isNegated)
    }

    private fun classMember(): CharSet {
        if (isTrailingDash()) return CharSet.Single(cursor.next())
        return rangeOrSingle(classCharacter())
    }

    private fun rangeOrSingle(first: CharSet): CharSet {
        val single = (first as? CharSet.Single)?.takeIf { !isTrailingDash() && cursor.accept('-') } ?: return first
        val last = classCharacter() as? CharSet.Single
        cursor.require(last != null && single.codePoint <= last.codePoint)
        return CharSet.Range(first = single.codePoint, last = last?.codePoint ?: single.codePoint)
    }

    private fun isTrailingDash(): Boolean = cursor.peek() == '-'.code && cursor.peek(1) == ']'.code

    private fun classCharacter(): CharSet {
        cursor.require(!cursor.isAtEnd)
        val codePoint = cursor.next()
        if (codePoint == '\\'.code) return escape()
        cursor.require(isClassCharacter(codePoint))
        return CharSet.Single(codePoint)
    }

    private fun isNormal(codePoint: Int): Boolean = codePoint !in specials && isScalarValue(codePoint)

    private fun isClassCharacter(codePoint: Int): Boolean {
        val isSpecial = codePoint == '-'.code || codePoint in '['.code..']'.code
        return !isSpecial && isScalarValue(codePoint)
    }

    private fun isScalarValue(codePoint: Int): Boolean {
        val isSurrogate = codePoint in FIRST_SURROGATE..LAST_SURROGATE
        return codePoint in 0..LAST_CODE_POINT && !isSurrogate
    }
}
