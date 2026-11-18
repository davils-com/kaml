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

package com.davils.kaml.document.pointer

internal object PointerTokens {
    private const val TILDE: Char = '~'
    private val escapes: Map<Char, Char> = mapOf('0' to '~', '1' to '/')

    fun escaped(token: String): String {
        val tildes = token.replace(oldValue = "~", newValue = "~0")
        return tildes.replace(oldValue = "/", newValue = "~1")
    }

    fun unescaped(token: String): String? {
        val parts = token.split(TILDE)
        val escaped = parts.drop(1)
        if (escaped.any { part -> part.firstOrNull() !in escapes }) return null
        val decoded = escaped.joinToString("") { part -> escapes.getValue(part.first()) + part.drop(1) }
        return parts.first() + decoded
    }

    fun indexOf(token: String): Int? {
        val isDigits = token.isNotEmpty() && token.all { it in '0'..'9' }
        val isCanonical = token == "0" || !token.startsWith("0")
        return token.toIntOrNull().takeIf { isDigits && isCanonical }
    }
}
