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

package com.davils.kaml.core

import com.davils.kaml.core.emit.YamlEmitOptions
import com.davils.kaml.core.emit.YamlEmitOptionsBuilder
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.YamlLimitsBuilder
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * The builder of [YamlConfiguration] behind [yaml].
 *
 * Every property starts from its default and means what the property of the same name of [YamlConfiguration]
 * means. Building validates the configuration and every nested block and throws a
 * [com.davils.kore.core.validation.report.ValidationException] whose report names every invalid setting.
 *
 * A builder is not thread-safe and belongs to the block it is given to; once the block has been built, setting a
 * property or declaring a nested block throws an [IllegalStateException].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlBuilder internal constructor() : ValidatingBuilder<YamlConfiguration>() {
    override val subject: String get() = "yaml"

    /**
     * The schema that resolves plain scalars and checks tags; [YamlSchema.Core] by default.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var schema: YamlSchema = YamlSchema.Core
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a document that declares `%YAML 1.1` is read with [YamlSchema.Yaml11] instead of [schema]; `true` by
     * default.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var isSchemaSelectedByVersion: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * What the composer does with a repeated key; [YamlDuplicateKeys.Refuse] by default.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var duplicateKeys: YamlDuplicateKeys = YamlDuplicateKeys.Refuse
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * What the composer does with a tag the schema does not define; [YamlUnknownTags.Keep] by default.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var unknownTags: YamlUnknownTags = YamlUnknownTags.Keep
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The limits of reading and writing; [YamlLimits.Untrusted] by default. A [limits] block starts from this value.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var limits: YamlLimits = YamlLimits.Untrusted
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The options of the emitter; [YamlEmitOptions.Default] by default.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var emitter: YamlEmitOptions = YamlEmitOptions.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Changes the limits that [limits] holds so far, such as `limits { maximumDepth = 64 }`.
     *
     * An invalid limit is reported under `limits` in the report of the build, and [limits] keeps its earlier value.
     *
     * @param configure the block that sets the limits to change.
     * @throws IllegalStateException if it is called after the block was built.
     * @since 1.0.0
     */
    public fun limits(configure: YamlLimitsBuilder.() -> Unit) {
        val builder = YamlLimitsBuilder()
        builder.from(limits)
        nested(builder, ::limits, configure)?.let { built -> limits = built }
    }

    /**
     * Builds the options of the emitter from their defaults, such as `emitter { indent = 4 }`.
     *
     * An invalid option is reported under `emitter` in the report of the build, and [emitter] keeps its earlier value.
     *
     * @param configure the block that sets the options.
     * @throws IllegalStateException if it is called after the block was built.
     * @since 1.0.0
     */
    public fun emitter(configure: YamlEmitOptionsBuilder.() -> Unit) {
        nested(YamlEmitOptionsBuilder(), ::emitter, configure)?.let { built -> emitter = built }
    }

    override fun assemble(): YamlConfiguration = YamlConfiguration(this)
}
