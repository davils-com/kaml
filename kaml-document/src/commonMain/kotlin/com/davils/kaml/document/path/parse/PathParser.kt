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

package com.davils.kaml.document.path.parse

import com.davils.kaml.document.path.query.PathQuery
import com.davils.kaml.document.path.query.PathSegment
import com.davils.kaml.document.path.query.PathSelector

internal class PathParser(private val cursor: PathCursor, maximumNumberLength: Int) {
    private val literals = LiteralParser(cursor, maximumNumberLength)
    private val numbers = IntegerParser(cursor)
    private val filters: FilterParser by lazy { FilterParser(cursor, this, literals) }

    private val selectorStarts: Map<Char, () -> PathSelector> = mapOf(
        '\'' to { PathSelector.Name(literals.string('\'')) },
        '"' to { PathSelector.Name(literals.string('"')) },
        '*' to ::wildcard,
        '?' to ::filter,
    )

    fun wholeQuery(): PathQuery {
        cursor.expect("$")
        val query = PathQuery(isRelative = false, segments = segments())
        if (!cursor.isAtEnd) cursor.fail("unexpected content")
        return query
    }

    fun filterQuery(): PathQuery? {
        val isRelative = cursor.accept("@")
        if (!isRelative && !cursor.accept("$")) return null
        return PathQuery(isRelative = isRelative, segments = segments())
    }

    private fun segments(): List<PathSegment> = generateSequence { cursor.attempt(::spacedSegment) }.toList()

    private fun spacedSegment(): PathSegment? {
        cursor.skipBlanks()
        if (cursor.accept("..")) return descendant()
        return child()
    }

    private fun child(): PathSegment? {
        if (cursor.accept(".")) return PathSegment(listOf(dotted()), isDescendant = false)
        return cursor.current.takeIf { it == '[' }?.let { PathSegment(bracketed(), isDescendant = false) }
    }

    private fun descendant(): PathSegment {
        if (cursor.current == '[') return PathSegment(bracketed(), isDescendant = true)
        return PathSegment(listOf(dotted()), isDescendant = true)
    }

    private fun dotted(): PathSelector {
        if (cursor.accept("*")) return PathSelector.Wildcard
        if (cursor.current?.let(NameCharacters::isFirst) != true) cursor.fail("expected a member name")
        val name = cursor.takeWhile(NameCharacters::isName)
        if (!NameCharacters.isWellFormed(name)) cursor.fail("a member name must not contain a lone surrogate")
        return PathSelector.Name(name)
    }

    private fun bracketed(): List<PathSelector> {
        cursor.expect("[")
        val selectors = mutableListOf(spacedSelector())
        while (cursor.accept(",")) selectors += spacedSelector()
        cursor.expect("]")
        return selectors
    }

    private fun spacedSelector(): PathSelector {
        cursor.skipBlanks()
        val read = selectorStarts[cursor.current] ?: ::indexOrSlice
        val selector = read()
        cursor.skipBlanks()
        return selector
    }

    private fun wildcard(): PathSelector {
        cursor.advance()
        return PathSelector.Wildcard
    }

    private fun filter(): PathSelector = cursor.nested {
        cursor.advance()
        cursor.skipBlanks()
        PathSelector.Filter(filters.logical())
    }

    private fun indexOrSlice(): PathSelector {
        val start = numbers.optional()
        cursor.skipBlanks()
        if (!cursor.accept(":")) return PathSelector.Index(start ?: cursor.fail("expected a selector"))
        return slice(start)
    }

    private fun slice(start: Long?): PathSelector {
        cursor.skipBlanks()
        val end = numbers.optional()
        cursor.skipBlanks()
        val hasStep = cursor.accept(":")
        cursor.skipBlanks()
        val step = numbers.optional().takeIf { hasStep }
        return PathSelector.Slice(start = start, end = end, step = step)
    }
}
