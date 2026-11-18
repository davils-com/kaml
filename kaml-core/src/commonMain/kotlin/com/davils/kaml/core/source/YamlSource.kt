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

package com.davils.kaml.core.source

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * The text or the bytes of a YAML stream, with an optional name.
 *
 * Bytes are decoded strictly when the stream is read: the encoding is detected from a byte order mark or from the zero
 * bytes among the first four bytes, as YAML 1.2 asks, and is UTF-8 otherwise. Bytes that are no valid UTF-8, UTF-16
 * or UTF-32, such as overlong forms, truncated sequences, encoded or unpaired surrogates and code points above
 * U+10FFFF, make the read fail with an invalid encoding diagnostic that names the offset of the first bad byte; they
 * are never repaired silently. The input limit of the configuration bounds a source before it is decoded and again
 * before it is parsed.
 *
 * A source is immutable and safe to share between threads: [of] copies the bytes it is given.
 *
 * @since 1.0.0
 */
public class YamlSource private constructor(
    internal val text: String?,
    internal val bytes: ByteArray,
    /**
     * The name of the source, such as a file name, or null. It may contain anything and is escaped wherever it is
     * shown.
     *
     * @since 1.0.0
     */
    public val name: String?,
) {
    internal val byteLength: Int get() = bytes.size

    override fun toString(): String {
        val known = name ?: return "YamlSource(unnamed)"
        return "YamlSource(${SafeText.quoted(known)})"
    }

    /**
     * Creates sources.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Creates a source of [text], which is read as it is, without decoding.
         *
         * @param text the YAML stream; its characters are checked when it is read.
         * @param name the name of the source, or null.
         * @return the source.
         * @since 1.0.0
         */
        public fun of(text: String, name: String? = null): YamlSource =
            YamlSource(text = text, bytes = ByteArray(0), name = name)

        /**
         * Creates a source of [bytes] in UTF-8, UTF-16 or UTF-32, which are decoded when the source is read.
         *
         * @param bytes the encoded YAML stream; it is copied, so later changes to the array do not reach the source.
         * @param name the name of the source, or null.
         * @return the source.
         * @since 1.0.0
         */
        public fun of(bytes: ByteArray, name: String? = null): YamlSource =
            YamlSource(text = null, bytes = bytes.copyOf(), name = name)
    }
}
