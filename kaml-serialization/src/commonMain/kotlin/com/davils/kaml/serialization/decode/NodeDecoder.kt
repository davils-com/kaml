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
import com.davils.kaml.core.node.boolean
import com.davils.kaml.core.node.isNull
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class, DavilsInternal::class)
internal class NodeDecoder(
    private val context: DecodeContext,
    val node: YamlNode,
    val location: YamlNode,
    val depth: Int,
    isCharged: Boolean,
) : AbstractDecoder() {
    private var polymorphic: PolymorphicDecoder? = null

    init {
        if (isCharged) context.visit(location)
    }

    val unresolvedType: String? get() = polymorphic?.unresolvedType

    override val serializersModule: SerializersModule get() = context.configuration.serializersModule

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = CompositeDecoder.DECODE_DONE

    override fun decodeNotNullMark(): Boolean = !node.isNull

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = node.boolean() ?: context.mismatch(location, "a boolean")

    override fun decodeByte(): Byte = context.numbers.integer(node, location, BYTES).toByte()

    override fun decodeShort(): Short = context.numbers.integer(node, location, SHORTS).toShort()

    override fun decodeInt(): Int = context.numbers.integer(node, location, INTS).toInt()

    override fun decodeLong(): Long = context.numbers.long(node, location)

    override fun decodeFloat(): Float = context.numbers.float(node, location)

    override fun decodeDouble(): Double = context.numbers.real(node, location)

    override fun decodeChar(): Char = text().singleOrNull() ?: context.mismatch(location, "a single character")

    override fun decodeString(): String = text()

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = text()
        val index = enumDescriptor.getElementIndex(name)
        if (index == CompositeDecoder.UNKNOWN_NAME) {
            val known = SafeText.text(enumDescriptor.elementNames.joinToString())
            context.fail(YamlDiagnosticCode.UnknownEnumValue, location, "${SafeText.quoted(name)} is none of $known")
        }
        return index
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val structure = context.structure(descriptor, this)
        polymorphic = structure as? PolymorphicDecoder
        return structure
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        val bytes = context.binaryOrNull(deserializer.descriptor, this)
        @Suppress("UNCHECKED_CAST")
        if (bytes != null) return bytes as T
        return try {
            deserializer.deserialize(this)
        } catch (refusal: IllegalArgumentException) {
            throw context.refused(refusal, deserializer.descriptor, this)
        }
    }

    private fun text(): String {
        val scalar = node as? YamlNode.Scalar
        if (scalar == null || scalar.isNull) context.mismatch(location, "a string")
        return scalar.value
    }

    private companion object {
        val BYTES: LongRange = Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()
        val SHORTS: LongRange = Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()
        val INTS: LongRange = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
    }
}
