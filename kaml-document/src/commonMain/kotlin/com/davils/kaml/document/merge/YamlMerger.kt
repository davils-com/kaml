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

package com.davils.kaml.document.merge

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.merge.trace.LayerMerging

/**
 * Merges layers of configuration, such as defaults, the settings of a site and those of a host, into one graph and
 * records which layer every value comes from.
 *
 * Later layers win: a mapping merges into a mapping member by member, a sequence combines with a sequence as the
 * [options] say, and everything else replaces what came before. A member is matched by the text of its key, the
 * first occurrence when a key repeats. A value marked as deleted, as [YamlMergeOptions.deletion] defines, removes
 * what it replaces.
 *
 * A merger is immutable and safe to share between threads.
 *
 * @property options how the layers are merged.
 * @property limits the limits of every merge: the nodes of every layer after the first count against
 * [YamlLimits.maximumNodes], and no layer may be deeper than [YamlLimits.maximumDepth].
 * @since 1.0.0
 */
public class YamlMerger(
    public val options: YamlMergeOptions = YamlMergeOptions.Default,
    public val limits: YamlLimits = YamlLimits.Untrusted,
) {
    /**
     * Merges [layers] in order, each over the ones before it.
     *
     * @param layers the layers, the first one at the bottom.
     * @return the merged graph with the origin of every value.
     * @throws IllegalArgumentException if [layers] is empty.
     * @throws YamlDocumentLimitException if the layers hold more nodes than [YamlLimits.maximumNodes] or are deeper
     * than [YamlLimits.maximumDepth]; nothing is merged then.
     * @since 1.0.0
     */
    public fun merge(layers: List<YamlLayer>): YamlMerged {
        require(layers.isNotEmpty()) { "A merge needs at least one layer" }
        return LayerMerging(options, limits).merged(layers)
    }

    /**
     * Returns the options and nothing else.
     *
     * @return a description such as `YamlMerger(YamlMergeOptions(...))`.
     * @since 1.0.0
     */
    override fun toString(): String = "YamlMerger($options)"
}
