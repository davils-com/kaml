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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A JSON Pointer as RFC 6901 defines it: the path from the root of a document to one of its nodes, as a list of
 * reference tokens.
 *
 * A token names a member of a mapping by the text of its key, or an item of a sequence by its decimal index without
 * leading zeros. A key that is a sequence or a mapping is named by its kind, `Sequence` or `Mapping`, since JSON has
 * no such keys. When a mapping holds a key more than once, which only a syntax tree or a graph built in code can,
 * a token resolves to its first occurrence everywhere in kaml-document: in [at], in JSONPath and in every patch.
 *
 * A pointer is immutable and safe to share between threads. [toString] shows it escaped and shortened through
 * [SafeText], since its tokens may come from an attacker; [text] is the exact RFC 6901 form.
 *
 * @since 1.0.0
 */
public class JsonPointer private constructor(tokens: List<String>) {
    /**
     * The reference tokens from the root down, unescaped; empty for the root. The list cannot be changed.
     *
     * @since 1.0.0
     */
    public val tokens: List<String> = buildList { addAll(tokens) }

    /**
     * Whether this pointer refers to the root of a document, which it does when it has no token.
     *
     * @since 1.0.0
     */
    public val isRoot: Boolean get() = tokens.isEmpty()

    /**
     * The pointer to the parent of the node this pointer refers to, or `null` for the root.
     *
     * @since 1.0.0
     */
    public val parent: JsonPointer? get() = JsonPointer(tokens.dropLast(1)).takeIf { !isRoot }

    /**
     * The last token of this pointer, or `null` for the root.
     *
     * @since 1.0.0
     */
    public val last: String? get() = tokens.lastOrNull()

    /**
     * The exact RFC 6901 form of this pointer, such as `/a~1b/0`: every token after a `/`, with `~` written as `~0`
     * and `/` as `~1`. It is not escaped for display; show it through [toString] instead.
     *
     * @since 1.0.0
     */
    public val text: String get() = tokens.joinToString("") { token -> "/" + PointerTokens.escaped(token) }

    /**
     * Returns the pointer to the member or item [token] below the node this pointer refers to.
     *
     * @param token the unescaped token to append; any text is allowed.
     * @return a new pointer with [token] as its last token.
     * @since 1.0.0
     */
    public fun child(token: String): JsonPointer = JsonPointer(tokens + token)

    /**
     * Returns the pointer to the item at [index] of the sequence this pointer refers to.
     *
     * @param index the index of the item, from zero.
     * @return a new pointer with the decimal [index] as its last token.
     * @throws IllegalArgumentException if [index] is negative.
     * @since 1.0.0
     */
    public fun child(index: Int): JsonPointer {
        require(index >= 0) { "An index of a JSON pointer must not be negative, but was $index" }
        return child(index.toString())
    }

    /**
     * Returns whether this pointer equals [prefix] or lies below it.
     *
     * @param prefix the pointer that may lead to this one.
     * @return `true` if the tokens of [prefix] start the tokens of this pointer.
     * @since 1.0.0
     */
    public fun startsWith(prefix: JsonPointer): Boolean {
        val isLongEnough = tokens.size >= prefix.tokens.size
        return isLongEnough && tokens.subList(fromIndex = 0, toIndex = prefix.tokens.size) == prefix.tokens
    }

    /**
     * Returns whether [other] is a pointer with the same tokens.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is a [JsonPointer] with equal [tokens].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is JsonPointer && tokens == other.tokens

    /**
     * Returns a hash code of the tokens of this pointer.
     *
     * @return the hash code of [tokens].
     * @since 1.0.0
     */
    override fun hashCode(): Int = tokens.hashCode()

    /**
     * Returns [text] escaped and shortened through [SafeText], so a hostile token can neither forge a line of a log
     * nor control a terminal.
     *
     * @return the pointer as it is safe to show.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = SafeText.text(text)

    /**
     * Creates and parses JSON pointers.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The pointer to the root of a document, which has no token.
         *
         * @since 1.0.0
         */
        public val ROOT: JsonPointer = JsonPointer(emptyList())

        /**
         * Returns the pointer with the unescaped [tokens].
         *
         * @param tokens the tokens from the root down; the list is copied.
         * @return the pointer.
         * @since 1.0.0
         */
        public fun of(tokens: List<String>): JsonPointer = JsonPointer(tokens)

        /**
         * Returns the pointer with the unescaped [tokens].
         *
         * @param tokens the tokens from the root down.
         * @return the pointer.
         * @since 1.0.0
         */
        public fun of(vararg tokens: String): JsonPointer = JsonPointer(tokens.asList())

        /**
         * Parses the RFC 6901 form of a pointer, such as `/a~1b/0`.
         *
         * @param text the pointer; the empty text is the root.
         * @return the pointer.
         * @throws IllegalArgumentException if [text] does not start with `/` or holds a `~` that is not followed by
         * `0` or `1`; the message shows [text] escaped and shortened.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        public fun parse(text: String): JsonPointer =
            requireNotNull(parseOrNull(text)) { "${SafeText.quoted(text)} is not a JSON pointer" }

        /**
         * Parses the RFC 6901 form of a pointer, such as `/a~1b/0`, and never throws.
         *
         * @param text the pointer; the empty text is the root.
         * @return the pointer, or `null` if [text] is not one.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): JsonPointer? {
            if (text.isEmpty()) return ROOT
            val tokens = text.split("/").map(PointerTokens::unescaped)
            val isValid = tokens.first() == "" && tokens.none { it == null }
            return JsonPointer(tokens.drop(1).filterNotNull()).takeIf { isValid }
        }
    }
}
