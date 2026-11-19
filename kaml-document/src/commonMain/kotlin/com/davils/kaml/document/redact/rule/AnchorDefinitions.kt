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

package com.davils.kaml.document.redact.rule

import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.document.internal.OrderedIndex
import com.davils.kore.core.text.TextRange

internal class AnchorDefinitions(tree: YamlSyntaxTree) {
    private val names = OrderedIndex<String>()
    private val definitions = ArrayList<MutableList<Definition>>()

    init {
        tree.root.tokens.filter { token -> token.kind == YamlSyntaxKind.ANCHOR }.forEach { anchor ->
            val owner = anchor.ancestors.elementAt(1)
            val ordinal = names.ordinalFor(anchor.text.drop(1))
            if (ordinal == definitions.size) definitions.add(ArrayList())
            definitions[ordinal].add(Definition(anchor.range.start, owner.range))
        }
    }

    fun rangeOf(name: String, aliasOffset: Int, default: TextRange): TextRange = rangeOf(name, aliasOffset) ?: default

    fun rangeOf(name: String, aliasOffset: Int): TextRange? {
        val candidates = names.ordinalOf(name)?.let(definitions::get) ?: return null
        val index = candidates.binarySearch { definition -> definition.offset.compareTo(aliasOffset) }
        val before = -index - 2
        return candidates.getOrNull(before)?.range
    }

    private data class Definition(val offset: Int, val range: TextRange)
}
