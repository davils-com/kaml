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

package com.davils.kaml.core.syntax.edit

import com.davils.kaml.core.internal.NameSet
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kaml.core.syntax.YamlSyntaxToken
import com.davils.kore.core.text.TextEdit

internal class AnchorGuard(document: YamlSyntaxNode) {
    private val properties: List<YamlSyntaxToken> = document.tokens.filter { it.kind in nameKinds }.toList()

    val names: Set<String> = NameSet(properties.map(::nameOf))

    fun keepsAliases(edits: List<TextEdit>): Boolean = edits.all(::keepsAliases)

    private fun keepsAliases(edit: TextEdit): Boolean {
        val removed = edit.range
        val pending = NameSet(properties.filter { token -> isRemovedAnchor(token, edit) }.map(::nameOf))
        val later = properties.filter { token -> token.range.start >= removed.end }
        var isKept = true
        later.forEach { token ->
            val name = nameOf(token)
            val isPending = name in pending
            isKept = isKept && !(isPending && token.kind == YamlSyntaxKind.ALIAS)
            if (isPending) pending.remove(name)
        }
        return isKept
    }

    private fun isRemovedAnchor(token: YamlSyntaxToken, edit: TextEdit): Boolean {
        val removed = edit.range
        val isOverlapping = token.range.start < removed.end && removed.start < token.range.end
        return token.kind == YamlSyntaxKind.ANCHOR && isOverlapping
    }

    private fun nameOf(token: YamlSyntaxToken): String = token.text.drop(1)

    private companion object {
        val nameKinds: Set<YamlSyntaxKind> = setOf(YamlSyntaxKind.ANCHOR, YamlSyntaxKind.ALIAS)
    }
}
