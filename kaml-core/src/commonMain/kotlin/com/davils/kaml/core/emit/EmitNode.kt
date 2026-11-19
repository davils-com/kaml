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

package com.davils.kaml.core.emit

import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag

internal sealed interface EmitNode {
    val anchor: String?
    val tag: YamlTag?

    data class Scalar(
        val value: String,
        val style: YamlScalarStyle,
        override val anchor: String?,
        override val tag: YamlTag?,
        val plainTag: YamlTag?,
    ) : EmitNode {
        init {
            require(!UnsafeCharacters.hasUnrepresentable(value)) { LONE_SURROGATE }
        }

        companion object {
            const val LONE_SURROGATE: String =
                "A scalar cannot contain a lone surrogate, U+FFFE or U+FFFF, which YAML cannot represent"
        }
    }

    data class Sequence(
        val items: List<EmitNode>,
        val style: YamlCollectionStyle,
        override val anchor: String?,
        override val tag: YamlTag?,
    ) : EmitNode

    data class Mapping(
        val entries: List<Pair<EmitNode, EmitNode>>,
        val style: YamlCollectionStyle,
        override val anchor: String?,
        override val tag: YamlTag?,
        val comments: List<List<String>> = emptyList(),
    ) : EmitNode

    data class Alias(val name: String) : EmitNode {
        override val anchor: String? get() = null
        override val tag: YamlTag? get() = null
    }
}
