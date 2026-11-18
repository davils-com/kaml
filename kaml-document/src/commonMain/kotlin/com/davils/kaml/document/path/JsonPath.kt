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

package com.davils.kaml.document.path

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.path.evaluate.PathEvaluator
import com.davils.kaml.document.path.parse.PathParsing
import com.davils.kaml.document.path.parse.PathReading
import com.davils.kaml.document.path.query.PathQuery
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A JSONPath query as RFC 9535 defines it, parsed and ready to select nodes of a YAML node graph.
 *
 * A query works on the JSON view of a graph: a mapping is an object whose members are named by the text of their
 * keys, a sequence is an array, and a scalar is a string, a number, a boolean or null by its tag. When a mapping
 * holds a key more than once, which only a graph built in code can, a name selects its first occurrence, as a
 * [com.davils.kaml.document.pointer.JsonPointer] does. The functions `length`, `count`, `match`, `search` and `value`
 * are supported; `match` and `search` run their I-Regexp (RFC 9485) on a linear-time engine of kaml-document, and
 * input never reaches [Regex].
 *
 * Every query is bounded by the [limits] it was parsed with: the expression by
 * [YamlLimits.maximumQueryLength] and [YamlLimits.maximumQueryNesting], the numbers in it by
 * [YamlLimits.maximumNumberLength], and every evaluation by [YamlLimits.maximumQuerySteps],
 * [YamlLimits.maximumQueryResults] and [YamlLimits.maximumDepth]; a pattern of `match` and `search` by
 * [YamlLimits.maximumRegexpLength] and [YamlLimits.maximumRegexpSize], beyond which it is not a valid I-Regexp, and
 * so is a pattern whose compiler would emit more than twice as many nodes as that size, such as one that repeats empty
 * groups. The descendant segment `..` walks the graph iteratively. A pattern is compiled at most once per evaluation
 * and never cached beyond it.
 *
 * A query is immutable and safe to share between threads; every evaluation keeps its own state.
 *
 * @property expression the text of the query exactly as it was parsed.
 * @property limits the limits the query was parsed with and evaluates under.
 * @since 1.0.0
 */
public class JsonPath private constructor(
    public val expression: String,
    public val limits: YamlLimits,
    private val syntax: PathQuery,
) {
    /**
     * Selects the nodes of [node] this query names, with the location of each.
     *
     * The root `$` of the query is [node]. The nodes come in the order RFC 9535 defines; a node may come more than
     * once, as the RFC allows.
     *
     * @param node the root of the graph to query.
     * @return the selected nodes, which cannot be changed; empty if nothing matches.
     * @throws YamlDocumentLimitException if the evaluation needs more steps than [YamlLimits.maximumQuerySteps], a
     * segment selects more nodes than [YamlLimits.maximumQueryResults] or a comparison descends deeper than
     * [YamlLimits.maximumDepth]. A query never returns a part of its result.
     * @since 1.0.0
     */
    public fun query(node: YamlNode): List<JsonPathMatch> {
        val matches = PathEvaluator(node, limits).evaluate(syntax, node)
        return buildList { matches.forEach { match -> add(JsonPathMatch(match.pointer, match.node)) } }
    }

    /**
     * Returns whether [other] is a query with the same expression and limits.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is a [JsonPath] with an equal [expression] and equal [limits].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is JsonPath && expression == other.expression && limits == other.limits

    /**
     * Returns a hash code of the expression of this query.
     *
     * @return the hash code of [expression].
     * @since 1.0.0
     */
    override fun hashCode(): Int = expression.hashCode()

    /**
     * Returns the [expression] escaped and shortened through [SafeText], since it may come from an attacker.
     *
     * @return the query as it is safe to show.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = SafeText.text(expression)

    /**
     * Parses JSONPath queries.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Parses [expression] as a JSONPath query of RFC 9535.
         *
         * @param expression the query, such as `$.store.book[?@.price < 10].title`.
         * @param limits the limits the query is parsed with and evaluates under; [YamlLimits.Untrusted] by default.
         * @return the query.
         * @throws IllegalArgumentException if [expression] is not a well-formed and well-typed query, is longer than
         * [YamlLimits.maximumQueryLength], is nested deeper than [YamlLimits.maximumQueryNesting] or holds a number
         * that is longer than [YamlLimits.maximumNumberLength] or not finite. The message shows the expression
         * escaped and shortened, the reason and the offset.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        public fun parse(expression: String, limits: YamlLimits = YamlLimits.Untrusted): JsonPath =
            when (val reading = PathParsing.read(expression, limits)) {
                is PathReading.Read -> JsonPath(expression, limits, reading.query)

                is PathReading.Refused -> throw IllegalArgumentException(
                    "${SafeText.quoted(expression)} is not a JSONPath query: ${reading.reason} at offset " +
                        "${reading.offset}",
                )
            }

        /**
         * Parses [expression] as a JSONPath query of RFC 9535 and never throws.
         *
         * @param expression the query, such as `$..author`.
         * @param limits the limits the query is parsed with and evaluates under; [YamlLimits.Untrusted] by default.
         * @return the query, or `null` if [expression] is not one or exceeds a limit, as [parse] describes.
         * @since 1.0.0
         */
        public fun parseOrNull(expression: String, limits: YamlLimits = YamlLimits.Untrusted): JsonPath? =
            when (val reading = PathParsing.read(expression, limits)) {
                is PathReading.Read -> JsonPath(expression, limits, reading.query)
                is PathReading.Refused -> null
            }
    }
}
