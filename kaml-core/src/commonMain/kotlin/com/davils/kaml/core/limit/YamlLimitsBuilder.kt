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
    }
}
