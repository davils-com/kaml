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
import com.davils.kaml.core.syntax.ast.YamlEntrySyntax
import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.core.syntax.edit.YamlPath.Segment

internal class ValueSetting(private val writer: EditWriter) {
    fun set(parent: YamlValueSyntax, segment: Segment, value: YamlNode): EditStep = when (segment) {
        is Segment.Key -> setKey(parent, segment.name, value)
        is Segment.Index -> setIndex(parent, segment.index, value)
    }

    fun append(parent: YamlValueSyntax, value: YamlNode): EditStep {
        val sequence = parent as? YamlValueSyntax.Sequence
            ?: return EditStep.Refused(YamlEditRefusal.NotASequence)
        return writer.appended(sequence, value)
    }

    fun insert(parent: YamlValueSyntax, segment: Segment, value: YamlNode): EditStep {
        val position = segment as? Segment.Index ?: return EditStep.Refused(YamlEditRefusal.NotASequence)
        return insertAt(parent, position.index, value)
    }

    private fun insertAt(parent: YamlValueSyntax, index: Int, value: YamlNode): EditStep {
        val sequence = parent as? YamlValueSyntax.Sequence
            ?: return EditStep.Refused(YamlEditRefusal.NotASequence)
        return insertInto(sequence, index, value)
    }

    private fun insertInto(sequence: YamlValueSyntax.Sequence, index: Int, value: YamlNode): EditStep {
        if (index == sequence.items.size) return writer.appended(sequence, value)
        return insertBefore(sequence, sequence.items.getOrNull(index), value)
    }

    private fun insertBefore(sequence: YamlValueSyntax.Sequence, item: YamlValueSyntax?, value: YamlNode): EditStep {
        if (item == null) return EditStep.Refused(YamlEditRefusal.MissingPath)
        return writer.insertedBefore(sequence, item, value)
    }

    private fun setKey(parent: YamlValueSyntax, name: String, value: YamlNode): EditStep {
        val mapping = parent as? YamlValueSyntax.Mapping ?: return EditStep.Refused(YamlEditRefusal.NotAMapping)
        val entry = mapping.entry(name)
        val replaced = entry?.let { writer.replaced(it.value, value, slotOf(mapping, it)) }
        return replaced ?: writer.inserted(mapping, name, value)
    }

    private fun slotOf(mapping: YamlValueSyntax.Mapping, entry: YamlEntrySyntax): EditWriter.Slot =
        EditWriter.Slot(column = entry.column, isFlow = mapping.isFlow, isItem = false)

    private fun setIndex(parent: YamlValueSyntax, index: Int, value: YamlNode): EditStep {
        val sequence = parent as? YamlValueSyntax.Sequence ?: return EditStep.Refused(
            YamlEditRefusal.NotASequence,
        )
        return setItem(sequence, index, value)
    }

    private fun setItem(sequence: YamlValueSyntax.Sequence, index: Int, value: YamlNode): EditStep {
        val item = sequence.items.getOrNull(index) ?: return EditStep.Refused(YamlEditRefusal.MissingPath)
        return writer.replaced(item, value, slotOf(sequence, item))
    }

    private fun slotOf(sequence: YamlValueSyntax.Sequence, item: YamlValueSyntax): EditWriter.Slot {
        val layout = writer.layout
        val column = layout.columnOf(layout.hostOf(item.node))
        return EditWriter.Slot(column = column, isFlow = sequence.isFlow, isItem = true)
    }
}
