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

private val blanks: Set<Char?> = setOf(' ', '\t', '\n', '\r')

internal class PathCursor(private val text: String, private val maximumNesting: Int) {
    private var nesting: Int = 0

    var offset: Int = 0
        private set

    val isAtEnd: Boolean get() = offset >= text.length

    val current: Char? get() = text.getOrNull(offset)

    fun peek(ahead: Int): Char? = text.getOrNull(offset + ahead)

    fun advance(count: Int = 1) {
        offset += count
    }

    fun accept(prefix: String): Boolean {
        val isFound = text.startsWith(prefix, offset)
        if (isFound) advance(prefix.length)
        return isFound
    }

    fun expect(prefix: String) {
        if (!accept(prefix)) fail("expected \"$prefix\"")
    }

    fun skipBlanks() {
        while (current in blanks) advance()
    }

    fun reset(to: Int) {
        offset = to
    }

    fun <T> attempt(read: () -> T?): T? {
        val start = offset
        val value = read()
        if (value == null) reset(start)
        return value
    }

    fun <T> nested(read: () -> T): T {
        nesting++
        if (nesting > maximumNesting) fail("the expression is nested too deeply")
        val value = read()
        nesting--
        return value
    }

    fun takeWhile(predicate: (Char) -> Boolean): String {
        val start = offset
        while (current?.let(predicate) == true) advance()
        return text.substring(startIndex = start, endIndex = offset)
    }

    fun fail(reason: String): Nothing = throw PathSyntaxError(offset, reason)
}
