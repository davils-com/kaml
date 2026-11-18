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

package com.davils.kaml.core.emit

internal object CommentLines {
    private const val LINE_FEED: String = "\n"

    fun of(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val unified = text.replace(oldValue = "\r\n", newValue = LINE_FEED)
        val lines = unified.replace(oldValue = "\r", newValue = LINE_FEED).split(LINE_FEED)
        lines.forEach(::requireShowable)
        return lines
    }

    fun checked(lines: List<String>): List<String> {
        lines.forEach { line ->
            require(LINE_FEED !in line) { "A comment line cannot contain a line break: it would end the comment" }
            requireShowable(line)
        }
        return buildList(lines.size) { addAll(lines) }
    }

    private fun requireShowable(line: String) {
        val refused = UnsafeCharacters.firstUnsafeIndex(line)
        require(refused == UnsafeCharacters.NONE) {
            val notation = UnsafeCharacters.notationOf(UnsafeCharacters.codePointAt(line, refused))
            "A comment cannot contain $notation: it would break the YAML or hide text from a reader"
        }
    }
}
