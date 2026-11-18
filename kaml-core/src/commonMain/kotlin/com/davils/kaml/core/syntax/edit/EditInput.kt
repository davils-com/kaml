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

package com.davils.kaml.core.syntax.edit

import com.davils.kaml.core.emit.CommentLines
import com.davils.kaml.core.node.NodeIdentity
import com.davils.kaml.core.node.YamlNode

internal object EditInput {
    private const val HEX_RADIX: Int = 16
    private const val HEX_DIGITS: Int = 4
    private const val FIRST_NONCHARACTER: Char = '\uFFFE'

    fun commentLines(comment: List<String>): List<String> = comment.flatMap(::linesOf)

    fun requireWritable(text: String) {
        val refused = text.indices.firstOrNull { index -> isUnwritable(text, index) } ?: return
        val digits = text[refused].code.toString(HEX_RADIX)
        val notation = digits.uppercase().padStart(HEX_DIGITS, '0')
        throw IllegalArgumentException("YAML cannot represent U+$notation, so the editor cannot write it")
    }

    fun requireWritable(value: YamlNode) {
        val seen = HashSet<NodeIdentity>()
        val pending = ArrayList<YamlNode>()
        pending.add(value)
        while (pending.isNotEmpty()) {
            val node = pending.removeAt(pending.lastIndex)
            val isFirstVisit = seen.add(NodeIdentity(node))
            if (isFirstVisit) pending.addAll(visit(node))
        }
    }

    private fun visit(node: YamlNode): List<YamlNode> = when (node) {
        is YamlNode.Scalar -> {
            requireWritable(node.value)
            emptyList()
        }

        is YamlNode.Sequence -> {
            node.items
        }

        is YamlNode.Mapping -> {
            node.entries.flatMap { entry -> listOf(entry.key, entry.value) }
        }
    }

    private fun linesOf(text: String): List<String> {
        val lines = CommentLines.of(text).ifEmpty { listOf("") }
        lines.forEach(::requireVisible)
        return lines
    }

    private fun requireVisible(line: String) {
        val isVisible = QuotedText.isVisible(line.replace(oldChar = '\t', newChar = ' '))
        require(isVisible) { "A comment cannot contain an invisible character: it would hide text from a reader" }
    }

    private fun isUnwritable(text: String, index: Int): Boolean {
        val character = text[index]
        val isNoncharacter = character >= FIRST_NONCHARACTER
        val isLoneSurrogate = character.isSurrogate() && !isPaired(text, index)
        return isNoncharacter || isLoneSurrogate
    }

    private fun isPaired(text: String, index: Int): Boolean {
        val character = text[index]
        val isHighWithLow = character.isHighSurrogate() && text.getOrNull(index + 1)?.isLowSurrogate() == true
        val isLowAfterHigh = character.isLowSurrogate() && text.getOrNull(index - 1)?.isHighSurrogate() == true
        return isHighWithLow || isLowAfterHigh
    }
}
