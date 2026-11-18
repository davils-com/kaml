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
package com.davils.kaml.core.compose

import com.davils.kaml.core.parser.TagEscapes

internal object TagResolution {
    private const val VERBATIM_PREFIX: String = "!<"
    private const val VERBATIM_SUFFIX: String = ">"
    private const val NON_SPECIFIC: String = "!"

    fun resolve(text: String, directives: DocumentDirectives): String? {
        if (text.startsWith(VERBATIM_PREFIX)) return text.removePrefix(VERBATIM_PREFIX).removeSuffix(VERBATIM_SUFFIX)
        val handle = handleOf(text)
        return directives.prefixOf(handle)?.plus(TagEscapes.decode(text.substring(handle.length)).text)
    }

    fun handleOf(text: String): String {
        if (text == NON_SPECIFIC) return NON_SPECIFIC
        val second = text.indexOf('!', startIndex = 1)
        return text.substring(startIndex = 0, endIndex = second + 1).takeIf { second > 0 } ?: NON_SPECIFIC
    }
}
