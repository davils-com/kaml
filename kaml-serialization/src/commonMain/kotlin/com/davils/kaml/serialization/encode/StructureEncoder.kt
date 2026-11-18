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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor

@OptIn(ExperimentalSerializationApi::class)
internal class StructureEncoder(
    context: EncodeContext,
    private val collector: NodeCollector,
    depth: Int,
    private val parent: (YamlNode) -> Unit,
) : ValueEncoder(context, ElementOptions.NONE, depth, collector::add) {
    private var current: ElementOptions = ElementOptions.NONE

    override val options: ElementOptions get() = current

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        current = collector.element(descriptor, index)
        return true
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        context.configuration.isDefaultEncoded

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?,
    ) {
        val isOmitted = value == null && collector.isNullOmitted
        if (isOmitted) return
        encodeElement(descriptor, index)
        if (value == null) encodeNull()
        value?.let { present -> encodeSerializableValue(serializer, present) }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        parent(context.created(collector.build()))
    }
}
