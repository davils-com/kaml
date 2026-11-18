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

package com.davils.kaml.core.conformance.fixture

import kotlinx.serialization.json.Json

internal object JsonValues {
    fun split(text: String): List<String> {
        val values = ArrayList<String>()
        var start = skipWhitespace(text, 0)
        while (start < text.length) {
            val end = endOf(text, start)
            values.add(text.substring(startIndex = start, endIndex = end))
            start = skipWhitespace(text, end)
        }
        return values
    }

    private fun endOf(text: String, start: Int): Int = (start + 1..text.length).first { end ->
        isBoundary(text, end) && parses(text.substring(startIndex = start, endIndex = end))
    }

    private fun isBoundary(text: String, end: Int): Boolean = end == text.length || text[end].isWhitespace()

    private fun parses(candidate: String): Boolean = runCatching { Json.parseToJsonElement(candidate) }.isSuccess

    private fun skipWhitespace(text: String, from: Int): Int {
        var index = from
        while (index < text.length && text[index].isWhitespace()) {
            index++
        }
        return index
    }
}
