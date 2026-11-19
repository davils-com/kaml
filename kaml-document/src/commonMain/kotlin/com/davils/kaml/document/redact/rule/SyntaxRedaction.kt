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

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.node.YamlStream
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.patch.syntax.ContentRanges
import com.davils.kaml.document.redact.YamlRedactionRefusal
import com.davils.kaml.document.redact.YamlRedactionResult
import com.davils.kore.core.text.TextEdit

internal class SyntaxRedaction(private val rules: RedactionRules) {
    fun redacted(tree: YamlSyntaxTree): YamlRedactionResult {
        val stream = (tree.compose() as? YamlParseResult.Parsed)?.stream
            ?: return YamlRedactionResult.Refused(YamlRedactionRefusal.InvalidDocument)
        return bounded(tree, stream)
    }

    private fun bounded(tree: YamlSyntaxTree, stream: YamlStream): YamlRedactionResult = try {
        planned(tree, stream)?.let { edits -> verified(tree, edits) }
            ?: YamlRedactionResult.Refused(YamlRedactionRefusal.Undecidable)
    } catch (expected: YamlDocumentLimitException) {
        YamlRedactionResult.Refused(YamlRedactionRefusal.LimitExceeded)
    }

    private fun planned(tree: YamlSyntaxTree, stream: YamlStream): List<TextEdit>? {
        val budget = WorkBudget(rules.limits.maximumNodes, "maximumNodes")
        val targets = SyntaxTargets(tree, rules, budget)
        val graphNodes = targets.graphNodes(stream) ?: return null
        val walk = SyntaxWalk(tree, budget, rules.limits.maximumDepth)
        val dropped = DroppedEntries(tree = tree, stream = stream, walk = walk, budget = budget)
        val firstNodes = graphNodes + targets.keyedNodes(walk) + dropped.nodes
        val first = Coverage(firstNodes.map(ContentRanges::of))
        val nodes = firstNodes + targets.duplicateNodes(walk, first)
        val covered = Coverage(nodes.map(ContentRanges::of))
        val isDecided = dropped.unmatched.all(covered::contains)
        val edits = RedactionEdits(tree, rules, nodes)
        return edits.all(walk).takeIf { isDecided }
    }

    private fun verified(tree: YamlSyntaxTree, edits: List<TextEdit>): YamlRedactionResult {
        val redacted = editedOrNull(tree, edits)
        val isComposed = redacted?.compose() is YamlParseResult.Parsed
        val refused = YamlRedactionResult.Refused(YamlRedactionRefusal.Undecidable)
        return redacted?.takeIf { isComposed }?.let { edited -> YamlRedactionResult.Redacted(edits, edited) } ?: refused
    }

    private fun editedOrNull(tree: YamlSyntaxTree, edits: List<TextEdit>): YamlSyntaxTree? = try {
        tree.edit(edits)
    } catch (expected: IllegalArgumentException) {
        null
    }
}
