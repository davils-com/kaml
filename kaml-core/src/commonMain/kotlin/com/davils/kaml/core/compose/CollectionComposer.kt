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

@file:OptIn(DavilsInternal::class)

package com.davils.kaml.core.compose

import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.model.YamlNodeKind
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange

internal class CollectionComposer(
    private val settings: ComposeSettings,
    schema: YamlSchema,
    private val budget: ComposeBudget,
    private val report: (YamlDiagnosticCode, TextRange?, String) -> Unit,
) {
    private val tags = TagCheck(settings, schema, report)

    fun sequence(frame: ComposeFrame.Sequence): YamlNode.Sequence {
        val start = frame.start
        val tag = resolved(tag = start.tag, fallback = YamlTag.SEQUENCE)
        tags.check(tag, YamlNodeKind.Sequence, start.range)
        return YamlNode.Sequence(
            items = frame.items.toList(),
            tag = tag,
            style = start.style,
            anchor = start.anchor,
            range = start.range,
        )
    }

    fun mapping(frame: ComposeFrame.Mapping): YamlNode.Mapping {
        val start = frame.start
        val tag = resolved(tag = start.tag, fallback = YamlTag.MAPPING)
        tags.check(tag, YamlNodeKind.Mapping, start.range)
        val unique = deduplicated(frame.entries)
        return YamlNode.Mapping(
            entries = MergeKeys(budget, report).merged(unique, start.range),
            tag = tag,
            style = start.style,
            anchor = start.anchor,
            range = start.range,
        )
    }

    private fun resolved(tag: YamlTag?, fallback: YamlTag): YamlTag = tag?.takeIf { !it.isNonSpecific } ?: fallback

    private fun deduplicated(entries: List<YamlEntry>): List<YamlEntry> {
        val chosen = HashMap<YamlNode, Int>()
        entries.forEachIndexed { index, entry -> choose(chosen, index, entry) }
        return entries.filterIndexed { index, entry ->
            MergeKeys.isMergeKey(
                entry.key,
            ) ||
                chosen.getValue(entry.key) == index
        }
    }

    private fun choose(chosen: MutableMap<YamlNode, Int>, index: Int, entry: YamlEntry) {
        if (MergeKeys.isMergeKey(entry.key)) return
        val isRepeated = entry.key in chosen
        if (isRepeated) reportDuplicate(entry)
        if (!isRepeated || settings.duplicateKeys == YamlDuplicateKeys.KeepLast) chosen[entry.key] = index
    }

    private fun reportDuplicate(entry: YamlEntry) {
        if (settings.duplicateKeys != YamlDuplicateKeys.Refuse) return
        report(
            YamlDiagnosticCode.DuplicateKey,
            entry.key.range,
            messageFor(entry.key),
        )
    }

    private fun messageFor(key: YamlNode): String {
        val scalar = key as? YamlNode.Scalar ?: return "A ${key.kind} key appears more than once"
        return "The key ${SafeText.quoted(scalar.value)} appears more than once"
    }
}
