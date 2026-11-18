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

package com.davils.kaml.core.compose

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.event.YamlEvent
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange

internal class AnchorTable(
    private val budget: ComposeBudget,
    private val report: (YamlDiagnosticCode, TextRange?, String) -> Unit,
) {
    private val declared = HashMap<String, Anchored>()
    private val opened = HashMap<String, ArrayDeque<Int>>()
    private var counter = 0

    fun reset() {
        declared.clear()
        opened.clear()
    }

    fun next(): Int = counter++

    fun open(name: String) {
        val stack = opened.getOrPut(name) { ArrayDeque() }
        stack.addLast(next())
    }

    fun close(name: String?): Int {
        if (name == null) return next()
        return opened.getValue(name).removeLast()
    }

    fun declare(name: String, anchored: Anchored) {
        declared[name] = anchored
        budget.anchors(declared.size, anchored.node.range)
    }

    fun resolve(alias: YamlEvent.Alias): Anchored? {
        val name = alias.anchor.name
        val found = declared[name]
        val openOrder = opened[name]?.lastOrNull() ?: -1
        val declaredOrder = found?.order ?: -1
        val isRecursive = openOrder > declaredOrder
        if (isRecursive) {
            report(
                YamlDiagnosticCode.RecursiveAlias,
                alias.range,
                "The alias ${SafeText.quoted(name)} refers to a node that contains it",
            )
        }
        if (found == null && !isRecursive) {
            report(
                YamlDiagnosticCode.UndefinedAlias,
                alias.range,
                "The alias ${SafeText.quoted(name)} refers to no anchor declared before it",
            )
        }
        return found.takeIf { !isRecursive }
    }
}
