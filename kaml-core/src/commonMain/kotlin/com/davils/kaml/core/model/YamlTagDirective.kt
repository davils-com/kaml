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

private const val HASH_FACTOR: Int = 31

/**
 * A `%TAG` directive of a document, which binds a tag handle to a prefix, such as `%TAG !e! tag:example.com,2000:`.
 *
 * The parser checks the handle and the prefix of the directives it reads; this class keeps what it is given. A
 * directive is an immutable value and safe to share between threads; two directives are equal when their handles and
 * their prefixes are equal.
 *
 * @param handle the tag handle, such as `!`, `!!` or `!e!`.
 * @param prefix the prefix the handle stands for, such as `tag:example.com,2000:`.
 * @since 1.0.0
 */
public class YamlTagDirective(handle: String, prefix: String) {
    /**
     * The tag handle, such as `!`, `!!` or `!e!`.
     *
     * @since 1.0.0
     */
    public val handle: String = handle

    /**
     * The prefix the handle stands for, such as `tag:example.com,2000:`.
     *
     * @since 1.0.0
     */
    public val prefix: String = prefix

    /**
     * Returns whether [other] is a directive with the same [handle] and the same [prefix].
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal directive, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is YamlTagDirective && handle == other.handle && prefix == other.prefix

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of the handle and the prefix.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * handle.hashCode() + prefix.hashCode()

    /**
     * Returns the directive with its handle and prefix quoted and escaped through [SafeText], so the result is
     * bounded and safe to log.
     *
     * @return a text such as `YamlTagDirective(handle="!e!", prefix="tag:example.com,2000:")`.
     * @since 1.0.0
     */
    override fun toString(): String =
        "YamlTagDirective(handle=${SafeText.quoted(handle)}, prefix=${SafeText.quoted(prefix)})"
}
