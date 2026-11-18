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

package com.davils.kaml.core.node

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlNodeKind
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange

private const val HASH_FACTOR: Int = 31

/**
 * A node of the representation graph of YAML: a [Scalar], a [Sequence] or a [Mapping].
 *
 * [com.davils.kaml.core.Yaml.parse] composes nodes from a source, and [yamlScalar], [yamlSequence], [yamlMapping] and
 * their siblings build them in code. A node that an alias refers to is shared: the alias and its anchor stand for the
 * same instance, so a graph is a directed acyclic graph rather than a tree.
 *
 * Nodes are compared by their content, not by their place in a source: two nodes are equal when they are of the same
 * kind and have equal tags and equal content, a scalar by the value its tag gives it, so `0x10` equals `16`, a
 * sequence by its items in order and a mapping by its entries in any order. Anchors, styles and ranges never take
 * part. Every node knows its [depth], which the constructor computes from the children and refuses beyond
 * [YamlLimits.HARD_DEPTH], so [equals], [hashCode], [detached] and every other recursive walk of a node is bounded,
 * and a hash code is computed once, when a collection is built.
 *
 * Nodes are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public sealed interface YamlNode {
    /**
     * The kind of the node.
     *
     * @since 1.0.0
     */
    public val kind: YamlNodeKind

    /**
     * The tag of the node: the tag the source gives it, or the tag its schema resolves, such as
     * [YamlTag.INTEGER] for a plain `1`.
     *
     * @since 1.0.0
     */
    public val tag: YamlTag

    /**
     * The anchor of the node in its source, or `null` without one.
     *
     * The anchor is presentation only: it never takes part in [equals], and the emitter may give a shared node another
     * name.
     *
     * @since 1.0.0
     */
    public val anchor: YamlAnchor?

    /**
     * The place of the node in its source, in UTF-16 offsets of the decoded text, or `null` for a node built in code
     * or [detached] from its source.
     *
     * @since 1.0.0
     */
    public val range: TextRange?

    /**
     * How many collections nest on the longest path from this node down to a scalar or an empty collection, this node
     * included: 0 for a scalar, 1 for an empty collection or a collection of scalars, and one more than its deepest
     * item, key or value for every other collection.
     *
     * It is at most [YamlLimits.HARD_DEPTH]: the constructor of a collection refuses a deeper one, whether the
     * composer or code builds it.
     *
     * @since 1.0.0
     */
    public val depth: Int

    /**
     * A scalar: a text and the value its tag gives it.
     *
     * Two scalars are equal when their tags are equal and their values are equal, such as `0x10` and `16` as integers
     * or `1.0` and `1.00` as floats; two strings are equal when their texts are equal.
     *
     * @since 1.0.0
     */
    public class Scalar internal constructor(
        /**
         * The text of the scalar after escapes, folding and chomping, which may hold any character.
         *
         * @since 1.0.0
         */
        public val value: String,
        override val tag: YamlTag,
        /**
         * The style the scalar is written in.
         *
         * @since 1.0.0
         */
        public val style: YamlScalarStyle,
        override val anchor: YamlAnchor?,
        override val range: TextRange?,
        internal val canonical: ScalarValue,
    ) : YamlNode {
        /**
         * [YamlNodeKind.Scalar].
         *
         * @since 1.0.0
         */
        override val kind: YamlNodeKind get() = YamlNodeKind.Scalar

        /**
         * 0, since a scalar holds no node.
         *
         * @since 1.0.0
         */
        override val depth: Int get() = 0

        internal val identity: Int get() = super.hashCode()

        /**
         * Returns whether [other] is a scalar with an equal tag and an equal value.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal scalar, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean {
            val isSame = other is Scalar && tag == other.tag && canonical == other.canonical
            return this === other || isSame
        }

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code of the tag and the value.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * tag.hashCode() + canonical.hashCode()

        /**
         * Returns the tag and the text of the scalar, quoted and escaped through [SafeText] and shortened to a bounded
         * length, so the result is safe to log.
         *
         * @return a text such as `Scalar(<tag:yaml.org,2002:str>, "text")`.
         * @since 1.0.0
         */
        override fun toString(): String = "Scalar($tag, ${SafeText.quoted(value)})"
    }

    /**
     * A sequence: an ordered list of nodes.
     *
     * Two sequences are equal when their tags are equal and they hold equal items in the same order.
     *
     * @param items the items in order; the sequence keeps a copy.
     * @property tag the tag of the sequence, [YamlTag.SEQUENCE] unless the source or the code gives another.
     * @property style the style the sequence is written in.
     * @property anchor the anchor of the sequence, or `null` without one.
     * @property range the place of the sequence in its source, or `null`.
     * @throws IllegalArgumentException if the sequence would nest deeper than [YamlLimits.HARD_DEPTH].
     * @since 1.0.0
     */
    public class Sequence internal constructor(
        items: List<YamlNode>,
        override val tag: YamlTag,
        public val style: YamlCollectionStyle,
        override val anchor: YamlAnchor?,
        override val range: TextRange?,
    ) : YamlNode {
        /**
         * The items of the sequence, in order.
         *
         * @since 1.0.0
         */
        public val items: List<YamlNode> = buildList { addAll(items) }

        /**
         * One more than the deepest item, at most [YamlLimits.HARD_DEPTH].
         *
         * @since 1.0.0
         */
        override val depth: Int = NodeDepth.ofItems(this.items)

        private val hash: Int =
            this.items.fold(tag.hashCode()) { combined, item -> HASH_FACTOR * combined + item.hashCode() }

        /**
         * [YamlNodeKind.Sequence].
         *
         * @since 1.0.0
         */
        override val kind: YamlNodeKind get() = YamlNodeKind.Sequence

        internal val identity: Int get() = super.hashCode()

        /**
         * Returns whether [other] is a sequence with an equal tag and equal items in the same order.
         *
         * The comparison descends at most [depth] levels and compares two shared nodes once, so it stays bounded for
         * graphs with aliases.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal sequence, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is YamlNode && NodeEquality().isSame(first = this, second = other)

        /**
         * Returns a hash code consistent with [equals], computed when the sequence was built.
         *
         * @return the hash code of the tag and the items.
         * @since 1.0.0
         */
        override fun hashCode(): Int = hash

        /**
         * Returns the tag and the number of items, which is bounded and shows no content.
         *
         * @return a text such as `Sequence(<tag:yaml.org,2002:seq>, 2 items)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Sequence($tag, ${items.size} items)"
    }

    /**
     * A mapping: entries of unique keys and their values, in the order of the source.
     *
     * No two keys of a mapping are equal, so [get] is unambiguous: [yamlMapping], [YamlMappingBuilder] and
     * [withEntries] refuse a repeated key, and the composer keeps only one entry of a repeated key, as
     * [com.davils.kaml.core.YamlDuplicateKeys] demands. Two mappings are equal when their tags are equal and they hold
     * equal keys with equal values, in any order.
     *
     * @param entries the entries in order; the mapping keeps a copy with the first entry of every key.
     * @property tag the tag of the mapping, [YamlTag.MAPPING] unless the source or the code gives another.
     * @property style the style the mapping is written in.
     * @property anchor the anchor of the mapping, or `null` without one.
     * @property range the place of the mapping in its source, or `null`.
     * @throws IllegalArgumentException if the mapping would nest deeper than [YamlLimits.HARD_DEPTH].
     * @since 1.0.0
     */
    public class Mapping internal constructor(
        entries: List<YamlEntry>,
        override val tag: YamlTag,
        public val style: YamlCollectionStyle,
        override val anchor: YamlAnchor?,
        override val range: TextRange?,
    ) : YamlNode {
        /**
         * The entries of the mapping, in order, each with a key no other entry has.
         *
         * @since 1.0.0
         */
        public val entries: List<YamlEntry> = UniqueKeys.firstOf(entries)

        /**
         * One more than the deepest key or value, at most [YamlLimits.HARD_DEPTH].
         *
         * @since 1.0.0
         */
        override val depth: Int = NodeDepth.ofEntries(this.entries)

        private val hash: Int = HASH_FACTOR * tag.hashCode() + this.entries.sumOf { entry -> entry.hashCode() }

        private val byKeyHash: Map<Int, List<YamlEntry>> by lazy { this.entries.groupBy { it.key.hashCode() } }

        /**
         * [YamlNodeKind.Mapping].
         *
         * @since 1.0.0
         */
        override val kind: YamlNodeKind get() = YamlNodeKind.Mapping

        internal val identity: Int get() = super.hashCode()

        /**
         * The keys of the mapping, in order.
         *
         * @since 1.0.0
         */
        public val keys: List<YamlNode> get() = buildList { entries.forEach { entry -> add(entry.key) } }

        /**
         * Returns the value of the entry whose key equals [key], compared as nodes are compared, so `16` finds the key
         * `0x10`.
         *
         * @param key the key to look for.
         * @return the value of [key], or `null` if the mapping has no such key.
         * @since 1.0.0
         */
        public operator fun get(key: YamlNode): YamlNode? {
            val entry = entriesWithKeyHash(key.hashCode()).firstOrNull { candidate -> candidate.key == key }
            return entry?.value
        }

        internal fun entriesWithKeyHash(hash: Int): List<YamlEntry> = byKeyHash[hash].orEmpty()

        /**
         * Returns whether [other] is a mapping with an equal tag and equal keys with equal values, in any order.
         *
         * The comparison descends at most [depth] levels and compares two shared nodes once, so it stays bounded for
         * graphs with aliases.
         *
         * @param other the object to compare with, which may be `null`.
         * @return `true` if [other] is an equal mapping, `false` otherwise.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is YamlNode && NodeEquality().isSame(first = this, second = other)

        /**
         * Returns a hash code consistent with [equals], computed when the mapping was built.
         *
         * @return the hash code of the tag and the entries, independent of their order.
         * @since 1.0.0
         */
        override fun hashCode(): Int = hash

        /**
         * Returns the tag and the number of entries, which is bounded and shows no content.
         *
         * @return a text such as `Mapping(<tag:yaml.org,2002:map>, 2 entries)`.
         * @since 1.0.0
         */
        override fun toString(): String = "Mapping($tag, ${entries.size} entries)"
    }
}
