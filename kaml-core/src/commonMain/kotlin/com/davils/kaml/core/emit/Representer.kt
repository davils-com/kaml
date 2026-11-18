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

import com.davils.kaml.core.YamlConfiguration
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.NodeIdentity
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode

internal class Representer(
    private val configuration: YamlConfiguration,
    private val comments: YamlComments = YamlComments.None,
    takenAnchors: Set<String> = emptySet(),
) {
    private val options = configuration.emitter
    private val limits = configuration.limits
    private val references = HashMap<NodeIdentity, Int>()
    private val names = HashMap<NodeIdentity, String>()
    private val anchors = AnchorNames(taken = takenAnchors)
    private var resolution = PlainResolution.of(configuration, version = null)

    fun document(document: YamlDocument, version: YamlVersion? = document.version): EmitDocument {
        references.clear()
        names.clear()
        anchors.clear()
        resolution = PlainResolution.of(configuration, version)
        count(document.root)
        return EmitDocument(
            root = node(document.root, depth = 0),
            version = document.version,
            tagDirectives = document.tagDirectives,
            isExplicitStart = document.isExplicitStart,
            isExplicitEnd = document.isExplicitEnd,
            resolution = resolution,
            comments = comments.of(document.root),
        )
    }

    private fun count(root: YamlNode) {
        val pending = ArrayDeque<YamlNode>()
        pending.addLast(root)
        while (pending.isNotEmpty()) {
            val node = pending.removeLast()
            val identity = NodeIdentity(node)
            val seen = references[identity] ?: 0
            references[identity] = seen + 1
            if (seen == 0) pending.addAll(children(node))
        }
    }

    private fun children(node: YamlNode): List<YamlNode> = when (node) {
        is YamlNode.Scalar -> emptyList()
        is YamlNode.Sequence -> node.items
        is YamlNode.Mapping -> node.entries.flatMap { entry -> listOf(entry.key, entry.value) }
    }

    private fun node(node: YamlNode, depth: Int): EmitNode {
        val existing = names[NodeIdentity(node)]
        if (existing != null) return EmitNode.Alias(existing)
        val anchor = anchorFor(node)
        return when (node) {
            is YamlNode.Scalar -> scalar(node, anchor)
            is YamlNode.Sequence -> sequence(node, anchor, nested(depth))
            is YamlNode.Mapping -> mapping(node, anchor, nested(depth))
        }
    }

    private fun sequence(node: YamlNode.Sequence, anchor: String?, depth: Int): EmitNode.Sequence = EmitNode.Sequence(
        items = node.items.map { item -> node(item, depth) },
        style = node.style,
        anchor = anchor,
        tag = node.tag.takeIf { it != YamlTag.SEQUENCE },
    )

    private fun mapping(node: YamlNode.Mapping, anchor: String?, depth: Int): EmitNode.Mapping {
        val ordered = ordered(node)
        return EmitNode.Mapping(
            entries = ordered.map { entry -> node(entry.key, depth) to node(entry.value, depth) },
            comments = ordered.map { entry -> comments.of(entry.key) },
            style = node.style,
            anchor = anchor,
            tag = node.tag.takeIf { it != YamlTag.MAPPING },
        )
    }

    private fun nested(depth: Int): Int {
        val nesting = depth + 1
        require(nesting < limits.maximumDepth) { tooDeep(limits.maximumDepth) }
        return nesting
    }

    private fun ordered(node: YamlNode.Mapping): List<YamlEntry> {
        if (!options.hasSortedKeys) return node.entries
        return node.entries.sortedBy(::sortKey)
    }

    private fun sortKey(entry: YamlEntry): String = (entry.key as? YamlNode.Scalar)?.value.orEmpty()

    private fun anchorFor(node: YamlNode): String? {
        val isShared = references.getValue(NodeIdentity(node)) > 1
        if (!isShared) return null
        val name = anchors.preferred(node.anchor?.name)
        names[NodeIdentity(node)] = name
        return name
    }

    private fun scalar(node: YamlNode.Scalar, anchor: String?): EmitNode.Scalar {
        val isTyped = node.tag in implicitTags
        val isCanonicalNeeded = isTyped && !keeps(node.value, node)
        val text = CanonicalText.of(node).takeIf { isCanonicalNeeded } ?: node.value
        val isImpliedType = isTyped && keeps(text, node)
        val isImplied = node.tag == YamlTag.STRING || isImpliedType
        return EmitNode.Scalar(
            value = text,
            style = YamlScalarStyle.Plain.takeIf { isImpliedType } ?: node.style,
            anchor = anchor,
            tag = node.tag.takeIf { !isImplied },
            plainTag = node.tag.takeIf { isImplied },
        )
    }

    private fun keeps(text: String, node: YamlNode.Scalar): Boolean =
        resolution.keeps(text = text, tag = node.tag, value = node.canonical)

    companion object {
        private val implicitTags: Set<YamlTag> = setOf(
            YamlTag.NULL,
            YamlTag.BOOLEAN,
            YamlTag.INTEGER,
            YamlTag.FLOAT,
        )

        fun tooDeep(maximumDepth: Int): String = "The node nests collections deeper than $maximumDepth levels"
    }
}
