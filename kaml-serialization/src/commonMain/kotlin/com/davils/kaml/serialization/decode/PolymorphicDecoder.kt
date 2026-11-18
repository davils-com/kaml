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

import com.davils.kaml.core.node.YamlNode
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder

internal class PolymorphicDecoder(
    context: DecodeContext,
    private val typed: TypedNode,
    location: YamlNode,
    depth: Int,
) : StructureDecoder(context, location, depth) {
    private var next = 0
    private var isValueRead = false

    val unresolvedType: String? get() = typed.name.value.takeUnless { isValueRead }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (next > TypedNode.VALUE) return CompositeDecoder.DECODE_DONE
        return next++
    }

    override fun nodeAt(index: Int): YamlNode = typed.nodeAt(index)

    override fun isCharged(index: Int): Boolean = typed.isCharged(index)

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T {
        isValueRead = true
        return super.decodeSerializableElement(
            descriptor = descriptor,
            index = index,
            deserializer = deserializer,
            previousValue = previousValue,
        )
    }
}
