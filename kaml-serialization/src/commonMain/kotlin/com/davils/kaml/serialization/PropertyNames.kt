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

package com.davils.kaml.serialization

import com.davils.kaml.serialization.annotation.YamlNames
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

@OptIn(ExperimentalSerializationApi::class, DavilsInternal::class)
internal class PropertyNames private constructor(private val descriptor: SerialDescriptor) {
    private val names = ArrayList<String>()
    private val indices = HashMap<String, Int>()

    fun nameOf(index: Int): String = names[index]

    fun indexOf(name: String): Int? = indices[name]

    private fun claim(name: String, index: Int) {
        val owner = indices.getOrPut(name) { index }
        check(owner == index) {
            "The class ${SafeText.quoted(descriptor.serialName)} uses the YAML name ${SafeText.quoted(name)} " +
                "for two properties"
        }
    }

    private fun aliasesOf(index: Int): List<String> {
        val annotations = descriptor.getElementAnnotations(index).filterIsInstance<YamlNames>()
        return annotations.flatMap { annotation -> annotation.names.asList() }
    }

    companion object {
        fun of(descriptor: SerialDescriptor, strategy: YamlNamingStrategy): PropertyNames {
            val names = PropertyNames(descriptor)
            val elements = 0 until descriptor.elementsCount
            elements.forEach { index ->
                names.names += strategy.nameOf(descriptor, index, descriptor.getElementName(index))
            }
            names.names.forEachIndexed { index, name -> names.claim(name, index) }
            elements.forEach { index -> names.aliasesOf(index).forEach { alias -> names.claim(alias, index) } }
            return names
        }
    }
}
