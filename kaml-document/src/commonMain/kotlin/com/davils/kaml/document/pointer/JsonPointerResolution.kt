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

package com.davils.kaml.document.pointer

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.ast.YamlValueSyntax

/**
 * Parses this text as the RFC 6901 form of a JSON pointer.
 *
 * @receiver the pointer, such as `/a/0`; the empty text is the root.
 * @return the pointer.
 * @throws IllegalArgumentException if the text is not a JSON pointer; see [JsonPointer.parse].
 * @since 1.0.0
 */
public fun String.toJsonPointer(): JsonPointer = JsonPointer.parse(this)

/**
 * Parses this text as the RFC 6901 form of a JSON pointer and never throws.
 *
 * @receiver the pointer, such as `/a/0`; the empty text is the root.
 * @return the pointer, or `null` if the text is not one.
 * @since 1.0.0
 */
public fun String.toJsonPointerOrNull(): JsonPointer? = JsonPointer.parseOrNull(this)

/**
 * Returns the node [pointer] refers to below this node.
 *
 * A token selects the member of a mapping whose key has the text of the token, the first one when the key occurs more
 * than once, or the item of a sequence at the decimal index of the token. The walk is iterative, so a pointer of any
 * length is safe; it takes time in proportion to the tokens times the members of the mappings on the way.
 *
 * @receiver the node the pointer starts at.
 * @param pointer the pointer to resolve.
 * @return the node, or `null` if a token names no member or item on the way.
 * @since 1.0.0
 */
public fun YamlNode.at(pointer: JsonPointer): YamlNode? {
    var current: YamlNode? = this
    pointer.tokens.forEach { token -> current = current?.let { node -> PointerSteps.child(node, token) } }
    return current
}

/**
 * Returns the syntax of the value [pointer] refers to in the document at [document] of this tree.
 *
 * The tokens are resolved as for [YamlNode.at]: the first occurrence of a duplicate key wins. An alias is not
 * followed, so a pointer through an alias resolves to `null`; resolve such a pointer on the composed node instead.
 *
 * @receiver the syntax tree to look into.
 * @param pointer the pointer to resolve.
 * @param document the index of the document in the tree, from zero.
 * @return the syntax of the value, or `null` if the document or the value does not exist.
 * @since 1.0.0
 */
public fun YamlSyntaxTree.at(pointer: JsonPointer, document: Int = 0): YamlValueSyntax? {
    var current: YamlValueSyntax? = documents.getOrNull(document)?.root
    pointer.tokens.forEach { token -> current = current?.let { value -> SyntaxSteps.child(value, token) } }
    return current
}

/**
 * Returns the syntax of the deepest value on the way of [pointer] in the document at [document] of this tree: the
 * value [pointer] refers to if it exists, otherwise that of its nearest ancestor that does.
 *
 * @receiver the syntax tree to look into.
 * @param pointer the pointer to resolve as far as it goes.
 * @param document the index of the document in the tree, from zero.
 * @return the syntax of the deepest value found, or `null` if the document does not exist.
 * @since 1.0.0
 */
public fun YamlSyntaxTree.nearest(pointer: JsonPointer, document: Int = 0): YamlValueSyntax? {
    var current: YamlValueSyntax? = documents.getOrNull(document)?.root
    var reached = current
    pointer.tokens.forEach { token ->
        current = current?.let { value -> SyntaxSteps.child(value, token) }
        reached = current ?: reached
    }
    return reached
}
