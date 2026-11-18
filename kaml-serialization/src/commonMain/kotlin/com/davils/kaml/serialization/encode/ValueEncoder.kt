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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.core.node.yamlScalar
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal open class ValueEncoder(
    protected val context: EncodeContext,
    private val fixedOptions: ElementOptions,
    protected val depth: Int,
    private val sink: (YamlNode) -> Unit,
) : AbstractEncoder() {
    override val serializersModule: SerializersModule get() = context.configuration.serializersModule

    protected open val options: ElementOptions get() = fixedOptions

    override fun encodeBoolean(value: Boolean) {
        put(yamlScalar(value))
    }

    override fun encodeByte(value: Byte) {
        put(yamlScalar(value.toLong()))
    }

    override fun encodeShort(value: Short) {
        put(yamlScalar(value.toLong()))
    }

    override fun encodeInt(value: Int) {
        put(yamlScalar(value))
    }

    override fun encodeLong(value: Long) {
        put(yamlScalar(value))
    }

    override fun encodeFloat(value: Float) {
        put(yamlScalar(value.toDouble()))
    }

    override fun encodeDouble(value: Double) {
        put(yamlScalar(value))
    }

    override fun encodeChar(value: Char) {
        put(context.text(value.toString(), options))
    }

    override fun encodeString(value: String) {
        put(context.text(value, options))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        put(context.text(enumDescriptor.getElementName(index), ElementOptions.NONE))
    }

    override fun encodeNull() {
        put(yamlNull())
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        context.structure(descriptor = descriptor, options = options, depth = depth, sink = sink)

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        val binary = context.binaryOrNull(serializer, value)
        if (binary != null) {
            put(binary)
            return
        }
        serializer.serialize(this, value)
    }

    private fun put(node: YamlNode) {
        sink(context.created(node))
    }
}
