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

package com.davils.kaml.document.path.evaluate

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.internal.Members
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.path.query.PathSelector

internal class Children(private val budget: WorkBudget) {
    fun of(match: PathMatch): List<PathMatch> = when (val node = match.node) {
        is YamlNode.Scalar -> {
            emptyList()
        }

        is YamlNode.Sequence -> {
            budget.charge(node.items.size.toLong())
            node.items.mapIndexed { index, item -> match.child(index.toString(), item) }
        }

        is YamlNode.Mapping -> {
            budget.charge(node.entries.size.toLong())
            node.entries.map { entry -> match.child(Members.tokenOf(entry.key), entry.value) }
        }
    }

    fun named(match: PathMatch, name: String): PathMatch? {
        val mapping = match.node as? YamlNode.Mapping ?: return null
        var scanned = 0L
        val entry = mapping.entries.firstOrNull { candidate ->
            scanned++
            Members.tokenOf(candidate.key) == name
        }
        budget.charge(scanned + 1L)
        return entry?.let { found -> match.child(name, found.value) }
    }

    fun indexed(match: PathMatch, index: Long): PathMatch? {
        val sequence = match.node as? YamlNode.Sequence ?: return null
        budget.charge()
        val size = sequence.items.size.toLong()
        val normalized = (index + size).takeIf { index < 0 } ?: index
        val item = normalized.takeIf { it in 0 until size }?.toInt()
        return item?.let { position -> match.child(position.toString(), sequence.items[position]) }
    }

    fun sliced(match: PathMatch, slice: PathSelector.Slice, visit: (PathMatch) -> Unit) {
        val sequence = match.node as? YamlNode.Sequence ?: return
        budget.charge()
        SliceIndices.each(slice, sequence.items.size) { index ->
            budget.charge()
            visit(match.child(index.toString(), sequence.items[index]))
        }
    }
}
