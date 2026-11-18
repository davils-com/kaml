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

import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.internal.GreenElement
import com.davils.kore.core.text.TextRange

internal class Located(val element: GreenElement, val offset: Int) {
    val kind: YamlSyntaxKind get() = element.kind

    val range: TextRange get() = TextRange.of(start = offset, length = element.length)

    val end: Int get() = offset + element.length

    val text: String get() = element.text

    val children: List<Located> by lazy { childrenOf(element) }

    fun child(kind: YamlSyntaxKind): Located? = children.firstOrNull { it.kind == kind }

    fun all(kind: YamlSyntaxKind): List<Located> = children.filter { it.kind == kind }

    fun valueNode(): Located? = children.firstOrNull { it.kind.isValueNode }

    fun has(kind: YamlSyntaxKind): Boolean = children.any { it.kind == kind }

    fun token(kind: YamlSyntaxKind): Located = children.first { it.kind == kind }

    private fun childrenOf(element: GreenElement): List<Located> {
        var position = offset
        return element.children.map { child ->
            val located = Located(child, position)
            position += child.length
            located
        }
    }
}
