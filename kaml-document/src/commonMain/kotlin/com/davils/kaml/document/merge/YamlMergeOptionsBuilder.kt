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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures [YamlMergeOptions].
 *
 * A builder is not safe to share between threads; the options it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlMergeOptionsBuilder internal constructor() : ValidatingBuilder<YamlMergeOptions>() {
    override val subject: String get() = "yaml merge"

    /**
     * How a later layer marks a value as deleted; [YamlDeletion.Tag] by default.
     *
     * @since 1.0.0
     */
    public var deletion: YamlDeletion = YamlDeletion.Tag

    /**
     * How sequences merge where no pointer of [sequences] applies; [YamlSequenceMerge.Replace] by default.
     *
     * @since 1.0.0
     */
    public var sequences: YamlSequenceMerge = YamlSequenceMerge.Replace

    internal val sequencesAt: MutableMap<String, YamlSequenceMerge> = linkedMapOf()

    /**
     * Sets how the sequences at [pointer] merge.
     *
     * @param pointer a JSON pointer in which the token `*` stands for every item of a sequence above it; an invalid
     * pointer fails the build.
     * @param merge how the sequences there merge; a later call for the same pointer wins.
     * @since 1.0.0
     */
    public fun sequences(pointer: String, merge: YamlSequenceMerge) {
        checkOpen()
        sequencesAt[pointer] = merge
    }

    override fun assemble(): YamlMergeOptions = YamlMergeOptions(this)
}
