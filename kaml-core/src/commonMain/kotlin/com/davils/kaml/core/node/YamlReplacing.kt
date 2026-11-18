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
package com.davils.kaml.core.node

/**
 * Returns a mapping with this mapping's tag, style, anchor and range, but holding [entries] instead of its entries.
 *
 * @param entries the entries of the new mapping, in order; the mapping keeps a copy.
 * @return the new mapping; this mapping is unchanged.
 * @throws IllegalArgumentException if two of [entries] have equal keys, or if the mapping would nest deeper than
 * [com.davils.kaml.core.limit.YamlLimits.HARD_DEPTH]; the message names the index of the repeated entry and nothing of
 * its content.
 * @since 1.0.0
 */
public fun YamlNode.Mapping.withEntries(entries: List<YamlEntry>): YamlNode.Mapping {
    UniqueKeys.requireUnique(entries)
    return YamlNode.Mapping(
        entries = entries,
        tag = tag,
        style = style,
        anchor = anchor,
        range = range,
    )
}

/**
 * Returns a sequence with this sequence's tag, style, anchor and range, but holding [items] instead of its items.
 *
 * @param items the items of the new sequence, in order; the sequence keeps a copy.
 * @return the new sequence; this sequence is unchanged.
 * @throws IllegalArgumentException if the sequence would nest deeper than
 * [com.davils.kaml.core.limit.YamlLimits.HARD_DEPTH].
 * @since 1.0.0
 */
public fun YamlNode.Sequence.withItems(items: List<YamlNode>): YamlNode.Sequence = YamlNode.Sequence(
    items = items,
    tag = tag,
    style = style,
    anchor = anchor,
    range = range,
)
