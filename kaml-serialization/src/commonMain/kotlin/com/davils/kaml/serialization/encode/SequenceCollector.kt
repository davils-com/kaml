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

import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlSequence
import kotlinx.serialization.descriptors.SerialDescriptor

internal class SequenceCollector(private val style: YamlCollectionStyle) : NodeCollector {
    private val items = ArrayList<YamlNode>()

    override val isNullOmitted: Boolean get() = false

    override fun element(descriptor: SerialDescriptor, index: Int): ElementOptions = ElementOptions.NONE

    override fun add(node: YamlNode) {
        items.add(node)
    }

    override fun build(): YamlNode = yamlSequence(items = items, style = style)
}
