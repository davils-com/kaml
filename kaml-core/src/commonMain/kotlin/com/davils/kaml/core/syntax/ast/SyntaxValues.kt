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

package com.davils.kaml.core.syntax.ast

import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxNode

internal object SyntaxValues {
    private val entryKinds: Set<YamlSyntaxKind> = setOf(
        YamlSyntaxKind.BLOCK_MAPPING_ENTRY,
        YamlSyntaxKind.FLOW_MAPPING_ENTRY,
    )

    private val factories: Map<YamlSyntaxKind, Factory> = mapOf(
        YamlSyntaxKind.SCALAR to Factory { node, indent, _ -> YamlValueSyntax.Scalar(node, indent) },
        YamlSyntaxKind.BLOCK_SCALAR to Factory { node, indent, _ -> YamlValueSyntax.Scalar(node, indent) },
        YamlSyntaxKind.ALIAS_NODE to Factory { node, _, _ -> YamlValueSyntax.Alias(node) },
        YamlSyntaxKind.EMPTY_NODE to Factory { node, _, _ -> YamlValueSyntax.Empty(node) },
        YamlSyntaxKind.BLOCK_SEQUENCE to Factory { node, _, index -> YamlValueSyntax.Sequence(node, index) },
        YamlSyntaxKind.FLOW_SEQUENCE to Factory { node, _, index -> YamlValueSyntax.Sequence(node, index) },
        YamlSyntaxKind.BLOCK_MAPPING to Factory { node, _, index -> YamlValueSyntax.Mapping(node, index) },
        YamlSyntaxKind.FLOW_MAPPING to Factory { node, _, index -> YamlValueSyntax.Mapping(node, index) },
    )

    fun of(node: YamlSyntaxNode, parentIndent: Int, index: CommentIndex): YamlValueSyntax {
        val factory = factories.getValue(node.kind)
        return factory.create(node, parentIndent, index)
    }

    fun wrapped(wrapper: YamlSyntaxNode, parentIndent: Int, index: CommentIndex): YamlValueSyntax {
        val nodes = wrapper.childNodes.filter { it.kind.isValueNode }
        val values = nodes.map { of(it, parentIndent, index) }
        return (values + YamlValueSyntax.Empty(wrapper)).first()
    }

    fun items(node: YamlSyntaxNode, index: CommentIndex): List<YamlValueSyntax> {
        val items = node.childNodes.mapNotNull { child -> item(child, index) }
        return buildList { addAll(items) }
    }

    fun entries(node: YamlSyntaxNode, index: CommentIndex): List<YamlEntrySyntax> {
        val isPair = node.kind == YamlSyntaxKind.FLOW_PAIR
        val hosts = listOf(node).takeIf { isPair } ?: node.childNodes.filter { it.kind in entryKinds }
        return buildList { hosts.forEach { host -> add(YamlEntrySyntax(host, index)) } }
    }

    private fun item(child: YamlSyntaxNode, index: CommentIndex): YamlValueSyntax? {
        if (child.kind == YamlSyntaxKind.FLOW_PAIR) return YamlValueSyntax.Mapping(child, index)
        val column = index.lines.positionOf(child.range.start).column
        val isEntry = child.kind == YamlSyntaxKind.BLOCK_SEQUENCE_ENTRY
        val entry = child.takeIf { isEntry }?.let { wrapped(it, column, index) }
        return entry ?: child.takeIf { it.kind.isValueNode }?.let { of(it, column, index) }
    }

    private fun interface Factory {
        fun create(node: YamlSyntaxNode, indent: Int, index: CommentIndex): YamlValueSyntax
    }
}
