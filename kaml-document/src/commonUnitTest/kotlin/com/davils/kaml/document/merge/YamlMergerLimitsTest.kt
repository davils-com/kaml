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
import com.davils.kaml.document.fixture.collidingMapping
import com.davils.kaml.document.fixture.growsNearLinearly
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private const val ITEMS: Int = 20_000

private const val NAME: String = "name"
private const val WIDTH: Int = 20_000
private const val FEW_LAYERS: Int = 2
private const val MANY_LAYERS: Int = 100
private const val LAYER_GROWTH: Int = 3
private const val THREE: Int = 3
private const val SHORT_POINTER: Int = 5_000
private const val LONG_POINTER: Int = 50_000
private const val POINTER_GROWTH: Int = 20
private const val COLLIDING_BITS: Int = 3
private const val FEW_BITS: Int = 11
private const val MANY_BITS: Int = 13
private const val GROWTH: Int = 8
private val slack: Duration = 200.milliseconds

private fun colliding(bits: Int): List<String> = (0 until (1 shl bits)).map { number ->
    (0 until bits).joinToString("") { bit -> "Aa".takeIf { number shr bit and 1 == 0 } ?: "BB" }
}

private fun named(names: List<String>): YamlNode =
    yamlSequence(names.map { name -> yamlMapping(listOf(YamlEntry(key = yamlScalar(NAME), value = yamlScalar(name)))) })

private fun mergedByName(names: List<String>): YamlMerged {
    val byName = yamlMergeOptions { sequences = YamlSequenceMerge.ByKey(NAME) }
    return YamlMerger(byName).merge(listOf(YamlLayer("a", named(names)), YamlLayer("b", named(names))))
}

private fun wide(size: Int): YamlNode =
    yamlMapping(List(size) { index -> YamlEntry(key = yamlScalar("k$index"), value = yamlScalar("v")) })

private fun timed(work: () -> Unit): Duration {
    val mark = TimeSource.Monotonic.markNow()
    work()
    return mark.elapsedNow()
}

private fun item(name: Int, member: String): YamlNode = yamlMapping(
    listOf(
        YamlEntry(key = yamlScalar("name"), value = yamlScalar("n$name")),
        YamlEntry(key = yamlScalar(member), value = yamlScalar(1)),
    ),
)

