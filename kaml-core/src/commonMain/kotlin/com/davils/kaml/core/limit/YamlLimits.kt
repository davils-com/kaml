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

package com.davils.kaml.core.limit

import com.davils.kaml.core.YamlViolationCodes
import com.davils.kore.core.Limits
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty0

/**
 * The limits that bound the work and the memory kaml spends on one source or one emitted text.
 *
 * Every input of kaml may come from an attacker, so every entry point checks its input against these limits before it
 * does work that grows with the input, and the budgets of nodes, aliases, anchors and diagnostics are charged while
 * events and nodes are built, so an oversized document stops at the first excess instead of after it is in memory.
 * What exceeds a limit is refused with the diagnostic or the exception each limit names; nothing is truncated
 * silently.
 *
 * Every limit is at least 1 and at most a hard ceiling, a `HARD_*` constant of the companion that no preset and no
 * configuration exceeds. [Untrusted] is the default and fits documents from unknown sources; [Relaxed] raises every
 * limit for trusted, large documents and is still finite. Other limits are built with [yamlLimits] or the `limits`
 * block of [com.davils.kaml.core.yaml], which validate them and throw a
 * [com.davils.kore.core.validation.report.ValidationException] for a value below 1 or above its ceiling.
 *
 * The limits are an immutable value and safe to share between threads; two limits are equal when every limit is
 * equal.
 *
 * @since 1.0.0
 */
public class YamlLimits internal constructor(builder: YamlLimitsBuilder) : Validatable {
    /**
     * The most UTF-16 code units the text of a source may have: 64 Mi (67,108,864) by default, at most
     * [HARD_INPUT_LENGTH].
     *
     * A longer source is refused with an [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.InputTooLong] diagnostic
     * before it is parsed, and a source of bytes is refused before it is decoded when it holds more than four bytes per
     * allowed code unit. The syntax tree and its incremental reparse check the same limit.
     *
     * @since 1.0.0
     */
    public val maximumInputLength: Int = builder.maximumInputLength

    /**
     * The most levels collections may nest in a document: 256 by default, at most [HARD_DEPTH].
     *
     * Deeper nesting stops the parser and the composer with a
     * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.TooDeep] diagnostic, and no deeper node is built. Whatever
     * the limit, a [com.davils.kaml.core.node.YamlNode] never nests deeper than [HARD_DEPTH], so every recursive walk
     * of a node is bounded.
     *
     * @since 1.0.0
     */
    public val maximumDepth: Int = builder.maximumDepth

    /**
     * The most nodes a stream may have, counted as they are built: 5,000,000 by default, at most [HARD_NODES].
     *
     * The node beyond the limit stops the work with a [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.TooManyNodes]
     * diagnostic. [com.davils.kaml.core.node.detached] charges the same limit with the nodes it creates and throws an
     * [IllegalArgumentException] beyond it.
     *
     * @since 1.0.0
     */
    public val maximumNodes: Long = builder.maximumNodes

    /**
     * The most nodes all aliases of a document may stand for together: 100,000 by default, at most
     * [HARD_ALIAS_EXPANSION].
     *
     * Every alias charges the number of nodes of the node it refers to, so a document that nests aliases to multiply
     * its size, such as a billion laughs, stops at this budget with a
     * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.TooManyAliasExpansions] diagnostic and never at the heap.
     *
     * @since 1.0.0
     */
    public val maximumAliasExpansion: Long = builder.maximumAliasExpansion

    /**
     * The most anchors a document may declare: 10,000 by default, at most [HARD_ANCHORS].
     *
     * The anchor beyond the limit stops the work with a
     * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.TooManyAnchors] diagnostic.
     *
     * @since 1.0.0
     */
    public val maximumAnchors: Int = builder.maximumAnchors

    /**
     * The most UTF-16 code units the value of one scalar may have: 16 Mi (16,777,216) by default, at most
     * [HARD_INPUT_LENGTH].
     *
     * A longer scalar stops the work with a [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.ScalarTooLong]
     * diagnostic.
     *
     * @since 1.0.0
     */
    public val maximumScalarLength: Int = builder.maximumScalarLength

    /**
     * The most items of a sequence or entries of a mapping: 1,000,000 by default, at most [HARD_COLLECTION_SIZE].
     *
     * A larger collection stops the work with a
     * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.CollectionTooLarge] diagnostic.
     *
     * @since 1.0.0
     */
    public val maximumCollectionSize: Int = builder.maximumCollectionSize

    /**
     * The most documents a stream may have: 10,000 by default, at most [HARD_DOCUMENTS].
     *
     * The document beyond the limit stops the work with a
     * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.TooManyDocuments] diagnostic.
     *
     * @since 1.0.0
     */
    public val maximumDocuments: Int = builder.maximumDocuments

