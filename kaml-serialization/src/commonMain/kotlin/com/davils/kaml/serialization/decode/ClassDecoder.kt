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
import com.davils.kaml.core.node.isNull
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.serialization.PropertyNames
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.CompositeDecoder

@OptIn(ExperimentalSerializationApi::class)
internal class ClassDecoder(
    context: DecodeContext,
    descriptor: SerialDescriptor,
    node: YamlNode,
    location: YamlNode,
    depth: Int,
) : StructureDecoder(context, location, depth) {
    private val configuration = context.configuration
    private val values = HashMap<Int, YamlNode>()
    private val order: ArrayDeque<Int>

    init {
        val mapping = node as? YamlNode.Mapping ?: context.mismatch(location, "a mapping")
        val names = context.namesOf(descriptor)
        mapping.entries.forEach { entry -> place(names = names, key = entry.key, value = entry.value) }
        val decoded = (0 until descriptor.elementsCount).filter { index -> isDecoded(descriptor, index) }
        order = ArrayDeque(decoded)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        order.removeFirstOrNull() ?: CompositeDecoder.DECODE_DONE

    override fun nodeAt(index: Int): YamlNode = values[index] ?: yamlNull()

    override fun isCharged(index: Int): Boolean = index in values

    private fun place(names: PropertyNames, key: YamlNode, value: YamlNode) {
        val name = (key as? YamlNode.Scalar)?.value
        val index = name?.let(names::indexOf)
        if (index == null) {
            unknown(key)
            return
        }
        val kept = values.getOrPut(index) { value }
        if (kept !== value) {
            context.report(
                YamlDiagnosticCode.DuplicateKey,
                key,
                "The property ${context.describe(key)} is given more than once",
            )
        }
    }

    private fun unknown(key: YamlNode) {
        if (configuration.isUnknownKeyIgnored) return
        context.report(YamlDiagnosticCode.UnknownProperty, key, "There is no property ${context.describe(key)}")
    }

    private fun isDecoded(descriptor: SerialDescriptor, index: Int): Boolean {
        val value = values[index] ?: return isImplicitNull(descriptor, index)
        return !isCoerced(descriptor, index, value)
    }

    private fun isImplicitNull(descriptor: SerialDescriptor, index: Int): Boolean {
        val element = descriptor.getElementDescriptor(index)
        return !configuration.hasExplicitNulls && element.isNullable && !descriptor.isElementOptional(index)
    }

    private fun isCoerced(descriptor: SerialDescriptor, index: Int, value: YamlNode): Boolean {
        val element = descriptor.getElementDescriptor(index)
        val isOptional = configuration.isInputCoerced && descriptor.isElementOptional(index)
        val isNullForValue = value.isNull && !element.isNullable
        val isUnknownEnum = element.kind == SerialKind.ENUM && isUnknownEnum(element, value)
        return isOptional && (isNullForValue || isUnknownEnum)
    }

    private fun isUnknownEnum(element: SerialDescriptor, value: YamlNode): Boolean {
        val scalar = value as? YamlNode.Scalar ?: return true
        return element.getElementIndex(scalar.value) == CompositeDecoder.UNKNOWN_NAME
    }
}
