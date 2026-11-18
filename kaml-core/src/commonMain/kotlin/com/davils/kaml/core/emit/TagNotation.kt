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

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlTagDirective

internal class TagNotation(directives: List<YamlTagDirective>) {
    private val handles: Set<String> = handlesOf(directives)

    fun of(tag: YamlTag): String = shorthand(tag) ?: "!<${UriText.verbatim(tag.uri)}>"

    fun directive(directive: YamlTagDirective): String = "%TAG ${directive.handle} ${UriText.prefix(directive.prefix)}"

    private fun shorthand(tag: YamlTag): String? {
        val uri = tag.uri
        val isCoreFree = tag.isCore && SECONDARY !in handles
        val isLocalFree = uri.startsWith(PRIMARY) && PRIMARY !in handles
        val core = uri.removePrefix(YamlTag.CORE_TAG_PREFIX).takeIf { suffix -> isCoreFree && suffix.isNotEmpty() }
        val local = uri.removePrefix(PRIMARY).takeIf { isLocalFree }
        val coreText = core?.let { suffix -> SECONDARY + UriText.suffix(suffix) }
        return coreText ?: local?.let { suffix -> PRIMARY + UriText.suffix(suffix) }
    }

    private fun handlesOf(directives: List<YamlTagDirective>): Set<String> {
        val seen = HashSet<String>()
        directives.forEach { directive ->
            require(isHandle(directive.handle)) { INVALID_HANDLE }
            require(directive.prefix.isNotEmpty()) { MISSING_PREFIX }
            require(seen.add(directive.handle)) { REPEATED_HANDLE }
        }
        return seen
    }

    private fun isHandle(handle: String): Boolean {
        val name = handle.removeSurrounding(PRIMARY)
        val isSurrounded = name.length + SECONDARY.length == handle.length
        val isNamed = isSurrounded && name.isNotEmpty() && name.all(::isNameCharacter)
        return handle == PRIMARY || handle == SECONDARY || isNamed
    }

    private fun isNameCharacter(character: Char): Boolean {
        val isLetter = character in 'a'..'z' || character in 'A'..'Z'
        return isLetter || character in '0'..'9' || character == '-'
    }

    companion object {
        const val INVALID_HANDLE: String =
            "A %TAG handle must be !, !! or ! followed by ASCII letters, digits or hyphens and a closing !"
        const val MISSING_PREFIX: String = "A %TAG directive needs a prefix"
        const val REPEATED_HANDLE: String = "A %TAG handle is declared twice in one document"

        private const val PRIMARY: String = "!"
        private const val SECONDARY: String = "!!"
    }
}
