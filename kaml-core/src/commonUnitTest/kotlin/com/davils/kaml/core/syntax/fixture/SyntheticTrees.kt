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

package com.davils.kaml.core.syntax.fixture

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.internal.GreenElement
import com.davils.kaml.core.syntax.internal.GreenNode
import com.davils.kaml.core.syntax.internal.GreenToken
import com.davils.kaml.core.syntax.internal.YamlSyntaxParse
import com.davils.kaml.core.yaml

internal val relaxed: Yaml = yaml { limits = YamlLimits.Relaxed }

internal fun deepMappings(levels: Int, indent: Int = 1): YamlSyntaxTree {
    var mapping = mappingOf(GreenNode(YamlSyntaxKind.VALUE, listOf(token(YamlSyntaxKind.WHITESPACE, " "), scalar("b"))))
    for (level in levels - 1 downTo 1) {
        val value = GreenNode(
            YamlSyntaxKind.VALUE,
            listOf(
                token(YamlSyntaxKind.LINE_BREAK, "\n"),
                token(YamlSyntaxKind.WHITESPACE, " ".repeat(level * indent)),
                mapping,
            ),
        )
        mapping = mappingOf(value)
    }
    return treeOf(listOf(mapping))
}

internal fun deepSequences(levels: Int): YamlSyntaxTree {
    var sequence = GreenNode(
        YamlSyntaxKind.FLOW_SEQUENCE,
        listOf(token(YamlSyntaxKind.FLOW_SEQUENCE_START, "["), token(YamlSyntaxKind.FLOW_SEQUENCE_END, "]")),
    )
    repeat(levels - 1) {
        sequence = GreenNode(
            YamlSyntaxKind.FLOW_SEQUENCE,
            listOf(
                token(YamlSyntaxKind.FLOW_SEQUENCE_START, "["),
                sequence,
                token(YamlSyntaxKind.FLOW_SEQUENCE_END, "]"),
            ),
        )
    }
    return treeOf(listOf(sequence))
}

internal fun strayNodeTree(): YamlSyntaxTree {
    val stray = GreenNode(YamlSyntaxKind.ERROR, listOf(token(YamlSyntaxKind.UNEXPECTED, "]")))
    val document = GreenNode(YamlSyntaxKind.DOCUMENT, listOf(scalar("a")))
    val stream = GreenNode(YamlSyntaxKind.STREAM, listOf(stray, document))
    return YamlSyntaxTree(text = stream.text, parsed = YamlSyntaxParse(stream, emptyList()), yaml = relaxed)
}

private fun mappingOf(value: GreenNode): GreenNode {
    val key = GreenNode(YamlSyntaxKind.KEY, listOf(scalar("a")))
    val entry = GreenNode(
        YamlSyntaxKind.BLOCK_MAPPING_ENTRY,
        listOf(key, token(YamlSyntaxKind.MAPPING_VALUE, ":"), value),
    )
    return GreenNode(YamlSyntaxKind.BLOCK_MAPPING, listOf(entry))
}

private fun scalar(text: String): GreenNode =
    GreenNode(YamlSyntaxKind.SCALAR, listOf(token(YamlSyntaxKind.PLAIN_SCALAR, text)))

private fun token(kind: YamlSyntaxKind, text: String): GreenToken = GreenToken(kind, text)

private fun treeOf(content: List<GreenElement>): YamlSyntaxTree {
    val document = GreenNode(YamlSyntaxKind.DOCUMENT, content)
    val stream = GreenNode(YamlSyntaxKind.STREAM, listOf(document))
    return YamlSyntaxTree(text = stream.text, parsed = YamlSyntaxParse(stream, emptyList()), yaml = relaxed)
}
