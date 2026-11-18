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

package com.davils.kaml.document.merge

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.yamlLimits
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.document.YamlDocumentLimitException
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val ITEMS: Int = 20_000

private fun item(name: Int, member: String): YamlNode = yamlMapping(
    listOf(
        YamlEntry(key = yamlScalar("name"), value = yamlScalar("n$name")),
        YamlEntry(key = yamlScalar(member), value = yamlScalar(1)),
    ),
)

class YamlMergerLimitsTest : FunSpec({
    context("keyed sequences") {
        test("merge through a hash index, however many items they hold") {
            val options = yamlMergeOptions { sequences = YamlSequenceMerge.ByKey("name") }
            val base = yamlSequence((0 until ITEMS).map { item(it, "a") })
            val overlay = yamlSequence((0 until ITEMS).reversed().map { item(it, "b") })
            val merged = YamlMerger(
                options,
                YamlLimits.Relaxed,
            ).merge(listOf(YamlLayer("x", base), YamlLayer("y", overlay)))
            val items = (merged.node as YamlNode.Sequence).items

            items.size shouldBe ITEMS
            items.last() shouldBe nodeOf("{name: n${ITEMS - 1}, a: 1, b: 1}")
        }
    }

    context("keyed items") {
        test("without a text identity are appended and deletions without one are dropped") {
            val options = yamlMergeOptions { sequences = YamlSequenceMerge.ByKey("name") }
            val base = YamlLayer("a", nodeOf("[{name: {x: 1}}, {other: 1}]"))
            val overlay = YamlLayer("b", nodeOf("[!delete x, {name: n}, {name: {x: 1}}]"))
            val merged = YamlMerger(options).merge(listOf(base, overlay))

            merged.node shouldBe nodeOf("[{name: {x: 1}}, {other: 1}, {name: n}, {name: {x: 1}}]")
            YamlLayer("a", nodeOf("[1]")) shouldNotBe YamlLayer("a", nodeOf("[2]"))
        }
    }

    context("keys") {
        test("merge into an equal key written in another form") {
            val merged = YamlMerger().merge(
                listOf(YamlLayer("a", nodeOf("{0x1: a}")), YamlLayer("b", nodeOf("{1: b}"))),
            )
            val entry = (merged.node as YamlNode.Mapping).entries.single()

            entry.key shouldBe nodeOf("0x1")
            entry.value shouldBe yamlScalar("b")
        }
    }

    context("limits") {
        test("count the nodes of every layer against the node budget") {
            val small = yamlLimits { maximumNodes = 4 }
            val layers = listOf(YamlLayer("a", nodeOf("{x: 1}")), YamlLayer("b", nodeOf("{y: 2}")))

            YamlMerger(limits = small).merge(layers).node shouldBe nodeOf("{x: 1, y: 2}")
            shouldThrow<YamlDocumentLimitException> {
                YamlMerger(limits = small).merge(layers + YamlLayer("c", nodeOf("{z: 3}")))
            }.limit shouldBe "maximumNodes"
        }

        test("refuse a layer deeper than the depth limit") {
            val shallow = yamlLimits { maximumDepth = 2 }
            val base = YamlLayer("a", nodeOf("{x: {y: 1}}"))

            YamlMerger(limits = shallow).merge(listOf(base, YamlLayer("b", nodeOf("{x: {y: 2}}")))).node shouldBe
                nodeOf("{x: {y: 2}}")
            shouldThrow<YamlDocumentLimitException> {
                YamlMerger(limits = shallow).merge(listOf(base, YamlLayer("b", nodeOf("{x: {y: {z: 1}}}"))))
            }.limit shouldBe "maximumDepth"
        }
    }

    context("descriptions") {
        test("show names escaped and never the content of a layer") {
            HostileFragments.all.forEach { hostile ->
                val layer = YamlLayer(hostile, nodeOf("{secret: value}"))

                unsafeCodePointsIn(layer.toString()).shouldBeEmpty()
                unsafeCodePointsIn(YamlSequenceMerge.ByKey(hostile).toString()).shouldBeEmpty()
                unsafeCodePointsIn(yamlMergeOptions { sequences("/$hostile", YamlSequenceMerge.Append) }.toString())
                    .shouldBeEmpty()
            }
            YamlLayer("a", nodeOf("{secret: value}")).toString() shouldBe "YamlLayer(\"a\", Mapping)"
            YamlMerger().toString() shouldBe "YamlMerger(${YamlMergeOptions.Default})"
        }

        test("compare layers and strategies by value") {
            YamlLayer("a", nodeOf("[1]")) shouldBe YamlLayer("a", nodeOf("[1]"))
            YamlLayer("a", nodeOf("[1]")).hashCode() shouldBe YamlLayer("a", nodeOf("[1]")).hashCode()
            YamlLayer("a", nodeOf("[1]")) shouldNotBe YamlLayer("b", nodeOf("[1]"))
            YamlSequenceMerge.ByKey("k") shouldBe YamlSequenceMerge.ByKey("k")
            YamlSequenceMerge.ByKey("k").hashCode() shouldBe "k".hashCode()
            YamlSequenceMerge.Append.toString() shouldBe "Append"
        }
    }
})
