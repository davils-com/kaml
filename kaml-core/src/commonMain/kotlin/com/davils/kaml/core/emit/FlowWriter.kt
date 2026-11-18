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

internal class FlowWriter(private val styler: ScalarStyler, private val tags: TagNotation) {
    fun inline(node: EmitNode, context: ScalarContext, out: OutputBuffer) {
        val properties = properties(node)
        out.append(properties)
        val isSeparated = properties.isNotEmpty() && !isEmptyBody(node, context)
        if (isSeparated) out.append(' ')
        body(node, context, out)
    }

    fun isEmptyBody(node: EmitNode, context: ScalarContext): Boolean =
        node is EmitNode.Scalar && styler.isEmptyBody(node, context)

    fun properties(node: EmitNode): String {
        val anchor = node.anchor?.let { name -> "&$name" }
        val tag = node.tag?.let(tags::of)
        return listOfNotNull(anchor, tag).joinToString(separator = " ")
    }

    private fun body(node: EmitNode, context: ScalarContext, out: OutputBuffer) {
        when (node) {
            is EmitNode.Alias -> out.append("*${node.name}")
            is EmitNode.Scalar -> styler.inline(node, context, out)
            is EmitNode.Sequence -> sequence(node, out)
            is EmitNode.Mapping -> mapping(node, out)
        }
    }

    private fun sequence(node: EmitNode.Sequence, out: OutputBuffer) {
        out.append('[')
        node.items.forEachIndexed { index, item ->
            if (index > 0) out.append(SEPARATOR)
            inline(item, ScalarContext.FLOW, out)
        }
        out.append(']')
    }

    private fun mapping(node: EmitNode.Mapping, out: OutputBuffer) {
        out.append('{')
        node.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) out.append(SEPARATOR)
            inline(key, ScalarContext.FLOW_KEY, out)
            if (key is EmitNode.Alias) out.append(' ')
            out.append(": ")
            inline(value, ScalarContext.FLOW, out)
        }
        out.append('}')
    }

    private companion object {
        const val SEPARATOR: String = ", "
    }
}
