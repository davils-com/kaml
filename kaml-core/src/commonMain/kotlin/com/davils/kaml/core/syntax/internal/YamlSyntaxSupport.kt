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

package com.davils.kaml.core.syntax.internal

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.compose.BlockScalarValues
import com.davils.kaml.core.compose.FlowScalarValues
import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.syntax.YamlSyntaxKind

internal object YamlSyntaxSupport {
    private val flowValues: Map<YamlSyntaxKind, (String) -> String> = mapOf(
        YamlSyntaxKind.PLAIN_SCALAR to FlowScalarValues::plain,
        YamlSyntaxKind.SINGLE_QUOTED_SCALAR to FlowScalarValues::singleQuoted,
        YamlSyntaxKind.DOUBLE_QUOTED_SCALAR to FlowScalarValues::doubleQuoted,
    )

    fun parse(text: String, yaml: Yaml): YamlSyntaxParse {
        val reading = yaml.pipeline.readText(text)
        return YamlSyntaxParse(reading.root, reading.diagnostics)
    }

    fun compose(yaml: Yaml, text: String, root: GreenNode, diagnostics: List<YamlDiagnostic>): YamlParseResult =
        yaml.pipeline.composeTree(text, root, diagnostics)

    fun flowScalarValue(kind: YamlSyntaxKind, text: String): String {
        val value = flowValues[kind] ?: return text
        return value(text)
    }

    fun blockScalarValue(header: String, content: String, parentIndent: Int): String =
        BlockScalarValues.value(header, content, parentIndent)
}
