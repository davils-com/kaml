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

private const val DECIMAL_RADIX: Int = 10

internal class RegexpParser(pattern: String, private val maximumDepth: Int, private val maximumRepeat: Int) {
    private val cursor = RegexpCursor(pattern)
    private val charSets = CharSetParser(cursor)
    private var depth = 0

    fun parse(): RegexpNode? {
        val node = alternation()
        return node.takeIf { cursor.isValid && cursor.isAtEnd }
    }

    private fun alternation(): RegexpNode {
        val branches = arrayListOf(branch())
        while (cursor.isValid && cursor.accept('|')) branches.add(branch())
        return branches.singleOrNull() ?: RegexpNode.Alternation(branches)
    }

    private fun branch(): RegexpNode {
        val items = ArrayList<RegexpNode>()
        while (cursor.isValid && !isBranchEnd()) items.add(piece())
        return items.singleOrNull() ?: RegexpNode.Sequence(items)
    }

    private fun isBranchEnd(): Boolean = cursor.isAtEnd || cursor.peek() == '|'.code || cursor.peek() == ')'.code

    private fun piece(): RegexpNode {
        val atom = atom()
        if (cursor.accept('*')) return RegexpNode.Repeat(atom, least = 0, most = null)
        return quantified(atom)
    }

    private fun quantified(atom: RegexpNode): RegexpNode {
        val least = 1.takeIf { cursor.accept('+') } ?: 0.takeIf { cursor.accept('?') }
        if (least != null) return RegexpNode.Repeat(atom, least = least, most = 1.takeIf { least == 0 })
        return ranged(atom)
    }

    private fun ranged(atom: RegexpNode): RegexpNode {
        if (!cursor.accept('{')) return atom
        val least = number()
        val upper = upperBound(least)
        cursor.expect('}')
        val most = upper ?: least
        cursor.require(most >= least && most <= maximumRepeat)
        return RegexpNode.Repeat(atom, least = least, most = upper)
    }

    private fun upperBound(least: Int): Int? {
        if (!cursor.accept(',')) return least
        val isOpen = cursor.peek() == '}'.code
        return isOpen.takeIf { !it }?.let { number() }
    }

    private fun number(): Int {
        var value = 0
        var digits = 0
        while (cursor.peek() in '0'.code..'9'.code) {
            value = minOf(a = value * DECIMAL_RADIX + (cursor.next() - '0'.code), b = maximumRepeat + 1)
            digits++
        }
        cursor.require(digits > 0)
        return value
    }

    private fun atom(): RegexpNode {
        cursor.require(!cursor.isAtEnd)
        val codePoint = cursor.next()
        if (codePoint == '('.code) return group()
        return RegexpNode.Chars(charSets.charSetOf(codePoint))
    }

    private fun group(): RegexpNode {
        depth++
        cursor.require(depth <= maximumDepth)
        val node = alternation()
        cursor.expect(')')
        depth--
        return node
    }
}
