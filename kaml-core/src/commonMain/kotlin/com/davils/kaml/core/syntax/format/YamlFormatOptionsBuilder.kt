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

package com.davils.kaml.core.syntax.format

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures [YamlFormatOptions] inside [yamlFormatOptions].
 *
 * A builder is not thread-safe and is used for one value: once the options are built, every change throws.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlFormatOptionsBuilder internal constructor() : ValidatingBuilder<YamlFormatOptions>() {
    override val subject: String get() = "yaml formatter"

    /**
     * The number of spaces each level of block collections is indented by, from 1 to 9; 2 by default.
     *
     * @throws IllegalStateException if it is set after the options were built.
     * @since 1.0.0
     */
    public var indent: Int = DEFAULT_INDENT
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a block sequence that is the value of a mapping entry is indented below its key; `true` by default.
     *
     * @throws IllegalStateException if it is set after the options were built.
     * @since 1.0.0
     */
    public var isSequenceIndented: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The most blank lines kept in a row outside of block scalars, zero or more; 1 by default.
     *
     * @throws IllegalStateException if it is set after the options were built.
     * @since 1.0.0
     */
    public var maximumEmptyLines: Int = 1
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a text that does not end with a line break gets one; `true` by default.
     *
     * @throws IllegalStateException if it is set after the options were built.
     * @since 1.0.0
     */
    public var hasFinalLineBreak: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): YamlFormatOptions = YamlFormatOptions(this)

    private companion object {
        const val DEFAULT_INDENT: Int = 2
    }
}
