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

import com.davils.kaml.document.internal.CodePoints

internal class RegexpCursor(pattern: String) {
    private val codePoints = CodePoints.of(pattern)
    private var index = 0

    var isValid: Boolean = true
        private set

    val isAtEnd: Boolean get() = index >= codePoints.size

    fun peek(ahead: Int = 0): Int = codePoints.getOrElse(index + ahead) { -1 }

    fun next(): Int {
        val codePoint = peek()
        index++
        return codePoint
    }

    fun accept(character: Char): Boolean {
        val isFound = !isAtEnd && peek() == character.code
        if (isFound) index++
        return isFound
    }

    fun expect(character: Char) {
        require(accept(character))
    }

    fun require(condition: Boolean) {
        if (!condition) isValid = false
    }
}
