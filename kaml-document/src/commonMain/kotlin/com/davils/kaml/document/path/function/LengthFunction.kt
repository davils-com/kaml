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

package com.davils.kaml.document.path.function

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.document.internal.CodePoints

internal object LengthFunction : PathFunction {
    override val name: String = "length"

    override val parameters: List<ParameterType> = listOf(ParameterType.Value)

    override val result: ResultType = ResultType.Value

    override fun evaluate(arguments: List<ArgumentValue>, context: FunctionContext): ArgumentValue {
        val value = arguments.single() as ArgumentValue.Value
        val length = value.node?.let { node -> lengthOf(node, context) }
        return ArgumentValue.Value(length?.let(::yamlScalar))
    }

    private fun lengthOf(node: YamlNode, context: FunctionContext): Int? = when (node) {
        is YamlNode.Scalar -> node.value.takeIf { node.tag == YamlTag.STRING }?.let { text -> counted(text, context) }
        is YamlNode.Sequence -> node.items.size
        is YamlNode.Mapping -> node.entries.size
    }

    private fun counted(text: String, context: FunctionContext): Int {
        context.budget.charge(text.length.toLong())
        return CodePoints.count(text)
    }
}
