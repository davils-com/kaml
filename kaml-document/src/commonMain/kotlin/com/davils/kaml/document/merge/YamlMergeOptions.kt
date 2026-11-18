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

import com.davils.kaml.document.YamlDocumentViolationCodes
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * How a [YamlMerger] merges layers. Build them with [yamlMergeOptions].
 *
 * The options are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class YamlMergeOptions internal constructor(builder: YamlMergeOptionsBuilder) : Validatable {
    /**
     * How a later layer marks a value as deleted.
     *
     * @since 1.0.0
     */
    public val deletion: YamlDeletion = builder.deletion

    /**
     * How sequences merge where no pointer of [sequencesAt] applies.
     *
     * @since 1.0.0
     */
    public val sequences: YamlSequenceMerge = builder.sequences

    /**
     * How the sequences at each pointer merge, where the token `*` stands for every item of a sequence above; the
     * map cannot be changed.
     *
     * @since 1.0.0
     */
    public val sequencesAt: Map<String, YamlSequenceMerge> = buildMap { putAll(builder.sequencesAt) }

    private val strategies: Map<JsonPointer, YamlSequenceMerge> = parsedStrategies()

    internal fun sequenceMergeAt(pattern: JsonPointer): YamlSequenceMerge = strategies[pattern] ?: sequences

    override fun ValidationScope.validate() {
        each(::sequencesAt, sequencesAt) { merge ->
            if (merge is YamlSequenceMerge.ByKey && merge.key.isBlank()) {
                fail("must name the key that identifies an item", YamlDocumentViolationCodes.INVALID_KEY)
            }
        }
        val invalid = sequencesAt.keys.filter { JsonPointer.parseOrNull(it) == null }
        invalid.forEach { _ ->
            fail(::sequencesAt, "names a key that is not a JSON pointer", YamlDocumentViolationCodes.INVALID_POINTER)
        }
        val defaultKey = (sequences as? YamlSequenceMerge.ByKey)?.key
        if (defaultKey?.isBlank() == true) {
            fail(::sequences, "must name the key that identifies an item", YamlDocumentViolationCodes.INVALID_KEY)
        }
    }

    private fun parsedStrategies(): Map<JsonPointer, YamlSequenceMerge> {
        val parsed = sequencesAt.mapNotNull { (pointer, merge) ->
            JsonPointer.parseOrNull(pointer)?.let { parsedPointer -> parsedPointer to merge }
        }
        return parsed.toMap()
    }

    /**
     * Returns the options, with every pointer escaped.
     *
     * @return a description such as `YamlMergeOptions(deletion=Tag, sequences=Replace, sequencesAt={})`.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String {
        val shown = sequencesAt.entries.joinToString(
            separator = ", ",
            prefix = "{",
            postfix = "}",
        ) { (pointer, merge) ->
            "${SafeText.quoted(pointer)}=$merge"
        }
        return "YamlMergeOptions(deletion=$deletion, sequences=$sequences, sequencesAt=$shown)"
    }

    /**
     * Holds the default options.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default options: deletion by the tag `!delete` and sequences that replace each other.
         *
         * @since 1.0.0
         */
        public val Default: YamlMergeOptions = YamlMergeOptionsBuilder().build()
    }
}
