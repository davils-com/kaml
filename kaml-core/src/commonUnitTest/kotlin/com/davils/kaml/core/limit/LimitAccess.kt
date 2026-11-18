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

internal class LimitAccess(
    val name: String,
    val ceiling: Long,
    private val assign: YamlLimitsBuilder.(Long) -> Unit,
    private val read: (YamlLimits) -> Long,
) {
    fun assignTo(builder: YamlLimitsBuilder, value: Long) {
        builder.assign(value)
    }

    fun of(limits: YamlLimits): Long = read(limits)
}

internal val limitAccesses: List<LimitAccess> = listOf(
    LimitAccess(
        name = "maximumInputLength",
        ceiling = YamlLimits.HARD_INPUT_LENGTH.toLong(),
        assign = { value -> maximumInputLength = value.toInt() },
        read = { built -> built.maximumInputLength.toLong() },
    ),
    LimitAccess(
        name = "maximumDepth",
        ceiling = YamlLimits.HARD_DEPTH.toLong(),
        assign = { value -> maximumDepth = value.toInt() },
        read = { built -> built.maximumDepth.toLong() },
    ),
    LimitAccess(
        name = "maximumNodes",
        ceiling = YamlLimits.HARD_NODES,
        assign = { value -> maximumNodes = value },
        read = { built -> built.maximumNodes },
    ),
    LimitAccess(
        name = "maximumAliasExpansion",
        ceiling = YamlLimits.HARD_ALIAS_EXPANSION,
        assign = { value -> maximumAliasExpansion = value },
        read = { built -> built.maximumAliasExpansion },
    ),
    LimitAccess(
        name = "maximumAnchors",
        ceiling = YamlLimits.HARD_ANCHORS.toLong(),
        assign = { value -> maximumAnchors = value.toInt() },
        read = { built -> built.maximumAnchors.toLong() },
    ),
    LimitAccess(
        name = "maximumScalarLength",
        ceiling = YamlLimits.HARD_INPUT_LENGTH.toLong(),
        assign = { value -> maximumScalarLength = value.toInt() },
        read = { built -> built.maximumScalarLength.toLong() },
    ),
    LimitAccess(
        name = "maximumCollectionSize",
        ceiling = YamlLimits.HARD_COLLECTION_SIZE.toLong(),
        assign = { value -> maximumCollectionSize = value.toInt() },
        read = { built -> built.maximumCollectionSize.toLong() },
    ),
    LimitAccess(
        name = "maximumDocuments",
        ceiling = YamlLimits.HARD_DOCUMENTS.toLong(),
        assign = { value -> maximumDocuments = value.toInt() },
        read = { built -> built.maximumDocuments.toLong() },
    ),
    LimitAccess(
        name = "maximumNumberLength",
        ceiling = YamlLimits.HARD_NUMBER_LENGTH.toLong(),
        assign = { value -> maximumNumberLength = value.toInt() },
        read = { built -> built.maximumNumberLength.toLong() },
    ),
    LimitAccess(
        name = "maximumDiagnostics",
        ceiling = YamlLimits.HARD_DIAGNOSTICS.toLong(),
        assign = { value -> maximumDiagnostics = value.toInt() },
        read = { built -> built.maximumDiagnostics.toLong() },
    ),
    LimitAccess(
        name = "maximumOutputLength",
        ceiling = YamlLimits.HARD_INPUT_LENGTH.toLong(),
        assign = { value -> maximumOutputLength = value.toInt() },
        read = { built -> built.maximumOutputLength.toLong() },
    ),
)
