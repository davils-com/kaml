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
package com.davils.kaml.core.compose

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.NodeKeySet
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kore.core.text.TextRange

internal class MergeKeys(
    private val budget: ComposeBudget,
    private val report: (YamlDiagnosticCode, TextRange?, String) -> Unit,
) {
    fun merged(entries: List<YamlEntry>, range: TextRange?): List<YamlEntry> {
        if (entries.none { entry -> isMergeKey(entry.key) }) return entries
        val merge = Merge(entries, range)
        val pending = entries.iterator()
        while (pending.hasNext() && !budget.isExhausted) {
            merge.expand(pending.next())
        }
        return merge.result
    }

    private fun sourcesOf(value: YamlNode): List<YamlNode.Mapping> {
        val mappings = when (value) {
            is YamlNode.Sequence -> value.items
            is YamlNode.Scalar, is YamlNode.Mapping -> listOf(value)
        }
        val invalid = mappings.filterNot { it is YamlNode.Mapping }
        invalid.forEach { node ->
            report(
                YamlDiagnosticCode.InvalidMerge,
                node.range,
                "A merge key needs a mapping or a sequence of mappings",
            )
        }
        return mappings.filterIsInstance<YamlNode.Mapping>()
    }

    private inner class Merge(entries: List<YamlEntry>, private val range: TextRange?) {
        private val plain = entries.filterNot { entry -> isMergeKey(entry.key) }
        private val seen = NodeKeySet().also { keys -> plain.forEach { entry -> keys.add(entry.key) } }
        private var admitted = 0

        val result: MutableList<YamlEntry> = ArrayList(entries.size)

        fun expand(entry: YamlEntry) {
            if (!isMergeKey(entry.key)) {
                result.add(entry)
                return
            }
            val sources = sourcesOf(entry.value)
            val candidates = sources.flatMap(YamlNode.Mapping::entries).iterator()
            while (candidates.hasNext() && !budget.isExhausted) {
                admit(candidates.next())
            }
        }

        private fun admit(candidate: YamlEntry) {
            if (!seen.add(candidate.key)) return
            val most = budget.remainingExpansion
            budget.expand(NodeSizes.of(candidate.key, most) + NodeSizes.of(candidate.value, most), range)
            budget.collection(plain.size + admitted + 1, range)
            if (budget.isExhausted) return
            result.add(candidate)
            admitted++
        }
    }

    companion object {
        fun isMergeKey(key: YamlNode): Boolean = key is YamlNode.Scalar && key.tag == YamlTag.MERGE
    }
}
