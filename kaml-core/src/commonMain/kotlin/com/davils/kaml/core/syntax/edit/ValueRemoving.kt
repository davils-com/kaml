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

import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.core.syntax.edit.YamlPath.Segment
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange

internal class ValueRemoving(private val tree: YamlSyntaxTree, private val layout: NodeLayout) {
    fun remove(parent: YamlValueSyntax, segment: Segment): EditStep = when (segment) {
        is Segment.Key -> removedEntry(parent, segment.name)
        is Segment.Index -> removedItem(parent, segment.index)
    }

    private fun removedEntry(parent: YamlValueSyntax, name: String): EditStep {
        val mapping = parent as? YamlValueSyntax.Mapping ?: return EditStep.Refused(YamlEditRefusal.NotAMapping)
        return removedEntry(mapping, name)
    }

    private fun removedEntry(mapping: YamlValueSyntax.Mapping, name: String): EditStep {
        val entry = mapping.entry(name) ?: return EditStep.Refused(YamlEditRefusal.MissingPath)
        return pairRemoval(mapping) ?: removal(mapping.isFlow, mapping.entries.map(YamlEntrySyntax::node), entry.node)
    }

    private fun pairRemoval(mapping: YamlValueSyntax.Mapping): EditStep? {
        val isPair = mapping.node.kind == YamlSyntaxKind.FLOW_PAIR
        return EditStep.Refused(YamlEditRefusal.Unsupported).takeIf { isPair }
    }

    private fun removedItem(parent: YamlValueSyntax, index: Int): EditStep {
        val sequence = parent as? YamlValueSyntax.Sequence ?: return EditStep.Refused(
            YamlEditRefusal.NotASequence,
        )
        return removedItem(sequence, index)
    }

    private fun removedItem(sequence: YamlValueSyntax.Sequence, index: Int): EditStep {
        val hosts = sequence.items.map { item -> item.node.takeIf { sequence.isFlow } ?: layout.hostOf(item.node) }
        val target = hosts.getOrNull(index) ?: return EditStep.Refused(YamlEditRefusal.MissingPath)
        return removal(sequence.isFlow, hosts, target)
    }

    private fun removal(isFlow: Boolean, siblings: List<YamlSyntaxNode>, target: YamlSyntaxNode): EditStep =
        when (isFlow) {
            true -> EditStep.Edits(layout.flowRemoval(siblings, target))
            false -> blockRemoval(target)
        }

    private fun blockRemoval(target: YamlSyntaxNode): EditStep {
        val lines = tree.lines
        val firstLine = lines.lineRange(lines.lineOf(target.range.start))
        val isLineOwned = tree.text.substring(startIndex = firstLine.start, endIndex = target.range.start).isBlank()
        if (!isLineOwned) return EditStep.Refused(YamlEditRefusal.Unsupported)
        return EditStep.Edits(TextEdit(TextRange(start = firstLine.start, end = layout.endAfter(target)), ""))
    }
}
