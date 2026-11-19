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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * The builder of [YamlLimits] behind [yamlLimits] and the `limits` block of [com.davils.kaml.core.yaml].
 *
 * Every property starts from the limits the block is based on, [YamlLimits.Untrusted] unless a base is given, and
 * means what the property of the same name of [YamlLimits] means. Building validates every value: one below 1 or
 * above its hard ceiling of [YamlLimits] makes the build throw a
 * [com.davils.kore.core.validation.report.ValidationException] whose report names every such limit.
 *
 * A builder is not thread-safe and belongs to the block it is given to; once the block has been built, setting a
 * property throws an [IllegalStateException].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class YamlLimitsBuilder internal constructor() : ValidatingBuilder<YamlLimits>() {
    override val subject: String get() = "yaml limits"

    /**
     * The value of [YamlLimits.maximumInputLength]; [DEFAULT_INPUT_LENGTH] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumInputLength: Int = DEFAULT_INPUT_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumDepth]; [DEFAULT_DEPTH] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumDepth: Int = DEFAULT_DEPTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumNodes]; [DEFAULT_NODES] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumNodes: Long = DEFAULT_NODES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumAliasExpansion]; [DEFAULT_ALIAS_EXPANSION] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumAliasExpansion: Long = DEFAULT_ALIAS_EXPANSION
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumAnchors]; [DEFAULT_ANCHORS] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumAnchors: Int = DEFAULT_ANCHORS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumScalarLength]; [DEFAULT_SCALAR_LENGTH] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumScalarLength: Int = DEFAULT_SCALAR_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumCollectionSize]; [DEFAULT_COLLECTION_SIZE] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumCollectionSize: Int = DEFAULT_COLLECTION_SIZE
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumDocuments]; [DEFAULT_DOCUMENTS] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumDocuments: Int = DEFAULT_DOCUMENTS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumNumberLength]; [DEFAULT_NUMBER_LENGTH] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumNumberLength: Int = DEFAULT_NUMBER_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumDiagnostics]; [DEFAULT_DIAGNOSTICS] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumDiagnostics: Int = DEFAULT_DIAGNOSTICS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of [YamlLimits.maximumOutputLength]; [DEFAULT_OUTPUT_LENGTH] unless the base says otherwise.
     *
     * @throws IllegalStateException if it is set after the block was built.
     * @since 1.0.0
     */
    public var maximumOutputLength: Int = DEFAULT_OUTPUT_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The most characters of a JSONPath expression that kaml-document parses.
     *
     * It must lie between 1 and [YamlLimits.HARD_QUERY_LENGTH]; [DEFAULT_QUERY_LENGTH] by default.
     *
     * @since 1.0.0
     */
    public var maximumQueryLength: Int = DEFAULT_QUERY_LENGTH

    /**
     * The deepest nesting of a JSONPath expression and of an I-Regexp pattern: parentheses, filters inside filters,
     * function arguments and the groups of a pattern.
     *
     * It must lie between 1 and [YamlLimits.HARD_QUERY_NESTING]; [DEFAULT_QUERY_NESTING] by default.
     *
     * @since 1.0.0
     */
    public var maximumQueryNesting: Int = DEFAULT_QUERY_NESTING

    /**
     * The most steps the evaluation of one JSONPath query may take, shared by its selectors, its filters, its
     * comparisons and the I-Regexp engine of `match` and `search`.
     *
     * It must lie between 1 and [YamlLimits.HARD_QUERY_STEPS]; [DEFAULT_QUERY_STEPS] by default.
     *
     * @since 1.0.0
     */
    public var maximumQuerySteps: Long = DEFAULT_QUERY_STEPS

    /**
     * The most nodes a JSONPath query may select, counted for every segment of the query.
     *
     * It must lie between 1 and [YamlLimits.HARD_QUERY_RESULTS]; [DEFAULT_QUERY_RESULTS] by default.
     *
     * @since 1.0.0
     */
    public var maximumQueryResults: Int = DEFAULT_QUERY_RESULTS

    /**
     * The most characters of an I-Regexp pattern that the JSONPath functions `match` and `search` compile.
     *
     * It must lie between 1 and [YamlLimits.HARD_REGEXP_LENGTH]; [DEFAULT_REGEXP_LENGTH] by default.
     *
     * @since 1.0.0
     */
    public var maximumRegexpLength: Int = DEFAULT_REGEXP_LENGTH

    /**
     * The most instructions the compiled program of an I-Regexp pattern may hold; repeats such as `a{1000}` count once
     * per copy.
     *
     * It must lie between 1 and [YamlLimits.HARD_REGEXP_SIZE]; [DEFAULT_REGEXP_SIZE] by default.
     *
     * @since 1.0.0
     */
    public var maximumRegexpSize: Int = DEFAULT_REGEXP_SIZE

    /**
     * The most operations of a JSON Patch, and the most changes a JSON Merge Patch may make to a syntax tree.
     *
     * It must lie between 1 and [YamlLimits.HARD_PATCH_OPERATIONS]; [DEFAULT_PATCH_OPERATIONS] by default.
     *
     * @since 1.0.0
     */
    public var maximumPatchOperations: Int = DEFAULT_PATCH_OPERATIONS

    /**
     * The most characters a pattern supplied by the caller, such as a value or comment pattern of a redaction, is
     * applied to at once.
     *
     * It must lie between 1 and [YamlLimits.HARD_PATTERN_INPUT_LENGTH]; [DEFAULT_PATTERN_INPUT_LENGTH] by default.
     *
     * @since 1.0.0
     */
    public var maximumPatternInputLength: Int = DEFAULT_PATTERN_INPUT_LENGTH

    /**
     * The most characters of the text of a JSON Pointer that kaml-document parses, such as the `path` and `from` of an
     * operation of a JSON Patch read from a document.
     *
     * It must lie between 1 and [YamlLimits.HARD_POINTER_LENGTH]; [DEFAULT_POINTER_LENGTH] by default.
     *
     * @since 1.0.0
     */
    public var maximumPointerLength: Int = DEFAULT_POINTER_LENGTH

    internal fun from(limits: YamlLimits) {
        maximumInputLength = limits.maximumInputLength
        maximumDepth = limits.maximumDepth
        maximumNodes = limits.maximumNodes
        maximumAliasExpansion = limits.maximumAliasExpansion
        maximumAnchors = limits.maximumAnchors
        maximumScalarLength = limits.maximumScalarLength
        maximumCollectionSize = limits.maximumCollectionSize
        maximumDocuments = limits.maximumDocuments
        maximumNumberLength = limits.maximumNumberLength
        maximumDiagnostics = limits.maximumDiagnostics
        maximumOutputLength = limits.maximumOutputLength
        maximumQueryLength = limits.maximumQueryLength
        maximumQueryNesting = limits.maximumQueryNesting
        maximumQuerySteps = limits.maximumQuerySteps
        maximumQueryResults = limits.maximumQueryResults
        maximumRegexpLength = limits.maximumRegexpLength
        maximumRegexpSize = limits.maximumRegexpSize
        maximumPatchOperations = limits.maximumPatchOperations
        maximumPatternInputLength = limits.maximumPatternInputLength
        maximumPointerLength = limits.maximumPointerLength
    }

    override fun assemble(): YamlLimits = YamlLimits(this)

    /**
     * The defaults of every limit, which make up [YamlLimits.Untrusted].
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default of [maximumInputLength]: 64 Mi (67,108,864) UTF-16 code units.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_INPUT_LENGTH: Int = 64 * 1024 * 1024

        /**
         * The default of [maximumDepth]: 256 levels.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DEPTH: Int = 256

        /**
         * The default of [maximumNodes]: 5,000,000 nodes.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_NODES: Long = 5_000_000L

        /**
         * The default of [maximumAliasExpansion]: 100,000 nodes.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ALIAS_EXPANSION: Long = 100_000L

        /**
         * The default of [maximumAnchors]: 10,000 anchors.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ANCHORS: Int = 10_000

        /**
         * The default of [maximumScalarLength]: 16 Mi (16,777,216) UTF-16 code units.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_SCALAR_LENGTH: Int = 16 * 1024 * 1024

        /**
         * The default of [maximumCollectionSize]: 1,000,000 items or entries.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_COLLECTION_SIZE: Int = 1_000_000

        /**
         * The default of [maximumDocuments]: 10,000 documents.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DOCUMENTS: Int = 10_000

        /**
         * The default of [maximumNumberLength]: 1,000 characters.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_NUMBER_LENGTH: Int = 1_000

        /**
         * The default of [maximumDiagnostics]: 1,000 diagnostics.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DIAGNOSTICS: Int = 1_000

        /**
         * The default of [maximumOutputLength]: 64 Mi (67,108,864) UTF-16 code units.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_OUTPUT_LENGTH: Int = 64 * 1024 * 1024

        /**
         * The default of [maximumQueryLength].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_QUERY_LENGTH: Int = 64 * 1024

        /**
         * The default of [maximumQueryNesting].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_QUERY_NESTING: Int = 64

        /**
         * The default of [maximumQuerySteps].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_QUERY_STEPS: Long = 10_000_000L

        /**
         * The default of [maximumQueryResults].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_QUERY_RESULTS: Int = 1_000_000

        /**
         * The default of [maximumRegexpLength].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_REGEXP_LENGTH: Int = 4_096

        /**
         * The default of [maximumRegexpSize].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_REGEXP_SIZE: Int = 10_000

        /**
         * The default of [maximumPatchOperations].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_PATCH_OPERATIONS: Int = 10_000

        /**
         * The default of [maximumPatternInputLength].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_PATTERN_INPUT_LENGTH: Int = 64 * 1024

        /**
         * The default of [maximumPointerLength].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_POINTER_LENGTH: Int = 64 * 1024
    }
}
