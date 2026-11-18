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
import com.davils.kaml.core.syntax.YamlSyntaxToken
import com.davils.kore.core.text.LineMap

internal class CommentIndex(val root: YamlSyntaxNode, val lines: LineMap, private val text: String) {
    private val comments: Map<Int, YamlSyntaxToken> = commentsOf(root)

    fun trailingOn(line: Int): String? = comments[line]?.let(::textOf)

    fun above(line: Int): List<String> {
        val collected = ArrayList<String>()
        var current = line - 1
        while (current >= 0 && isCommentLine(current)) {
            collected.add(0, textOf(comments.getValue(current)))
            current--
        }
        return collected
    }

    private fun commentsOf(root: YamlSyntaxNode): Map<Int, YamlSyntaxToken> {
        val tokens = root.tokens.filter { it.kind == YamlSyntaxKind.COMMENT }
        return tokens.associateBy { token -> lines.lineOf(token.range.start) }
    }

    private fun isCommentLine(line: Int): Boolean {
        val comment = comments[line] ?: return false
        val start = lines.lineRange(line).start
        return text.substring(startIndex = start, endIndex = comment.range.start).isBlank()
    }

    private fun textOf(token: YamlSyntaxToken): String {
        val raw = token.text.removePrefix("#")
        return raw.removePrefix(" ")
    }
}
