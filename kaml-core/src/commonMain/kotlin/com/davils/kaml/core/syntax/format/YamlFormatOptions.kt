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

import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.validateOrThrow
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * How a [YamlFormatter] lays out documents; built and validated with [yamlFormatOptions].
 *
 * Options are immutable and safe to share between threads. They are validated when they are created, also by a Java
 * caller that reaches the constructor, so no instance holds an indent or a number of blank lines out of range.
 *
 * @since 1.0.0
 */
public class YamlFormatOptions internal constructor(builder: YamlFormatOptionsBuilder) : Validatable {
    /**
     * The number of spaces each level of block collections is indented by, from 1 to 9.
     *
     * @since 1.0.0
     */
    public val indent: Int = builder.indent

    /**
     * Whether a block sequence that is the value of a mapping entry is indented below its key, rather than written
     * at the column of the key.
     *
     * @since 1.0.0
     */
    public val isSequenceIndented: Boolean = builder.isSequenceIndented

    /**
     * The most blank lines kept in a row outside of block scalars; zero or more.
     *
     * @since 1.0.0
     */
    public val maximumEmptyLines: Int = builder.maximumEmptyLines

    /**
     * Whether a text that does not end with a line break gets one.
     *
     * @since 1.0.0
     */
    public val hasFinalLineBreak: Boolean = builder.hasFinalLineBreak

    private val values: List<Any>
        get() = listOf(indent, isSequenceIndented, maximumEmptyLines, hasFinalLineBreak)

    init {
        validateOrThrow(SUBJECT)
    }

    override fun ValidationScope.validate() {
        if (indent !in 1..MAXIMUM_INDENT) {
            fail(
                ::indent,
                "must be between 1 and $MAXIMUM_INDENT, but was $indent",
                INVALID,
            )
        }
        if (maximumEmptyLines < 0) {
            fail(
                ::maximumEmptyLines,
                "must not be negative, but was $maximumEmptyLines",
                INVALID,
            )
        }
    }

    override fun equals(other: Any?): Boolean = other is YamlFormatOptions && values == other.values

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = "YamlFormatOptions(indent=$indent, isSequenceIndented=$isSequenceIndented, " +
        "maximumEmptyLines=$maximumEmptyLines, hasFinalLineBreak=$hasFinalLineBreak)"

    /**
     * Holds the default options.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val MAXIMUM_INDENT: Int = 9
        private const val SUBJECT: String = "yaml formatter"
        private val INVALID: ViolationCode = ViolationCode("kaml.format.invalid-option")

        /**
         * The default options: an indent of two spaces, indented sequences, at most one blank line in a row and a
         * final line break.
         *
         * @since 1.0.0
         */
        public val Default: YamlFormatOptions = YamlFormatOptionsBuilder().build()
    }
}
