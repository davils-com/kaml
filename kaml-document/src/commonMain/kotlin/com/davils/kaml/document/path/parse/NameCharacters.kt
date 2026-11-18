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

internal object NameCharacters {
    private const val FIRST_NON_ASCII: Int = 0x80

    fun isFirst(character: Char): Boolean {
        val isAlpha = character in 'a'..'z' || character in 'A'..'Z'
        return isAlpha || character == '_' || character.code >= FIRST_NON_ASCII
    }

    fun isName(character: Char): Boolean = isFirst(character) || character in '0'..'9'

    fun isWellFormed(name: String): Boolean {
        var index = 0
        var isWellFormed = true
        while (isWellFormed && index < name.length) {
            val character = name[index]
            val isPair = character.isHighSurrogate() && name.getOrNull(index + 1)?.isLowSurrogate() == true
            isWellFormed = isPair || !character.isSurrogate()
            index += 2.takeIf { isPair } ?: 1
        }
        return isWellFormed
    }

    fun isFunctionFirst(character: Char): Boolean = character in 'a'..'z'

    fun isFunction(character: Char): Boolean = isFunctionFirst(character) || character == '_' || character in '0'..'9'
}
