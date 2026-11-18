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

private const val CORE_PREFIX: String = "tag:yaml.org,2002:"
private const val NON_SPECIFIC_URI: String = "!"

/**
 * The tag of a node, which names its type: a global tag such as `tag:yaml.org,2002:str`, a local tag such as `!point`
 * or the non-specific tag `!`.
 *
 * A tag holds its resolved form, after the handle of a shorthand such as `!!str` has been replaced by its prefix and
 * percent escapes have been decoded, so it may hold any character a document holds. A tag only names a type: kaml
 * never constructs an object from a tag. [toString] escapes the URI, and the emitter percent-encodes it, so a tag
 * can neither forge a log line nor inject YAML.
 *
 * A tag is an immutable value and safe to share between threads; two tags are equal when their URIs are equal.
 *
 * @property uri the resolved URI of the tag.
 * @throws IllegalArgumentException if [uri] is empty.
 * @since 1.0.0
 */
@JvmInline
public value class YamlTag(public val uri: String) {
    init {
        require(uri.isNotEmpty()) { "A tag needs a URI" }
    }

    /**
     * Whether this is the non-specific tag `!`, which a plain scalar never has and which forces a quoted scalar to be
     * a string and a collection to be a generic sequence or mapping.
     *
     * @since 1.0.0
     */
    public val isNonSpecific: Boolean get() = uri == NON_SPECIFIC_URI

    /**
     * Whether this is a local tag, which starts with `!` and is more than `!`, such as `!point`.
     *
     * @since 1.0.0
     */
    public val isLocal: Boolean get() = uri.startsWith(NON_SPECIFIC_URI) && !isNonSpecific

    /**
     * Whether this tag starts with the prefix of the tags of YAML, [CORE_TAG_PREFIX].
     *
     * @since 1.0.0
     */
    public val isCore: Boolean get() = uri.startsWith(CORE_PREFIX)

    /**
     * Returns the tag in angle brackets, `<uri>`, with the URI escaped through [SafeText] and shortened to a bounded
     * length, so the result is safe to log.
     *
     * @return the escaped, bounded form `<uri>`.
     * @since 1.0.0
     */
    override fun toString(): String = "<${SafeText.text(uri)}>"

    /**
     * The tags of YAML and the non-specific tag.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The prefix `tag:yaml.org,2002:` of the tags of YAML, which the secondary handle `!!` stands for by default.
         *
         * @since 1.0.0
         */
        public const val CORE_TAG_PREFIX: String = CORE_PREFIX

        /**
         * The non-specific tag `!`.
         *
         * @since 1.0.0
         */
        public val NON_SPECIFIC: YamlTag = YamlTag(NON_SPECIFIC_URI)

        /**
         * The tag `tag:yaml.org,2002:str` of strings.
         *
         * @since 1.0.0
         */
        public val STRING: YamlTag = core("str")

        /**
         * The tag `tag:yaml.org,2002:int` of integers.
         *
         * @since 1.0.0
         */
        public val INTEGER: YamlTag = core("int")

        /**
         * The tag `tag:yaml.org,2002:float` of floating-point numbers.
         *
         * @since 1.0.0
         */
        public val FLOAT: YamlTag = core("float")

        /**
         * The tag `tag:yaml.org,2002:bool` of booleans.
         *
         * @since 1.0.0
         */
        public val BOOLEAN: YamlTag = core("bool")

        /**
         * The tag `tag:yaml.org,2002:null` of the null value.
         *
         * @since 1.0.0
         */
        public val NULL: YamlTag = core("null")

        /**
         * The tag `tag:yaml.org,2002:seq` of sequences.
         *
         * @since 1.0.0
         */
        public val SEQUENCE: YamlTag = core("seq")

        /**
         * The tag `tag:yaml.org,2002:map` of mappings.
         *
         * @since 1.0.0
         */
        public val MAPPING: YamlTag = core("map")

        /**
         * The tag `tag:yaml.org,2002:binary` of Base64 encoded binary data of YAML 1.1.
         *
         * @since 1.0.0
         */
        public val BINARY: YamlTag = core("binary")

        /**
         * The tag `tag:yaml.org,2002:timestamp` of timestamps of YAML 1.1.
         *
         * @since 1.0.0
         */
        public val TIMESTAMP: YamlTag = core("timestamp")

        /**
         * The tag `tag:yaml.org,2002:merge` of the merge key `<<` of YAML 1.1.
         *
         * @since 1.0.0
         */
        public val MERGE: YamlTag = core("merge")

        /**
         * The tag `tag:yaml.org,2002:set` of sets of YAML 1.1.
         *
         * @since 1.0.0
         */
        public val SET: YamlTag = core("set")

        /**
         * The tag `tag:yaml.org,2002:omap` of ordered mappings of YAML 1.1.
         *
         * @since 1.0.0
         */
        public val ORDERED_MAP: YamlTag = core("omap")

        /**
         * The tag `tag:yaml.org,2002:pairs` of sequences of pairs of YAML 1.1.
         *
         * @since 1.0.0
         */
        public val PAIRS: YamlTag = core("pairs")

        /**
         * The tag `tag:yaml.org,2002:value` of the value key `=` of YAML 1.1.
         *
         * @since 1.0.0
         */
        public val VALUE: YamlTag = core("value")

        /**
         * Returns the tag of YAML named [name], [CORE_TAG_PREFIX] followed by [name].
         *
         * @param name the name after the prefix, such as `str`.
         * @return the tag `tag:yaml.org,2002:` followed by [name].
         * @since 1.0.0
         */
        public fun core(name: String): YamlTag = YamlTag(CORE_PREFIX + name)
    }
}
