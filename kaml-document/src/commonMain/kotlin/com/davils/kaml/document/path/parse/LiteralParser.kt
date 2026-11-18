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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.core.node.yamlScalar

internal class LiteralParser(private val cursor: PathCursor, maximumNumberLength: Int) {
    private val strings = StringLiteralParser(cursor)
    private val numbers = NumberLiteralParser(cursor, maximumNumberLength)
    private val keywords: Map<String, () -> YamlNode> = mapOf(
        "true" to { yamlScalar(true) },
        "false" to { yamlScalar(false) },
        "null" to ::yamlNull,
    )

    fun string(quote: Char): String = strings.string(quote)

    fun literal(): YamlNode? {
        val quote = cursor.current?.takeIf { it == '\'' || it == '"' }
        if (quote != null) return yamlScalar(strings.string(quote))
        return number(cursor.current) ?: keyword()
    }

    private fun number(current: Char?): YamlNode? {
        val isNumber = current == '-' || current in '0'..'9'
        if (!isNumber) return null
        return numbers.read()
    }

    private fun keyword(): YamlNode? {
        val entry = keywords.entries.firstOrNull { (word, _) -> isKeyword(word) } ?: return null
        cursor.advance(entry.key.length)
        return entry.value()
    }

    private fun isKeyword(word: String): Boolean {
        val start = cursor.offset
        val isWord = cursor.accept(word)
        val isFollowedByName = cursor.current?.let(NameCharacters::isName) == true
        cursor.reset(start)
        return isWord && !isFollowedByName
    }
}
