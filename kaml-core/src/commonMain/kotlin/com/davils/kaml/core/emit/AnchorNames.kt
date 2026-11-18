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

internal class AnchorNames(
    private val reserved: Set<String> = emptySet(),
    private val taken: Set<String> = emptySet(),
) {
    private val used = HashSet<String>()
    private var counter = 0

    fun preferred(name: String?): String {
        val kept = name?.takeIf { isValid(it) && it !in used && it !in taken }
        val chosen = kept ?: generated()
        used += chosen
        return chosen
    }

    fun clear() {
        used.clear()
    }

    private fun generated(): String {
        var name: String
        do {
            counter++
            name = GENERATED_PREFIX + counter.toString().padStart(length = 3, padChar = '0')
        } while (name in used || name in reserved || name in taken)
        return name
    }

    companion object {
        private const val GENERATED_PREFIX: String = "id"
        private const val EXTRAS: String = "-_."

        fun isValid(name: String): Boolean = name.isNotEmpty() && name.all(::isAnchorCharacter)

        private fun isAnchorCharacter(character: Char): Boolean {
            val isLetter = character in 'a'..'z' || character in 'A'..'Z'
            return isLetter || character in '0'..'9' || character in EXTRAS
        }
    }
}
