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
package com.davils.kaml.core

import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.event.YamlEvent
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.detached
import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val RELAXED_DEPTH: Int = YamlLimits.HARD_DEPTH
private const val UNTRUSTED_DEPTH: Int = 256
private const val ALIASED_DEPTH: Int = 100

private val start: TextRange = TextRange.at(0)

private val nestings: Map<String, (Int) -> String> = mapOf(
    "flow sequences" to { levels -> "[".repeat(levels) + "]".repeat(levels) },
    "flow mappings" to { levels -> "{a: ".repeat(levels) + "b" + "}".repeat(levels) },
    "block sequences" to { levels -> "- ".repeat(levels) + "a" },
    "block mappings" to { levels -> List(levels) { level -> " ".repeat(level) + "a:" }.joinToString("\n") + " b" },
)

private fun aliased(name: String, content: String): String =
    "$name: &$name " + "[".repeat(ALIASED_DEPTH) + content + "]".repeat(ALIASED_DEPTH)

private fun scalar(value: String): YamlEvent =
    YamlEvent.Scalar(range = start, anchor = null, tag = null, value = value, style = YamlScalarStyle.Plain)

private fun nestedSequences(levels: Int): List<YamlEvent> = buildList {
    add(YamlEvent.StreamStart(start))
    add(YamlEvent.DocumentStart(range = start, isExplicit = false, version = null, tagDirectives = emptyList()))
    repeat(levels) {
        add(YamlEvent.SequenceStart(range = start, anchor = null, tag = null, style = YamlCollectionStyle.Flow))
    }
    add(scalar("leaf"))
    repeat(levels) { add(YamlEvent.SequenceEnd(start)) }
    add(YamlEvent.DocumentEnd(range = start, isExplicit = false))
    add(YamlEvent.StreamEnd(start))
}

private fun nestedMappings(levels: Int): List<YamlEvent> = buildList {
    add(YamlEvent.StreamStart(start))
    add(YamlEvent.DocumentStart(range = start, isExplicit = false, version = null, tagDirectives = emptyList()))
    repeat(levels) {
        add(YamlEvent.MappingStart(range = start, anchor = null, tag = null, style = YamlCollectionStyle.Block))
        add(scalar("key"))
    }
    add(scalar("leaf"))
    repeat(levels) { add(YamlEvent.MappingEnd(start)) }
    add(YamlEvent.DocumentEnd(range = start, isExplicit = false))
    add(YamlEvent.StreamEnd(start))
}

private fun YamlNode.shouldWalkSafely() {
    val twin = detached(YamlLimits.Relaxed)

    twin shouldBe this
    twin.hashCode() shouldBe hashCode()
    toString().isNotEmpty() shouldBe true
}

private fun YamlParseResult.shouldComposeOrRefuse(levels: Int) {
    when (this) {
        is YamlParseResult.Parsed -> {
            val root = stream.documents.single().root

            root.depth shouldBe levels
            root.shouldWalkSafely()
        }

        is YamlParseResult.Failed -> {
            diagnostics.map { it.code } shouldContain YamlDiagnosticCode.TooDeep
        }
    }
}

class YamlDepthLimitTest : FunSpec({
    val relaxed = yaml { limits = YamlLimits.Relaxed }

    context("a document composed to the relaxed depth") {
        listOf(RELAXED_DEPTH - 1, RELAXED_DEPTH, RELAXED_DEPTH + 1).forEach { levels ->
            test("of $levels sequences composes or is refused as too deep") {
                relaxed.pipeline.compose(nestedSequences(levels), emptyList()).shouldComposeOrRefuse(levels)
            }

            test("of $levels mappings composes or is refused as too deep") {
                relaxed.pipeline.compose(nestedMappings(levels), emptyList()).shouldComposeOrRefuse(levels)
            }
        }

        test("composes just below the limit") {
            val levels = RELAXED_DEPTH - 2
            val result = relaxed.pipeline.compose(nestedSequences(levels), emptyList())
            val parsed = result.shouldBeInstanceOf<YamlParseResult.Parsed>()
            val root = parsed.stream.single?.root

            root?.depth shouldBe levels
        }

        test("is refused as too deep at the limit") {
            val result = relaxed.pipeline.compose(nestedSequences(RELAXED_DEPTH), emptyList())

            result.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.TooDeep
        }
    }

    context("a document parsed to the default depth") {
        nestings.forEach { (name, nested) ->
            listOf(UNTRUSTED_DEPTH - 1, UNTRUSTED_DEPTH, UNTRUSTED_DEPTH + 1).forEach { levels ->
                test("of $levels $name composes or is refused as too deep") {
                    val result = Yaml.Default.parse(nested(levels))
                    val codes = result.diagnostics.map { it.code }
                    val root = (result as? YamlParseResult.Parsed)?.stream?.single?.root

                    (result is YamlParseResult.Parsed || YamlDiagnosticCode.TooDeep in codes) shouldBe true
                    root?.shouldWalkSafely()
                }
            }
        }
    }

    context("aliases that nest a node deeper") {
        val first = aliased(name = "a", content = "leaf")
        val second = aliased(name = "b", content = "*a")
        val third = aliased(name = "c", content = "*b")

        test("compose while the graph stays within the depth") {
            val result = Yaml.Default.parse(listOf(first, second).joinToString("\n"))
            val parsed = result.shouldBeInstanceOf<YamlParseResult.Parsed>()
            val root = parsed.stream.single?.root

            root?.depth shouldBe ALIASED_DEPTH + ALIASED_DEPTH + 1
        }

        test("are refused as too deep instead of building a deeper node") {
            val result = Yaml.Default.parse(listOf(first, second, third).joinToString("\n"))

            result.diagnostics.map { it.code } shouldContain YamlDiagnosticCode.TooDeep
        }
    }
})
