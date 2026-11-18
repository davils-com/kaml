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

package com.davils.kaml.serialization.decode

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.withEntries
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.serialization.YamlPolymorphism
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

@OptIn(DavilsInternal::class)
internal class PolymorphicReading(private val context: DecodeContext) {
    fun typed(decoder: NodeDecoder): TypedNode = when (context.configuration.polymorphism) {
        YamlPolymorphism.Property -> byProperty(decoder)
        YamlPolymorphism.Tag -> byTag(decoder)
        YamlPolymorphism.Wrapped -> byWrapper(decoder)
    }

    private fun byProperty(decoder: NodeDecoder): TypedNode {
        val mapping = decoder.node as? YamlNode.Mapping ?: context.mismatch(decoder.location, "a mapping with a type")
        val discriminator = context.configuration.classDiscriminator
        val entry = mapping.entries.firstOrNull { candidate ->
            (candidate.key as? YamlNode.Scalar)?.value ==
                discriminator
        }
        val name = entry?.value as? YamlNode.Scalar ?: missing(
            decoder,
            "the property ${SafeText.quoted(discriminator)}",
        )
        val rest = mapping.entries.filterNot { candidate -> candidate === entry }
        return TypedNode(name = name, content = mapping.withEntries(rest), nesting = 0, isNameCharged = true)
    }

    private fun byTag(decoder: NodeDecoder): TypedNode {
        val tag = decoder.node.tag.takeIf { tag -> tag.isLocal } ?: missing(decoder, "a local tag such as !name")
        return TypedNode(name = yamlScalar(tag.uri.drop(1)), content = decoder.node, nesting = 0, isNameCharged = false)
    }

    private fun byWrapper(decoder: NodeDecoder): TypedNode {
        val mapping = decoder.node as? YamlNode.Mapping
        val entry = mapping?.run { entries.singleOrNull() } ?: missing(decoder, "a mapping with one type name")
        val name = entry.key as? YamlNode.Scalar ?: missing(decoder, "a type name")
        return TypedNode(name = name, content = entry.value, nesting = 1, isNameCharged = true)
    }

    private fun missing(decoder: NodeDecoder, what: String): Nothing = context.fail(
        YamlDiagnosticCode.MissingPolymorphicType,
        decoder.location,
        "Expected $what to choose the type",
    )
}
