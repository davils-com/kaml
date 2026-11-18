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

package com.davils.kaml.core.parser

import com.davils.kaml.core.syntax.YamlSyntaxKind

internal enum class FlowShape(
    val closer: Char,
    val startKind: YamlSyntaxKind,
    val endKind: YamlSyntaxKind,
    val entryKind: YamlSyntaxKind,
) {
    SEQUENCE(
        closer = ']',
        startKind = YamlSyntaxKind.FLOW_SEQUENCE_START,
        endKind = YamlSyntaxKind.FLOW_SEQUENCE_END,
        entryKind = YamlSyntaxKind.FLOW_PAIR,
    ),
    MAPPING(
        closer = '}',
        startKind = YamlSyntaxKind.FLOW_MAPPING_START,
        endKind = YamlSyntaxKind.FLOW_MAPPING_END,
        entryKind = YamlSyntaxKind.FLOW_MAPPING_ENTRY,
    ),
    ;

    val isMapping: Boolean get() = this == MAPPING

    companion object {
        fun of(opener: Char): FlowShape = MAPPING.takeIf { opener == '{' } ?: SEQUENCE
    }
}
