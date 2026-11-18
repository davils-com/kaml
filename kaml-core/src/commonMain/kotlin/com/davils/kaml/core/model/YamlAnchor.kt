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

@file:OptIn(DavilsInternal::class)

package com.davils.kaml.core.model

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline

/**
 * The name of an anchor, which marks a node so that an alias can refer to it: `&name` marks the node and `*name`
 * refers to it.
 *
 * A name holds from 1 to [MAX_NAME_LENGTH] UTF-16 code units. It refuses every character YAML refuses in an anchor,
 * which are white space, line breaks, the flow indicators, that is commas, brackets and braces, the byte order mark,
 * lone surrogates and the noncharacters U+FFFE and U+FFFF. Stricter than YAML, it also refuses every control,
 * format, separator, private use and unassigned code point, the bidirectional controls and the invisible fillers, so
 * a name can neither reorder nor hide the text around it when it is written or shown. [isValidName] tells whether a
 * text is a valid name without throwing.
 *
 * An anchor is an immutable value and safe to share between threads; two anchors are equal when their names are
 * equal.
 *
 * @property name the name without the leading `&` or `*`.
 * @throws IllegalArgumentException if [name] is empty, longer than [MAX_NAME_LENGTH] or holds a character an anchor
 * cannot hold; the message does not repeat the name.
 * @since 1.0.0
 */
@JvmInline
public value class YamlAnchor(public val name: String) {
    init {
        require(isValidName(name)) { INVALID_NAME }
    }

    /**
     * Returns the anchor as YAML marks a node with it, `&` followed by the name, escaped through [SafeText] and
     * shortened to a bounded length, so the result is safe to log.
     *
     * @return the escaped, bounded form `&name`.
     * @since 1.0.0
     */
    override fun toString(): String = ANCHOR_INDICATOR + SafeText.text(name)

    /**
     * The bound of the length of an anchor name and the check of a name.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The most UTF-16 code units an anchor name may hold: 1024.
         *
         * @since 1.0.0
         */
        public const val MAX_NAME_LENGTH: Int = 1_024

        private const val ANCHOR_INDICATOR: String = "&"
        private const val INVALID_NAME: String =
            "An anchor needs a name of 1 to $MAX_NAME_LENGTH printable characters without spaces or flow indicators"

        /**
         * Returns whether [name] is a valid anchor name, the check the constructor makes.
         *
         * A parser uses it to report an invalid name in a document as a diagnostic instead of throwing.
         *
         * @param name the text to check, which may hold anything.
         * @return `true` if [name] holds from 1 to [MAX_NAME_LENGTH] characters an anchor may hold, `false` otherwise.
         * @since 1.0.0
         */
        public fun isValidName(name: String): Boolean {
            val isSized = name.length in 1..MAX_NAME_LENGTH
            return isSized && AnchorAlphabet.accepts(name)
        }
    }
}
