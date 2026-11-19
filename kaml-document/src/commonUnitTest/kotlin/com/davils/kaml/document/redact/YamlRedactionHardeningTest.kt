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

package com.davils.kaml.document.redact

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.document.fixture.lines
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.fixture.treeOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private const val SECRET: String = "hunter2"
private const val PASSWORD_PATH: String = "$.password"
private const val ANY_PASSWORD: String = "$..password"
private const val FEW_MAPPINGS: Int = 5_000
private const val MANY_MAPPINGS: Int = 40_000
private const val GROWTH: Int = 16
private val slack: Duration = 300.milliseconds

private fun equalMappings(count: Int): YamlNode =
    yamlSequence(List(count) { yamlMapping(listOf(YamlEntry(key = yamlScalar("a"), value = yamlScalar("b")))) })

private fun timed(work: () -> Unit): Duration {
    val mark = TimeSource.Monotonic.markNow()
    work()
    return mark.elapsedNow()
}

private fun scalarsOf(node: YamlNode): List<String> {
    val found = ArrayList<String>()
    val pending = ArrayDeque<YamlNode>()
    pending.addLast(node)
    while (pending.isNotEmpty()) {
        when (val current = pending.removeLast()) {
            is YamlNode.Scalar -> found.add(current.value)

            is YamlNode.Sequence -> current.items.forEach(pending::addLast)

            is YamlNode.Mapping -> current.entries.forEach { entry ->
                pending.addLast(entry.key)
                pending.addLast(entry.value)
            }
        }
    }
    return found
}

class YamlRedactionHardeningTest : FunSpec({
    context("keys of a graph") {
        test("are redacted where a path target is used as a key through an alias") {
            val redacted = yamlRedaction { path(PASSWORD_PATH) }.redact(nodeOf("{password: &p $SECRET, *p : x}"))

            scalarsOf(redacted) shouldNotContain SECRET
        }

        test("are redacted where the target of a key predicate is used as a key through an alias") {
            val redacted = yamlRedaction { key { it == "token" } }.redact(nodeOf("{token: &t $SECRET, *t : x}"))

            scalarsOf(redacted) shouldNotContain SECRET
        }

        test("are redacted where a target aliases an anchored key") {
            val redacted = yamlRedaction { path(PASSWORD_PATH) }.redact(nodeOf("{&p $SECRET : x, password: *p}"))

            scalarsOf(redacted) shouldNotContain SECRET
            redacted shouldBe nodeOf("{'***': x, password: '***'}")
        }

        test("refuse to make two keys of a mapping equal") {
            val colliding = nodeOf("{password: &p $SECRET, *p : x, '***': y}")

            shouldThrow<IllegalStateException> { yamlRedaction { path(PASSWORD_PATH) }.redact(colliding) }
        }
    }

    context("keys that are collections") {
        val inKey = lines("? {password: $SECRET}", ": x")

        test("are searched by a path in a graph") {
            scalarsOf(yamlRedaction { path(ANY_PASSWORD) }.redact(nodeOf(inKey))) shouldNotContain SECRET
        }

        test("are searched by a path in a syntax tree") {
            val redacted = yamlRedaction { path(ANY_PASSWORD) }.redact(treeOf(inKey))

            redacted.shouldBeInstanceOf<YamlRedactionResult.Redacted>().tree.text shouldBe
                lines("? {password: \"***\"}", ": x")
        }
    }

    context("graphs built in code") {
        test("are redacted in near-linear time, however many equal nodes without a range they hold") {
            val redaction = yamlRedaction { key { it == "password" } }
            val few = equalMappings(FEW_MAPPINGS)
            val many = equalMappings(MANY_MAPPINGS)
            val fewTime = timed { redaction.redact(few) }
            val manyTime = timed { redaction.redact(many) }

            (manyTime < fewTime * GROWTH + slack) shouldBe true
            redaction.redact(many) shouldBe many
        }
    }
})
