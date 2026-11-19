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
@file:OptIn(DavilsInternal::class)

package com.davils.kaml.core.compose

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.model.VisibleCodePoints
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

internal class NodePropertyReader(private val directives: DocumentDirectives, private val reports: Reports) {
    fun of(node: Located): NodeProperties {
        val tokens = node.all(YamlSyntaxKind.PROPERTIES).flatMap(Located::children)
        val anchors = tokens.filter { it.kind == YamlSyntaxKind.ANCHOR }
        val tags = tokens.filter { it.kind == YamlSyntaxKind.TAG }
        reportRepeated(anchors.drop(1) + tags.drop(1))
        val anchor = anchors.firstOrNull()?.let { token -> anchorOf(token.text) }
        val tag = tags.firstOrNull()?.let(::tagOf)
        return NodeProperties(anchor = anchor, tag = tag)
    }

    private fun anchorOf(text: String): YamlAnchor? {
        val name = text.drop(1)
        return name.takeIf(YamlAnchor::isValidName)?.let(::YamlAnchor)
    }

    private fun tagOf(token: Located): YamlTag? {
        val resolved = TagResolution.resolve(token.text, directives)
        if (resolved == null) {
            val handle = SafeText.quoted(TagResolution.handleOf(token.text))
            reports.add(YamlDiagnosticCode.UnknownTagHandle, token.range, "The tag handle $handle is not declared")
        }
        return resolved?.takeIf(::isTagText)?.let(::YamlTag)
    }

    private fun isTagText(text: String): Boolean = text.isNotEmpty() && VisibleCodePoints.isVisibleText(text)

    private fun reportRepeated(repeated: List<Located>) {
        repeated.forEach { token ->
            reports.add(YamlDiagnosticCode.DuplicateProperty, token.range, "A node has at most one anchor and one tag")
        }
    }
}
