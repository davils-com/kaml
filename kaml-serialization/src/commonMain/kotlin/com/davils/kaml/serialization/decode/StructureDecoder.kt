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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal abstract class StructureDecoder(
    protected val context: DecodeContext,
    protected val location: YamlNode,
    private val depth: Int,
) : CompositeDecoder {
    override val serializersModule: SerializersModule get() = context.configuration.serializersModule

    protected abstract fun nodeAt(index: Int): YamlNode

    protected open fun isCharged(index: Int): Boolean = true

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = child(index).decodeBoolean()

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = child(index).decodeByte()

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = child(index).decodeShort()

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = child(index).decodeInt()

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = child(index).decodeLong()

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = child(index).decodeFloat()

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = child(index).decodeDouble()

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = child(index).decodeChar()

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = child(index).decodeString()

    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder = child(index)

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T = child(index).decodeSerializableValue(deserializer)

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?,
    ): T? {
        val child = child(index)
        val isNull = !deserializer.descriptor.isNullable && !child.decodeNotNullMark()
        if (isNull) return null
        return child.decodeSerializableValue(deserializer)
    }

    override fun endStructure(descriptor: SerialDescriptor) = Unit

    private fun child(index: Int): NodeDecoder {
        val node = nodeAt(index)
        val located = node.takeIf { node.range != null } ?: location
        return NodeDecoder(
            context = context,
            node = node,
            location = located,
            depth = depth,
            isCharged = isCharged(index),
        )
    }
}
