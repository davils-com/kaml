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

package com.davils.kaml.serialization

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlBuilder
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import com.davils.kaml.core.yaml as buildYaml

/**
 * Collects the options of a [YamlFormat], see [yamlFormat].
 *
 * The builder validates the options when it builds and is meant for a single use on one thread: once the format is
 * built, every change throws, so no option can be changed after it was validated.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlFormatBuilder internal constructor() : ValidatingBuilder<YamlFormatConfiguration>() {
    override val subject: String get() = "yaml format"

    /**
     * The reader and writer of YAML text, whose schema, limits and emitter options the format uses.
     * [Yaml.Default] by default, whose limits are meant for untrusted input.
     *
     * @throws IllegalStateException if it is set after the format was built.
     * @since 1.0.0
     */
    public var yaml: Yaml = Yaml.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a key that names no property of a class is ignored instead of reported as
     * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.UnknownProperty]. `false` by default.
     *
     * @throws IllegalStateException if it is set after the format was built.
     * @since 1.0.0
     */
    public var isUnknownKeyIgnored: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a property that has its default value is written. `true` by default.
     *
     * @throws IllegalStateException if it is set after the format was built.
     * @since 1.0.0
     */
    public var isDefaultEncoded: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a property whose value is `null` is written as `null`. When `false`, such a property is left out and a
     * missing nullable property without a default is read as `null`. `true` by default.
     *
     * @throws IllegalStateException if it is set after the format was built.
     * @since 1.0.0
     */
    public var hasExplicitNulls: Boolean = true
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether a `null` for a property that cannot be `null`, or an unknown enum value, is replaced by the default
     * value of the property, if it has one, instead of reported. `false` by default.
     *
     * @throws IllegalStateException if it is set after the format was built.
     * @since 1.0.0
     */
    public var isInputCoerced: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * How the YAML names of properties derive from their serial names. [YamlNamingStrategy.Identity] by default.
     *
     * @throws IllegalStateException if it is set after the format was built.
     * @since 1.0.0
     */
    public var namingStrategy: YamlNamingStrategy = YamlNamingStrategy.Identity
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * How the type of a polymorphic value is written and read. [YamlPolymorphism.Property] by default.
     *
     * @throws IllegalStateException if it is set after the format was built.
     * @since 1.0.0
     */
    public var polymorphism: YamlPolymorphism = YamlPolymorphism.Property
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The name of the property that holds the type name with [YamlPolymorphism.Property]. [DEFAULT_DISCRIMINATOR] by
     * default; it must not be blank.
     *
     * @throws IllegalStateException if it is set after the format was built.
     * @since 1.0.0
     */
    public var classDiscriminator: String = DEFAULT_DISCRIMINATOR
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The serializers module that resolves contextual and polymorphic serializers. It is the closed registry of the
     * types a document can name. Empty by default.
     *
     * @throws IllegalStateException if it is set after the format was built.
     * @since 1.0.0
     */
    public var serializersModule: SerializersModule = EmptySerializersModule()
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Sets [yaml] to a reader and writer built from [configure].
     *
     * @param configure sets the options of the YAML configuration, such as its limits.
     * @throws com.davils.kore.core.validation.report.ValidationException if the YAML configuration is invalid.
     * @throws IllegalStateException if it is called after the format was built.
     * @since 1.0.0
     */
    public fun yaml(configure: YamlBuilder.() -> Unit) {
        yaml = buildYaml(configure)
    }

    override fun assemble(): YamlFormatConfiguration = YamlFormatConfiguration(this)

    /**
     * The defaults of the builder.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default name of the property that holds the type name of a polymorphic value.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DISCRIMINATOR: String = "type"
    }
}
