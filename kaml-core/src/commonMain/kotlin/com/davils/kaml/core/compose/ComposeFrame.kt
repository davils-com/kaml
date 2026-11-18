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

package com.davils.kaml.core.compose

import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode

internal sealed interface ComposeFrame {
    var size: Long

    fun attach(node: YamlNode)

    class Document(val start: YamlEvent.DocumentStart) : ComposeFrame {
        override var size: Long = 0L
        var root: YamlNode? = null
            private set

        override fun attach(node: YamlNode) {
            root = node
        }
    }

    class Sequence(val start: YamlEvent.SequenceStart) : ComposeFrame {
        override var size: Long = 1L
        val items: MutableList<YamlNode> = ArrayList()

        override fun attach(node: YamlNode) {
            items.add(node)
        }
    }

    class Mapping(val start: YamlEvent.MappingStart) : ComposeFrame {
        override var size: Long = 1L
        val entries: MutableList<YamlEntry> = ArrayList()
        private var pendingKey: YamlNode? = null

        val count: Int get() = entries.size

        override fun attach(node: YamlNode) {
            val key = pendingKey
            if (key == null) {
                pendingKey = node
                return
            }
            entries.add(YamlEntry(key, node))
            pendingKey = null
        }
    }
}
