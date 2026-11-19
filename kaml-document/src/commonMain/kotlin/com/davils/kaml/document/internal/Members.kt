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

package com.davils.kaml.document.internal

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode

internal object Members {
    fun tokenOf(key: YamlNode): String = (key as? YamlNode.Scalar)?.value ?: key.kind.name

    fun allByToken(mapping: YamlNode.Mapping): OrderedGroups<String, YamlNode> {
        val members = OrderedGroups<String, YamlNode>()
        mapping.entries.forEach { entry -> members.add(tokenOf(entry.key), entry.value) }
        return members
    }

    fun first(mapping: YamlNode.Mapping, token: String): YamlEntry? =
        mapping.entries.firstOrNull { entry -> tokenOf(entry.key) == token }
}
