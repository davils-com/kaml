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

/**
 * How a [YamlFormat] writes and reads the type of a polymorphic value, such as a subclass of a sealed interface.
 *
 * Whatever the style, the type name read from a document only selects a serializer that the application registered: a
 * subclass of a sealed class or a subclass registered for the base class in the
 * [kotlinx.serialization.modules.SerializersModule] of the format. kaml never looks up or instantiates a class by its
 * name, so a document cannot reach a type the application did not offer. An unknown type name is reported as
 * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.UnknownPolymorphicType] and a missing one as
 * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.MissingPolymorphicType].
 *
 * @since 1.0.0
 */
public enum class YamlPolymorphism {
    /**
     * The type name is a property of the mapping, named by [YamlFormatConfiguration.classDiscriminator], such as
     * `type: router`. A subtype that encodes as a scalar or a sequence is written as it is, without its type name, and
     * cannot be read back; use [Wrapped] for such subtypes. A subtype with a property of the same name as the
     * discriminator is refused when it is encoded.
     *
     * @since 1.0.0
     */
    Property,

    /**
     * The type name is the local tag of the mapping, such as `!router`. A subtype that encodes as a scalar or a
     * sequence is written as it is, without its type name, and cannot be read back; use [Wrapped] for such subtypes.
     *
     * @since 1.0.0
     */
    Tag,

    /**
     * The value is wrapped in a mapping with a single entry whose key is the type name, such as `router: {ports: 8}`.
     * Works for every subtype, whether it encodes as a mapping, a sequence or a scalar.
     *
     * @since 1.0.0
     */
    Wrapped,
}
