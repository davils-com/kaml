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
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlinx.serialization.modules.SerializersModule

/**
 * The validated, immutable options of a [YamlFormat], built by [YamlFormatBuilder]. Each property is documented on the
 * builder.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class YamlFormatConfiguration internal constructor(builder: YamlFormatBuilder) : Validatable {
    /**
     * The reader and writer of YAML text, see [YamlFormatBuilder.yaml].
     *
     * @since 1.0.0
     */
    public val yaml: Yaml = builder.yaml

    /**
     * Whether unknown keys are ignored, see [YamlFormatBuilder.isUnknownKeyIgnored].
     *
     * @since 1.0.0
     */
    public val isUnknownKeyIgnored: Boolean = builder.isUnknownKeyIgnored

    /**
     * Whether default values are written, see [YamlFormatBuilder.isDefaultEncoded].
     *
     * @since 1.0.0
     */
    public val isDefaultEncoded: Boolean = builder.isDefaultEncoded

    /**
     * Whether `null` values are written, see [YamlFormatBuilder.hasExplicitNulls].
     *
     * @since 1.0.0
     */
    public val hasExplicitNulls: Boolean = builder.hasExplicitNulls

    /**
     * Whether invalid values fall back to defaults, see [YamlFormatBuilder.isInputCoerced].
     *
     * @since 1.0.0
     */
    public val isInputCoerced: Boolean = builder.isInputCoerced

    /**
     * How property names derive from serial names, see [YamlFormatBuilder.namingStrategy].
     *
     * @since 1.0.0
     */
    public val namingStrategy: YamlNamingStrategy = builder.namingStrategy

    /**
     * How polymorphic types are written and read, see [YamlFormatBuilder.polymorphism].
     *
     * @since 1.0.0
     */
    public val polymorphism: YamlPolymorphism = builder.polymorphism

    /**
     * The property that holds the type name, see [YamlFormatBuilder.classDiscriminator].
     *
     * @since 1.0.0
     */
    public val classDiscriminator: String = builder.classDiscriminator

    /**
     * The registry of contextual and polymorphic serializers, see [YamlFormatBuilder.serializersModule].
     *
     * @since 1.0.0
     */
    public val serializersModule: SerializersModule = builder.serializersModule

    override fun ValidationScope.validate() {
        if (classDiscriminator.isBlank()) {
            fail(::classDiscriminator, "must not be blank", YamlFormatViolationCodes.BLANK_DISCRIMINATOR)
        }
    }

    override fun toString(): String =
        "YamlFormatConfiguration(isUnknownKeyIgnored=$isUnknownKeyIgnored, isDefaultEncoded=$isDefaultEncoded, " +
            "hasExplicitNulls=$hasExplicitNulls, isInputCoerced=$isInputCoerced, polymorphism=$polymorphism, " +
            "classDiscriminator=${SafeText.quoted(classDiscriminator)})"
}
