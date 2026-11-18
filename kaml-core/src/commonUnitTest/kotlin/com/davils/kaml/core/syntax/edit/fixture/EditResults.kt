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

package com.davils.kaml.core.syntax.edit.fixture

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.syntax.YamlSyntaxTree
import com.davils.kaml.core.syntax.edit.YamlEditRefusal
import com.davils.kaml.core.syntax.edit.YamlEditResult
import com.davils.kaml.core.syntax.edit.YamlEditor
import com.davils.kore.core.text.edited
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

internal fun edited(
    text: String,
    tree: YamlSyntaxTree = YamlSyntaxTree.parse(text),
    operation: YamlEditor.() -> YamlEditResult,
): String {
    val editor = YamlEditor(tree)
    val result = editor.operation().shouldBeInstanceOf<YamlEditResult.Edited>()
    text.edited(result.edits) shouldBe result.tree.text
    return result.tree.text
}

internal fun composed(text: String, operation: YamlEditor.() -> YamlEditResult): YamlNode {
    val editor = YamlEditor(YamlSyntaxTree.parse(text))
    val result = editor.operation().shouldBeInstanceOf<YamlEditResult.Edited>()
    val incremental = result.tree.compose().shouldBeInstanceOf<YamlParseResult.Parsed>()
    val reparsed = YamlSyntaxTree.parse(result.tree.text)
    val full = reparsed.compose().shouldBeInstanceOf<YamlParseResult.Parsed>()
    val root = rootOf(incremental)
    rootOf(full) shouldBe root
    return root
}

internal fun refusal(text: String, document: Int = 0, operation: YamlEditor.() -> YamlEditResult): YamlEditRefusal {
    val editor = YamlEditor(YamlSyntaxTree.parse(text), document)
    val result = editor.operation().shouldBeInstanceOf<YamlEditResult.Refused>()
    return result.reason
}

private fun rootOf(parsed: YamlParseResult.Parsed): YamlNode {
    val document = parsed.stream.documents.single()
    return document.root
}
