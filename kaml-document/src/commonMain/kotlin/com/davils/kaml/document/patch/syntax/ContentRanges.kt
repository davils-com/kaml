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

package com.davils.kaml.document.patch.syntax

import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kaml.core.syntax.YamlSyntaxToken
import com.davils.kore.core.text.TextRange

internal object ContentRanges {
    fun of(node: YamlSyntaxNode): TextRange {
        val tokens = node.tokens.filterNot { it.kind.isTrivia }.toList()
        val content = tokens.dropWhile(::isProperty)
        val first = content.firstOrNull() ?: return TextRange.at(node.range.start)
        return TextRange(start = first.range.start, end = endOf(content.last()))
    }

    private fun isProperty(token: YamlSyntaxToken): Boolean =
        token.ancestors.any { ancestor -> ancestor.kind == YamlSyntaxKind.PROPERTIES }

    private fun endOf(token: YamlSyntaxToken): Int {
        val trailing = token.text.length - token.text.trimEnd('\r', '\n').length
        return token.range.end - trailing
    }
}