    /**
     * The most characters a number may have to be converted to its value: 1,000 by default, at most
     * [HARD_NUMBER_LENGTH].
     *
     * A longer number is never converted, so its length cannot decide the time of the conversion; it is reported with a
     * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.NumberTooLong] diagnostic.
     *
     * @since 1.0.0
     */
    public val maximumNumberLength: Int = builder.maximumNumberLength

    /**
     * The most diagnostics a result may hold: 1,000 by default, at most [HARD_DIAGNOSTICS].
     *
     * The diagnostics beyond the limit are left out, and one
     * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.TooManyDiagnostics] diagnostic says so.
     *
     * @since 1.0.0
     */
    public val maximumDiagnostics: Int = builder.maximumDiagnostics

    /**
     * The most UTF-16 code units an emitted text may have: 64 Mi (67,108,864) by default, at most
     * [HARD_INPUT_LENGTH].
     *
     * The emitter stops with an [IllegalStateException] as soon as its text would grow beyond the limit, so an
     * oversized text is never returned in part.
     *
     * @since 1.0.0
     */
    public val maximumOutputLength: Int = builder.maximumOutputLength

    private val values: List<Long>
        get() = listOf(
            maximumInputLength.toLong(),
            maximumDepth.toLong(),
            maximumNodes,
            maximumAliasExpansion,
            maximumAnchors.toLong(),
            maximumScalarLength.toLong(),
            maximumCollectionSize.toLong(),
            maximumDocuments.toLong(),
            maximumNumberLength.toLong(),
            maximumDiagnostics.toLong(),
            maximumOutputLength.toLong(),
        )

    /**
     * Checks that every limit lies between 1 and its hard ceiling.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        bounded(
            property = ::maximumInputLength,
            value = maximumInputLength.toLong(),
            ceiling = HARD_INPUT_LENGTH.toLong(),
        )
        bounded(property = ::maximumDepth, value = maximumDepth.toLong(), ceiling = HARD_DEPTH.toLong())
        bounded(property = ::maximumNodes, value = maximumNodes, ceiling = HARD_NODES)
        bounded(property = ::maximumAliasExpansion, value = maximumAliasExpansion, ceiling = HARD_ALIAS_EXPANSION)
        bounded(property = ::maximumAnchors, value = maximumAnchors.toLong(), ceiling = HARD_ANCHORS.toLong())
        bounded(
            property = ::maximumScalarLength,
            value = maximumScalarLength.toLong(),
            ceiling = HARD_INPUT_LENGTH.toLong(),
        )
        bounded(
            property = ::maximumCollectionSize,
            value = maximumCollectionSize.toLong(),
            ceiling = HARD_COLLECTION_SIZE.toLong(),
        )
        bounded(property = ::maximumDocuments, value = maximumDocuments.toLong(), ceiling = HARD_DOCUMENTS.toLong())
        bounded(
            property = ::maximumNumberLength,
            value = maximumNumberLength.toLong(),
            ceiling = HARD_NUMBER_LENGTH.toLong(),
        )
        bounded(
            property = ::maximumDiagnostics,
            value = maximumDiagnostics.toLong(),
            ceiling = HARD_DIAGNOSTICS.toLong(),
        )
        bounded(
            property = ::maximumOutputLength,
            value = maximumOutputLength.toLong(),
            ceiling = HARD_INPUT_LENGTH.toLong(),
        )
    }

    /**
     * Returns whether [other] holds the same value for every limit.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is equal limits, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is YamlLimits && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of every limit.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns every limit by a short name, such as `YamlLimits(input=67108864, depth=256, …)`.
     *
     * @return the limits as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "YamlLimits(input=$maximumInputLength, depth=$maximumDepth, nodes=$maximumNodes, " +
            "aliasExpansion=$maximumAliasExpansion, anchors=$maximumAnchors, scalar=$maximumScalarLength, " +
            "collection=$maximumCollectionSize, documents=$maximumDocuments, number=$maximumNumberLength, " +
            "diagnostics=$maximumDiagnostics, output=$maximumOutputLength)"

    private fun ValidationScope.bounded(property: KProperty0<*>, value: Long, ceiling: Long) {
        if (value < 1) fail(property, "must be at least 1, but was $value", YamlViolationCodes.NOT_POSITIVE)
        if (value > ceiling) {
            fail(property, "must not exceed $ceiling, but was $value", YamlViolationCodes.ABOVE_HARD_LIMIT)
        }
    }

    /**
     * The hard ceilings of every limit and the presets [Untrusted] and [Relaxed].
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The ceiling of [maximumInputLength], [maximumScalarLength] and [maximumOutputLength]: 1 Gi (1,073,741,824)
         * UTF-16 code units.
         *
         * @since 1.0.0
         */
        public const val HARD_INPUT_LENGTH: Int = 1 shl 30

