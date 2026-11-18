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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.edit.YamlEditResult
import com.davils.kaml.core.syntax.edit.YamlEditor
import com.davils.kaml.core.syntax.edit.YamlPath
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.patch.JsonPatchRefusal
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.PointerSteps
import com.davils.kaml.document.pointer.PointerTokens
import com.davils.kore.core.text.TextEdit

internal class SyntaxEdits(
    private val tree: YamlSyntaxTree,
    private val document: Int,
    private val root: YamlNode,
    private val budget: WorkBudget,
) {
    private val editor = YamlEditor(tree, document)

    fun added(path: JsonPointer, value: YamlNode): SyntaxStep = bounded { addedAt(path, value) }

    fun removed(path: JsonPointer): SyntaxStep = bounded { stepOf(editor.remove(pathOf(path))) }

    fun replaced(path: JsonPointer, value: YamlNode): SyntaxStep = bounded { replacedAt(path, value) }

    private fun addedAt(path: JsonPointer, value: YamlNode): SyntaxStep {
        val parent = path.parent ?: return rootReplaced(value)
        return stepOf(addedBelow(parent, path.tokens.last(), value))
    }

    private fun replacedAt(path: JsonPointer, value: YamlNode): SyntaxStep {
        if (path.isRoot) return rootReplaced(value)
        return stepOf(editor.set(pathOf(path), value))
    }

    private fun bounded(edit: () -> SyntaxStep): SyntaxStep = try {
        edit()
    } catch (expected: IllegalStateException) {
        SyntaxStep.Refused(JsonPatchRefusal.LimitExceeded)
    }

    private fun addedBelow(parent: JsonPointer, token: String, value: YamlNode): YamlEditResult {
        val location = pathOf(parent)
        if (PointerSteps.resolved(root, parent, budget) is YamlNode.Sequence) return inserted(location, token, value)
        return editor.set(location.key(token), value)
    }

    private fun inserted(location: YamlPath, token: String, value: YamlNode): YamlEditResult {
        val index = PointerTokens.indexOf(token) ?: return editor.append(location, value)
        return editor.insert(location.index(index), value)
    }

    private fun pathOf(pointer: JsonPointer): YamlPath {
        var path = YamlPath.ROOT
        var parent: YamlNode? = root
        pointer.tokens.forEach { token ->
            val index = PointerTokens.indexOf(token)?.takeIf { parent is YamlNode.Sequence }
            path = index?.let(path::index) ?: path.key(token)
            parent = parent?.let { node -> PointerSteps.child(node, token, budget) }
        }
        return path
    }

    private fun rootReplaced(value: YamlNode): SyntaxStep {
        val syntax = tree.documents.getOrNull(document) ?: return SyntaxStep.Refused(JsonPatchRefusal.MissingDocument)
        val text = tree.yaml.emit(value).trimEnd('\r', '\n')
        val range = ContentRanges.of(syntax.root.node)
        val isGlued = range.isEmpty && range.start > 0 && !tree.text[range.start - 1].isWhitespace()
        val edit = TextEdit(range, " ".takeIf { isGlued }.orEmpty() + text)
        return SyntaxStep.Done(tree.edit(listOf(edit)))
    }

    private fun stepOf(result: YamlEditResult): SyntaxStep = when (result) {
        is YamlEditResult.Edited -> SyntaxStep.Done(result.tree)
        is YamlEditResult.Refused -> SyntaxStep.Refused(JsonPatchRefusal.Unsupported)
    }
}
