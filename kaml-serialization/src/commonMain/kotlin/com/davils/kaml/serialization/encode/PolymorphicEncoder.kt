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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class PolymorphicEncoder(
    private val context: EncodeContext,
    private val options: ElementOptions,
    private val depth: Int,
    private val parent: (YamlNode) -> Unit,
) : AbstractEncoder() {
    private var name: String = ""
    private var content: YamlNode = yamlNull()

    override val serializersModule: SerializersModule get() = context.configuration.serializersModule

    override fun encodeString(value: String) {
        name = value
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
    ) {
        context.encode(serializer = serializer, value = value, options = options, depth = depth) { node ->
            content = node
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        parent(context.typed(name, content))
    }
}
