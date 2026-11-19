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

package com.davils.kaml.document.merge.trace

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.internal.KeyedSlots

internal sealed interface Traced {
    val origin: String

    data class Whole(val node: YamlNode, override val origin: String) : Traced

    data class Members(
        val template: YamlNode.Mapping,
        val slots: KeyedSlots<TracedEntry>,
        override val origin: String,
    ) : Traced {
        val entries: List<TracedEntry> get() = slots.items
    }

    data class Items(
        val template: YamlNode.Sequence,
        val slots: KeyedSlots<Traced>,
        val key: String?,
        override val origin: String,
    ) : Traced {
        val all: List<Traced> get() = slots.items
    }
}
