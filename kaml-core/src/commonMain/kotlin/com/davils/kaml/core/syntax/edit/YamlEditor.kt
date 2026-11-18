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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.core.syntax.edit.YamlPath.Segment
import com.davils.kore.core.text.TextEdit

/**
 * Edits a document of a [YamlSyntaxTree] by its values, keeping every comment, blank line and the layout of all text
 * it does not touch.
 *
 * Each operation addresses a value with a [YamlPath] from the root of the document and returns either the edited
 * tree, together with the text edits that produced it, or the reason it refused. The original tree never changes, so
 * an editor is immutable and safe to share between threads; to make several edits, create an editor for each result.
 *
 * Values are written by the emitter of the configuration of the tree, in the indentation and the line breaks of the
 * document, and keys as single-line implicit keys. They are written for the YAML version the edited document declares
 * and for the schema the configuration reads it with, so in a document that declares `%YAML 1.1` a string such as
 * `yes` or a key `<<` is quoted and keeps its meaning. Neither can inject YAML: a key or a scalar that could be read as
 * anything else, such as an indicator, a comment, a document marker or a line break, is quoted and escaped, so the
 * edited document parses back to exactly the string that was set. A comment is split into lines, each written behind
 * a `#`, and refused if it contains a control, separator, bidirectional or invisible character. Text that YAML cannot
 * represent at all, a lone surrogate, U+FFFE or U+FFFF, is refused as well.
 *
 * Aliases keep what they refer to. A value with shared nodes is written with anchors whose names the edited document
 * does not use yet, whatever anchor names its nodes carry, so no new anchor can capture an alias of the document. An
 * edit that would remove or replace a value or a key whose anchor an alias later in the document refers to is refused
 * with [YamlEditRefusal.AnchorInUse].
 *
 * The edited text is parsed again with the limits of the configuration of the tree; the result carries the
 * diagnostics of that parse, so an edit that makes the text longer than `YamlLimits.maximumInputLength` yields a tree
 * with an `InputTooLong` diagnostic. Exceptions the emitter throws for a value it refuses to write, such as one that
 * nests too deep, are passed on.
 *
 * @param tree the tree whose document is edited.
 * @param document the zero-based index of the document to edit among [YamlSyntaxTree.documents]; an index without a
 * document makes every edit refuse with [YamlEditRefusal.MissingDocument].
 * @since 1.0.0
 */
public class YamlEditor(private val tree: YamlSyntaxTree, private val document: Int = 0) {
    private val syntax = tree.documents.getOrNull(document)
    private val writer = EditWriter(tree, syntax)
    private val setting = ValueSetting(writer)
    private val removing = ValueRemoving(tree, writer.layout)
    private val annotating = EntryAnnotating(tree, writer)

    /**
     * Sets the value at [path] to [value]: replaces the value of an existing key or an existing item, or adds a new
     * entry after the last entry of a mapping when the key does not exist.
     *
     * A block value is replaced in block style, a value inside a flow collection in flow style. A comment behind the
     * old value stays on its line: behind the new value, or behind the key or the `-` when the new value spans several
     * lines.
     *
     * @param path the path of the value to set, ending with a key or an index.
     * @param value the new value; its scalars may contain any text YAML can represent.
     * @return the edited tree, or the reason the edit was refused.
     * @throws IllegalArgumentException if [value] or the new key contains a lone surrogate, U+FFFE or U+FFFF.
     * @since 1.0.0
     */
    public fun set(path: YamlPath, value: YamlNode): YamlEditResult {
        EditInput.requireWritable(value)
        return edit(path) { parent, segment -> setting.set(parent, segment, value) }
    }

    /**
     * Adds [value] as the last item of the sequence at [path].
     *
     * @param path the path of the sequence.
     * @param value the item to add; its scalars may contain any text YAML can represent.
     * @return the edited tree, or the reason the edit was refused.
     * @throws IllegalArgumentException if [value] contains a lone surrogate, U+FFFE or U+FFFF.
     * @since 1.0.0
     */
    public fun append(path: YamlPath, value: YamlNode): YamlEditResult {
        EditInput.requireWritable(value)
        return edit(path.index(Int.MAX_VALUE)) { parent, _ -> setting.append(parent, value) }
    }