class YamlMergerLimitsTest : FunSpec({
    context("keyed sequences") {
        test("merge through an index, however many items they hold") {
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

    context("keyed sequences whose identities have colliding hash codes") {
        test("are what the test feeds the merge") {
            val hashCodes = colliding(COLLIDING_BITS).map(String::hashCode)

            hashCodes.toSet().size shouldBe 1
        }

        test("merge in near-linear time on every target") {
            val few = colliding(FEW_BITS)
            val many = colliding(MANY_BITS)
            val fewTime = timed { mergedByName(few) }
            val manyTime = timed { mergedByName(many) }

            (manyTime < fewTime * GROWTH + slack) shouldBe true
        }

        test("merge the items with the same identity") {
            val names = colliding(COLLIDING_BITS)

            (mergedByName(names).node as YamlNode.Sequence).items.size shouldBe names.size
        }
    }

    context("layers over a wide mapping") {
        test("cost what they change, not the width of the mapping") {
            val base = YamlLayer("base", wide(WIDTH))
            val empty = yamlMapping(emptyList())
            val fewTime = timed { YamlMerger().merge(listOf(base) + List(FEW_LAYERS) { YamlLayer("l$it", empty) }) }
            val manyTime = timed { YamlMerger().merge(listOf(base) + List(MANY_LAYERS) { YamlLayer("l$it", empty) }) }

            (manyTime < fewTime * LAYER_GROWTH + slack) shouldBe true
        }

        test("keep the result and its origins") {
            val changed = YamlLayer("c", nodeOf("{k1: changed, k2: null}"))
            val layers = listOf(YamlLayer("base", wide(THREE)), YamlLayer("e", yamlMapping(emptyList())), changed)
            val merged = YamlMerger(yamlMergeOptions { deletion = YamlDeletion.Null }).merge(layers)

            merged.node shouldBe nodeOf("{k0: v, k1: changed}")
            merged.originOf(JsonPointer.of("k0")) shouldBe "base"
            merged.originOf(JsonPointer.of("k1")) shouldBe "c"
        }
    }

    context("origins") {
        test("are recorded in near-linear time, also when the hash codes of the keys collide") {
            val isLinear = growsNearLinearly { bits ->
                val layers = listOf(YamlLayer("a", collidingMapping(bits)), YamlLayer("b", wide(1)))
                val merged = YamlMerger().merge(layers)
                merged.origins.size shouldBe (1 shl bits) + 2
            }

            isLinear shouldBe true
        }

        test("are found in time that does not grow with the square of the length of a pointer") {
            val merged = YamlMerger().merge(listOf(YamlLayer("only", wide(1))))
            val shortTime = timed { merged.originOf(JsonPointer.of(List(SHORT_POINTER) { "x" })) }
            val longTime = timed { merged.originOf(JsonPointer.of(List(LONG_POINTER) { "x" })) }

            (longTime < shortTime * POINTER_GROWTH + slack) shouldBe true
            merged.originOf(JsonPointer.of(List(LONG_POINTER) { "k0" })) shouldBe "only"
        }

        test("are those of the nearest listed ancestor") {
            val layers = listOf(YamlLayer("a", nodeOf("{x: {y: [1, {z: 2}]}}")), YamlLayer("b", nodeOf("{x: {w: 3}}")))
            val merged = YamlMerger().merge(layers)

            merged.originOf(JsonPointer.ROOT) shouldBe "b"
            merged.originOf(JsonPointer.of("x", "w")) shouldBe "b"
            merged.originOf(JsonPointer.of("x", "y", "1", "z")) shouldBe "a"
            merged.originOf(JsonPointer.of("x", "y", "9")) shouldBe "a"
            merged.originOf(JsonPointer.of("q")) shouldBe "b"
        }
    }

    context("layers over a long sequence") {
        test("cost what they change when they merge by key or append, not the length of the sequence") {
            val byName = yamlMergeOptions { sequences = YamlSequenceMerge.ByKey(NAME) }
            val appending = yamlMergeOptions { sequences = YamlSequenceMerge.Append }
            val base = YamlLayer("base", named(List(WIDTH) { index -> "n$index" }))
            val layers = { count: Int ->
                listOf(base) + List(count) { YamlLayer("l$it", nodeOf("[{name: n0, x: $it}]")) }
            }
            val keyedFew = timed { YamlMerger(byName).merge(layers(FEW_LAYERS)) }
            val keyedMany = timed { YamlMerger(byName).merge(layers(MANY_LAYERS)) }
            val appendedFew = timed { YamlMerger(appending).merge(layers(FEW_LAYERS)) }
            val appendedMany = timed { YamlMerger(appending).merge(layers(MANY_LAYERS)) }

            (keyedMany < keyedFew * LAYER_GROWTH + slack) shouldBe true
            (appendedMany < appendedFew * LAYER_GROWTH + slack) shouldBe true
        }

        test("merge, delete and add items by key over several layers") {
            val byName = yamlMergeOptions { sequences = YamlSequenceMerge.ByKey(NAME) }
            val layers = listOf(
                YamlLayer("a", nodeOf("[{name: a, v: 1}, {name: b}]")),
                YamlLayer("b", nodeOf("[{name: a, w: 2}, {name: c}]")),
                YamlLayer("c", nodeOf("[{name: c, z: 3}, !delete {name: b}]")),
            )

            YamlMerger(byName).merge(layers).node shouldBe nodeOf("[{name: a, v: 1, w: 2}, {name: c, z: 3}]")
        }
    }

    context("keyed items over several layers") {
        test("merge into items an earlier layer merged, and ignore deletions of missing items and members") {
            val options = yamlMergeOptions { sequences = YamlSequenceMerge.ByKey(NAME) }
            val layers = listOf(
                YamlLayer("a", nodeOf("[{name: a, v: 1}, {name: {x: 1}}, {other: 1}, [1], {name: 2}]")),
                YamlLayer("b", nodeOf("[{name: a, w: 2}, {name: {x: 1}, y: 1}, !delete {name: gone}]")),
                YamlLayer("c", nodeOf("[{name: a, z: 3}, {name: 2, u: 1}]")),
                YamlLayer("d", nodeOf("{gone: !delete x}")),
            )
            val sequences = YamlMerger(options).merge(layers.dropLast(1))
            val mapping = YamlMerger(options).merge(listOf(YamlLayer("a", nodeOf("{k: 1}")), layers.last()))
            val readded = YamlMerger(options).merge(
                listOf(
                    YamlLayer("a", nodeOf("{k: 1, 1: one}")),
                    YamlLayer("b", nodeOf("{k: !delete x, 1: !delete x}")),
                    YamlLayer("c", nodeOf("{k: 2, 0x1: uno}")),
                ),
            )

            (sequences.node as YamlNode.Sequence).items.first() shouldBe nodeOf("{name: a, v: 1, w: 2, z: 3}")
            mapping.node shouldBe nodeOf("{k: 1}")
            readded.node shouldBe nodeOf("{k: 2, 0x1: uno}")
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
            YamlLayer("a", nodeOf("[1]")).equals(nodeOf("[1]")) shouldBe false
            YamlSequenceMerge.ByKey("k") shouldBe YamlSequenceMerge.ByKey("k")
            YamlSequenceMerge.ByKey("k").hashCode() shouldBe "k".hashCode()
            YamlSequenceMerge.Append.toString() shouldBe "Append"
        }
    }
})