        /**
         * The ceiling of [maximumDepth] and the depth no [com.davils.kaml.core.node.YamlNode] may exceed, however it is
         * built: 1,000 levels.
         *
         * @since 1.0.0
         */
        public const val HARD_DEPTH: Int = 1_000

        /**
         * The ceiling of [maximumNodes]: 100,000,000 nodes.
         *
         * @since 1.0.0
         */
        public const val HARD_NODES: Long = 100_000_000L

        /**
         * The ceiling of [maximumAliasExpansion]: 100,000,000 nodes.
         *
         * @since 1.0.0
         */
        public const val HARD_ALIAS_EXPANSION: Long = 100_000_000L

        /**
         * The ceiling of [maximumAnchors]: 10,000,000 anchors.
         *
         * @since 1.0.0
         */
        public const val HARD_ANCHORS: Int = 10_000_000

        /**
         * The ceiling of [maximumCollectionSize]: 100,000,000 items or entries.
         *
         * @since 1.0.0
         */
        public const val HARD_COLLECTION_SIZE: Int = 100_000_000

        /**
         * The ceiling of [maximumDocuments]: 10,000,000 documents.
         *
         * @since 1.0.0
         */
        public const val HARD_DOCUMENTS: Int = 10_000_000

        /**
         * The ceiling of [maximumNumberLength]: the longest decimal text Kore converts,
         * [Limits.MAX_DECIMAL_TEXT_LENGTH] (1,024) characters.
         *
         * @since 1.0.0
         */
        public const val HARD_NUMBER_LENGTH: Int = Limits.MAX_DECIMAL_TEXT_LENGTH

        /**
         * The ceiling of [maximumDiagnostics]: 100,000 diagnostics.
         *
         * @since 1.0.0
         */
        public const val HARD_DIAGNOSTICS: Int = 100_000

        private const val RELAXED_INPUT_LENGTH: Int = 512 * 1024 * 1024
        private const val RELAXED_NODES: Long = 50_000_000L
        private const val RELAXED_ALIAS_EXPANSION: Long = 10_000_000L
        private const val RELAXED_ANCHORS: Int = 1_000_000
        private const val RELAXED_COLLECTION_SIZE: Int = 50_000_000
        private const val RELAXED_DOCUMENTS: Int = 1_000_000
        private const val RELAXED_DIAGNOSTICS: Int = 10_000

        /**
         * The default limits, for documents from sources that are not trusted: the `DEFAULT_*` values of
         * [YamlLimitsBuilder], such as 64 Mi code units of input, 256 levels, 5,000,000 nodes and 100,000 nodes of
         * alias expansion.
         *
         * @since 1.0.0
         */
        public val Untrusted: YamlLimits = YamlLimitsBuilder().build()

        /**
         * Raised limits for large documents from trusted sources: 512 Mi code units of input, of a scalar and of
         * output, [HARD_DEPTH] levels, 50,000,000 nodes, 10,000,000 nodes of alias expansion, 1,000,000 anchors,
         * 50,000,000 entries of a collection, 1,000,000 documents, [HARD_NUMBER_LENGTH] characters of a number and
         * 10,000 diagnostics.
         *
         * Every limit stays finite and below its hard ceiling, so even a trusted source cannot exhaust the memory
         * without bound.
         *
         * @since 1.0.0
         */
        public val Relaxed: YamlLimits = relaxed()

        private fun relaxed(): YamlLimits {
            val builder = YamlLimitsBuilder()
            builder.maximumInputLength = RELAXED_INPUT_LENGTH
            builder.maximumDepth = HARD_DEPTH
            builder.maximumNodes = RELAXED_NODES
            builder.maximumAliasExpansion = RELAXED_ALIAS_EXPANSION
            builder.maximumAnchors = RELAXED_ANCHORS
            builder.maximumScalarLength = RELAXED_INPUT_LENGTH
            builder.maximumCollectionSize = RELAXED_COLLECTION_SIZE
            builder.maximumDocuments = RELAXED_DOCUMENTS
            builder.maximumNumberLength = HARD_NUMBER_LENGTH
            builder.maximumDiagnostics = RELAXED_DIAGNOSTICS
            builder.maximumOutputLength = RELAXED_INPUT_LENGTH
            return builder.build()
        }
    }
}
