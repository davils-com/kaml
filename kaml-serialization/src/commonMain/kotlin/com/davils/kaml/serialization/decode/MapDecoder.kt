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
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder

internal class MapDecoder(
    context: DecodeContext,
    node: YamlNode,
    location: YamlNode,
    depth: Int,
) : StructureDecoder(context, location, depth) {
    private val mapping: YamlNode.Mapping = node as? YamlNode.Mapping ?: context.mismatch(location, "a mapping")
    private val keys = DecodedKeys()
    private var next = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (next >= mapping.entries.size * 2) return CompositeDecoder.DECODE_DONE
        return next++
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = mapping.entries.size

    override fun nodeAt(index: Int): YamlNode {
        val entry = mapping.entries[index / 2]
        return entry.value.takeIf { index % 2 == 1 } ?: entry.key
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T {
        val value = super.decodeSerializableElement(
            descriptor = descriptor,
            index = index,
            deserializer = deserializer,
            previousValue = previousValue,
        )
        if (index % 2 == 0) keys.add(value, nodeAt(index))
        return value
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        keys.repeated().forEach { key ->
            context.report(
                YamlDiagnosticCode.DuplicateKey,
                key,
                "The key ${context.describeValue(key)} is given more than once",
            )
        }
    }
}
