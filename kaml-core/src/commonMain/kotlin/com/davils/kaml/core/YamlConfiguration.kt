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
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.validate as report

/**
 * The validated settings of a [Yaml], built with [yaml].
 *
 * A configuration is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class YamlConfiguration internal constructor(builder: YamlBuilder) : Validatable {
    /**
     * The schema that resolves plain scalars, such as `1` to an integer, and checks the tags of nodes.
     *
     * @since 1.0.0
     */
    public val schema: YamlSchema = builder.schema

    /**
     * Whether a document that declares `%YAML 1.1` is read with [YamlSchema.Yaml11] instead of [schema].
     *
     * @since 1.0.0
     */
    public val isSchemaSelectedByVersion: Boolean = builder.isSchemaSelectedByVersion

    /**
     * What the composer does with a key that appears more than once in one mapping.
     *
     * @since 1.0.0
     */
    public val duplicateKeys: YamlDuplicateKeys = builder.duplicateKeys

    /**
     * What the composer does with a tag the schema does not define.
     *
     * @since 1.0.0
     */
    public val unknownTags: YamlUnknownTags = builder.unknownTags

    /**
     * The limits every parse and every emit of the [Yaml] checks.
     *
     * @since 1.0.0
     */
    public val limits: YamlLimits = builder.limits

    /**
     * The options of the emitter.
     *
     * @since 1.0.0
     */
    public val emitter: YamlEmitOptions = builder.emitter

    /**
     * Checks the schema, the limits and the options of the emitter.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        within(::schema) { include(schema.report("schema").violations) }
        within(::limits) { include(limits.report("limits").violations) }
        within(::emitter) { include(emitter.report("emitter").violations) }
    }

    /**
     * Returns every setting, each in the bounded, escaped form of its own `toString`.
     *
     * @return the configuration as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "YamlConfiguration(schema=$schema, isSchemaSelectedByVersion=$isSchemaSelectedByVersion, " +
            "duplicateKeys=$duplicateKeys, unknownTags=$unknownTags, limits=$limits, emitter=$emitter)"
}
