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

package com.davils.kaml.core.emit

import com.davils.kaml.core.model.YamlVersion
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Builds [YamlEmitOptions] in the `emitter { }` block of the [com.davils.kaml.core.yaml] DSL.
 *
 * Every property starts at its default, which [YamlEmitOptions.Default] holds. The options are validated when the
 * block ends: an [indent] outside [YamlEmitOptions.MINIMUM_INDENT]..[YamlEmitOptions.MAXIMUM_INDENT] is reported as a
 * violation, and the surrounding builder then throws the `ValidationException` of Kore. Once the block has been
 * evaluated, the builder is closed, and setting a property throws an [IllegalStateException] instead of being lost.
 *
 * A builder is not thread-safe; it is meant to be configured by one thread, for one value.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlEmitOptionsBuilder internal constructor() : ValidatingBuilder<YamlEmitOptions>() {
    override val subject: String get() = "yaml emitter"

    /**
     * The number of spaces per level of a block collection, see [YamlEmitOptions.indent]; 2 by default.
     *
     * @throws IllegalStateException if it is set after the builder was evaluated.
     * @since 1.0.0
     */
    public var indent: Int = DEFAULT_INDENT
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a block sequence below a key is indented, see [YamlEmitOptions.isSequenceIndented]; `true` by default.
     *
     * @throws IllegalStateException if it is set after the builder was evaluated.
     * @since 1.0.0
     */
    public var isSequenceIndented: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The line break of the output, see [YamlEmitOptions.lineBreak]; [YamlLineBreak.Lf] by default.
     *
     * @throws IllegalStateException if it is set after the builder was evaluated.
     * @since 1.0.0
     */
    public var lineBreak: YamlLineBreak = YamlLineBreak.Lf
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether mapping entries are sorted by key, see [YamlEmitOptions.hasSortedKeys]; `false` by default.
     *
     * @throws IllegalStateException if it is set after the builder was evaluated.
     * @since 1.0.0
     */
    public var hasSortedKeys: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether every document starts with `---`, see [YamlEmitOptions.isDocumentStartExplicit]; `false` by default.
     *
     * @throws IllegalStateException if it is set after the builder was evaluated.
     * @since 1.0.0
     */
    public var isDocumentStartExplicit: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The preferred quotes, see [YamlEmitOptions.quoteStyle]; [YamlQuoteStyle.Double] by default.
     *
     * @throws IllegalStateException if it is set after the builder was evaluated.
     * @since 1.0.0
     */
    public var quoteStyle: YamlQuoteStyle = YamlQuoteStyle.Double
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The oldest YAML version that must read the output correctly, see [YamlEmitOptions.compatibility];
     * [YamlVersion.V1_2] by default.
     *
     * @throws IllegalStateException if it is set after the builder was evaluated.
     * @since 1.0.0
     */
    public var compatibility: YamlVersion = YamlVersion.V1_2
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether the styles of the nodes are kept where they are safe, see [YamlEmitOptions.isStylePreserved]; `true` by
     * default.
     *
     * @throws IllegalStateException if it is set after the builder was evaluated.
     * @since 1.0.0
     */
    public var isStylePreserved: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): YamlEmitOptions = YamlEmitOptions(this)

    private companion object {
        const val DEFAULT_INDENT: Int = 2
    }
}
