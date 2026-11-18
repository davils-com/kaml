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

package com.davils.kaml.serialization.encode

import com.davils.kaml.core.emit.YamlCommentsBuilder
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.string
import com.davils.kaml.core.node.withEntries
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlTagged
import com.davils.kaml.serialization.PropertyNames
import com.davils.kaml.serialization.ScalarText
import com.davils.kaml.serialization.WalkBudget
import com.davils.kaml.serialization.YamlFormatConfiguration
import com.davils.kaml.serialization.YamlPolymorphism
import com.davils.kaml.serialization.annotation.YamlComment
import com.davils.kaml.serialization.annotation.YamlFlow
import com.davils.kaml.serialization.annotation.YamlStyle
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlin.io.encoding.Base64

@OptIn(ExperimentalSerializationApi::class, DavilsInternal::class)
internal class EncodeContext(val configuration: YamlFormatConfiguration, private val comments: YamlCommentsBuilder?) {
    private val budget = WalkBudget.writing(configuration.yaml.configuration.limits)
    private val names = HashMap<SerialDescriptor, PropertyNames>()
    private val binary = ByteArraySerializer().descriptor

    fun <T> encode(
        serializer: SerializationStrategy<T>,
        value: T,
        options: ElementOptions,
        depth: Int,
        sink: (YamlNode) -> Unit,
    ) {
        val encoder = ValueEncoder(context = this, fixedOptions = options, depth = depth, sink = sink)
        encoder.encodeSerializableValue(serializer, value)
    }

    fun <T> binaryOrNull(serializer: SerializationStrategy<T>, value: T): YamlNode? {
        val bytes = (value as? ByteArray)?.takeIf { serializer.descriptor == binary } ?: return null
        return yamlTagged(Base64.encode(bytes), YamlTag.BINARY)
    }

    fun structure(
        descriptor: SerialDescriptor,
        options: ElementOptions,
        depth: Int,
        sink: (YamlNode) -> Unit,
    ): CompositeEncoder {
        val isPolymorphic = descriptor.kind is PolymorphicKind
        val isWrapped = configuration.polymorphism == YamlPolymorphism.Wrapped
        val nested = depth + (1.takeIf { !isPolymorphic || isWrapped } ?: 0)
        enter(nested)
        if (isPolymorphic) return PolymorphicEncoder(context = this, options = options, depth = nested, parent = sink)
        return StructureEncoder(
            context = this,
            collector = collector(descriptor, options),
            depth = nested,
            parent = sink,
        )
    }

    fun options(descriptor: SerialDescriptor, index: Int): ElementOptions {
        val annotations = descriptor.getElementAnnotations(index)
        return ElementOptions(
            comments = annotations.filterIsInstance<YamlComment>().flatMap { comment -> comment.lines.asList() },
            style = annotations.filterIsInstance<YamlStyle>().firstOrNull()?.scalar,
            isFlow = annotations.any { annotation -> annotation is YamlFlow },
        )
    }

    fun namesOf(descriptor: SerialDescriptor): PropertyNames =
        names.getOrPut(descriptor) { PropertyNames.of(descriptor, configuration.namingStrategy) }

    fun text(value: String, options: ElementOptions): YamlNode.Scalar {
        requireRepresentable(value)
        return yamlScalar(value, options.scalarStyle())
    }

    fun key(name: String): YamlNode = created(text(name, ElementOptions.NONE))

    fun comment(key: YamlNode, lines: List<String>) {
        comments?.let { builder -> lines.forEach { line -> builder.comment(key, line) } }
    }

    fun created(node: YamlNode): YamlNode {
        if (!budget.admits()) throw SerializationException("The value encodes to more than ${budget.allowance} nodes")
        return node
    }

    fun typed(name: String, content: YamlNode): YamlNode = when (configuration.polymorphism) {
        YamlPolymorphism.Property -> withDiscriminator(name, content)
        YamlPolymorphism.Tag -> retagged(name, content)
        YamlPolymorphism.Wrapped -> created(yamlMapping(listOf(YamlEntry(key = key(name), value = content))))
    }

    private fun enter(depth: Int) {
        if (!budget.isTooDeep(depth)) return
        throw SerializationException("The value nests deeper than ${budget.maximumDepth} levels")
    }

    private fun requireRepresentable(value: String) {
        if (ScalarText.isRepresentable(value)) return
        throw SerializationException(
            "A string holds a lone surrogate or the noncharacter U+FFFE or U+FFFF, which YAML cannot represent",
        )
    }

    private fun collector(descriptor: SerialDescriptor, options: ElementOptions): NodeCollector {
        val style = options.collectionStyle()
        val kind = descriptor.kind
        val sequence = SequenceCollector(style).takeIf { kind == StructureKind.LIST }
        val map = MapCollector(style).takeIf { kind == StructureKind.MAP }
        return sequence ?: map ?: ClassCollector(this, descriptor, style)
    }

    private fun withDiscriminator(name: String, content: YamlNode): YamlNode {
        val mapping = content as? YamlNode.Mapping ?: return content
        val discriminator = configuration.classDiscriminator
        val isTaken = mapping.entries.any { entry -> entry.key.string() == discriminator }
        check(!isTaken) {
            "The class discriminator ${SafeText.quoted(discriminator)} collides with a property of the type " +
                SafeText.quoted(name)
        }
        val typeEntry = YamlEntry(key = key(discriminator), value = created(text(name, ElementOptions.NONE)))
        return mapping.withEntries(
            buildList {
                add(typeEntry)
                addAll(mapping.entries)
            },
        )
    }

    private fun retagged(name: String, content: YamlNode): YamlNode {
        val mapping = content as? YamlNode.Mapping ?: return content
        requireRepresentable(name)
        return yamlMapping(entries = mapping.entries, style = mapping.style, tag = YamlTag("!$name"))
    }
}
