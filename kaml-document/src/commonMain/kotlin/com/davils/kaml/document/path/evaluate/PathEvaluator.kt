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

package com.davils.kaml.document.path.evaluate

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.internal.JsonEquality
import com.davils.kaml.document.internal.WorkBudget
import com.davils.kaml.document.path.function.FunctionContext
import com.davils.kaml.document.path.query.PathQuery
import com.davils.kaml.document.path.query.PathSegment
import com.davils.kaml.document.path.query.PathSelector
import com.davils.kaml.document.pointer.JsonPointer

internal class PathEvaluator(private val root: YamlNode, private val limits: YamlLimits) {
    private val budget = WorkBudget(limits.maximumQuerySteps, "maximumQuerySteps")
    private val children = Children(budget)
    private val descendants = Descendants(children)
    private val filters = FilterEvaluator(
        paths = this,
        functions = FunctionContext(limits, budget),
        comparison = JsonComparison(JsonEquality(budget, limits.maximumDepth)),
    )

    fun evaluate(query: PathQuery, current: YamlNode): List<PathMatch> {
        budget.charge()
        var matches = listOf(PathMatch(null, current.takeIf { query.isRelative } ?: root))
        query.segments.forEach { segment -> matches = applied(segment, matches) }
        return matches
    }

    fun pointerOf(match: PathMatch): JsonPointer {
        budget.charge(match.depth.toLong())
        return match.pointer
    }

    private fun applied(segment: PathSegment, inputs: List<PathMatch>): List<PathMatch> {
        val results = Results(limits.maximumQueryResults)
        inputs.forEach { input -> targets(segment, input) { target -> selected(segment, target, results) } }
        return results.all
    }

    private fun targets(segment: PathSegment, input: PathMatch, visit: (PathMatch) -> Unit) {
        if (segment.isDescendant) return descendants.visit(input, visit)
        budget.charge()
        visit(input)
    }

    private fun selected(segment: PathSegment, target: PathMatch, results: Results) {
        segment.selectors.forEach { selector -> selectedBy(selector, target, results) }
    }

    private fun selectedBy(selector: PathSelector, match: PathMatch, results: Results) {
        when (selector) {
            is PathSelector.Name -> children.named(match, selector.name)?.let(results::add)
            is PathSelector.Wildcard -> children.of(match).forEach(results::add)
            is PathSelector.Index -> children.indexed(match, selector.index)?.let(results::add)
            is PathSelector.Slice -> children.sliced(match, selector, results::add)
            is PathSelector.Filter -> filtered(selector, match, results)
        }
    }

    private fun filtered(selector: PathSelector.Filter, match: PathMatch, results: Results) {
        children.of(match).forEach { child ->
            if (filters.test(selector.expression, child.node)) results.add(child)
        }
    }
}