    /**
     * Inserts [value] into a sequence before the item at the index [path] ends with, or appends it when that index
     * is the size of the sequence.
     *
     * @param path the path of the position, ending with an index.
     * @param value the item to insert; its scalars may contain any text YAML can represent.
     * @return the edited tree, or the reason the edit was refused.
     * @throws IllegalArgumentException if [value] contains a lone surrogate, U+FFFE or U+FFFF.
     * @since 1.0.0
     */
    public fun insert(path: YamlPath, value: YamlNode): YamlEditResult {
        EditInput.requireWritable(value)
        return edit(path) { parent, segment -> setting.insert(parent, segment, value) }
    }

    /**
     * Removes the entry or the item at [path]; a block entry or item is removed with its lines.
     *
     * @param path the path of the entry or item, ending with a key or an index.
     * @return the edited tree, or the reason the edit was refused.
     * @since 1.0.0
     */
    public fun remove(path: YamlPath): YamlEditResult = edit(
        path,
    ) { parent, segment -> removing.remove(parent, segment) }

    /**
     * Replaces the key of the entry at [path] by [key] and keeps its value and comments.
     *
     * @param path the path of the entry, ending with a key.
     * @param key the new key, which may be any text YAML can represent; it is quoted where it must be.
     * @return the edited tree, or the reason the edit was refused, [YamlEditRefusal.Unsupported] if the written key
     * would be longer than the 1024 characters of an implicit key.
     * @throws IllegalArgumentException if [key] contains a lone surrogate, U+FFFE or U+FFFF.
     * @since 1.0.0
     */
    public fun rename(path: YamlPath, key: String): YamlEditResult =
        edit(path) { parent, segment -> annotating.rename(parent, segment, key) }

    /**
     * Writes [comment] as comment lines above the entry at [path], at the column of the entry.
     *
     * Each string of [comment] is split at its line breaks, and every line becomes a comment line of its own; an
     * empty string becomes an empty comment line. Entries of flow mappings cannot be commented.
     *
     * @param path the path of the entry, ending with a key.
     * @param comment the lines of the comment, without their `#`.
     * @return the edited tree, or the reason the edit was refused.
     * @throws IllegalArgumentException if a line of [comment] contains a control character other than a tab, a line
     * or paragraph separator, a bidirectional control, an invisible character or a lone surrogate, which could break
     * the YAML or hide text from a reader.
     * @since 1.0.0
     */
    public fun comment(path: YamlPath, comment: List<String>): YamlEditResult {
        val lines = EditInput.commentLines(comment)
        return edit(path) { parent, segment -> annotating.comment(parent, segment, lines) }
    }

    private fun edit(path: YamlPath, operation: (YamlValueSyntax, Segment) -> EditStep): YamlEditResult =
        when (val step = stepOf(path, operation)) {
            is EditStep.Edits -> edited(step.edits, path)
            is EditStep.Refused -> YamlEditResult.Refused(step.reason, path)
        }

    private fun edited(edits: List<TextEdit>, path: YamlPath): YamlEditResult {
        val isAliasKept = writer.anchors?.keepsAliases(edits) != false
        if (!isAliasKept) return YamlEditResult.Refused(YamlEditRefusal.AnchorInUse, path)
        return YamlEditResult.Edited(edits, tree.edit(edits))
    }

    private fun stepOf(path: YamlPath, operation: (YamlValueSyntax, Segment) -> EditStep): EditStep {
        val root = syntax?.root ?: return EditStep.Refused(YamlEditRefusal.MissingDocument)
        return stepIn(root, path, operation)
    }

    private fun stepIn(
        root: YamlValueSyntax,
        path: YamlPath,
        operation: (YamlValueSyntax, Segment) -> EditStep,
    ): EditStep {
        val target = EditPaths.locate(root, path) ?: return EditStep.Refused(YamlEditRefusal.MissingPath)
        return operation(target.parent, target.segment)
    }
}
