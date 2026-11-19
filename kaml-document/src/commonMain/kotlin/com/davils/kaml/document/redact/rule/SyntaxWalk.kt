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

import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.internal.WorkBudget

internal class SyntaxWalk(tree: YamlSyntaxTree, budget: WorkBudget, maximumDepth: Int) {
    val scalars: List<YamlValueSyntax.Scalar>

    val mappings: List<YamlValueSyntax.Mapping>

    val keyScalars: List<YamlValueSyntax.Scalar>

    init {
        val foundScalars = ArrayList<YamlValueSyntax.Scalar>()
        val foundMappings = ArrayList<YamlValueSyntax.Mapping>()
        val pending = ArrayDeque<Pending>()
        tree.documents.forEach { document -> pending.addLast(Pending(value = document.root, depth = 0)) }
        while (pending.isNotEmpty()) {
            val current = pending.removeLast()
            budget.charge()
            if (current.depth > maximumDepth) throw YamlDocumentLimitException("maximumDepth")
            val value = current.value
            (value as? YamlValueSyntax.Scalar)?.let(foundScalars::add)
            (value as? YamlValueSyntax.Mapping)?.let(foundMappings::add)
            val childDepth = current.depth + 1
            childrenOf(value).forEach { child -> pending.addLast(Pending(value = child, depth = childDepth)) }
        }
        scalars = foundScalars
        mappings = foundMappings
        val entries = foundMappings.flatMap(YamlValueSyntax.Mapping::entries)
        keyScalars = entries.mapNotNull { entry -> entry.key as? YamlValueSyntax.Scalar }
    }

    private fun childrenOf(value: YamlValueSyntax): List<YamlValueSyntax> = when (value) {
        is YamlValueSyntax.Sequence -> value.items
        is YamlValueSyntax.Mapping -> value.entries.flatMap(::entryChildren)
        is YamlValueSyntax.Scalar, is YamlValueSyntax.Alias, is YamlValueSyntax.Empty -> emptyList()
    }

    private fun entryChildren(entry: YamlEntrySyntax): List<YamlValueSyntax> {
        val key = entry.key.takeIf { it !is YamlValueSyntax.Scalar }
        return listOfNotNull(key, entry.value)
    }

    private data class Pending(val value: YamlValueSyntax, val depth: Int)
}
