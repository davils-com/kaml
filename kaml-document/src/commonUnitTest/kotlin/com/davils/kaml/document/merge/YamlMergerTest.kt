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

import com.davils.kaml.document.YamlDocumentViolationCodes
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.pointer.toJsonPointer
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val DEFAULTS: String = "defaults"
private const val SITE: String = "site"
private const val HOST: String = "host"
private const val NESTED: String = "{a: {b: 1}}"
private const val LISTED: String = "{a: [1]}"

private fun layer(name: String, text: String): YamlLayer = YamlLayer(name, nodeOf(text))

private fun YamlMerger.merged(vararg layers: YamlLayer): YamlMerged = merge(layers.toList())

class YamlMergerTest : FunSpec({
    context("mappings") {
        val merged = YamlMerger().merged(
            layer(name = DEFAULTS, text = "{server: {port: 80, tls: false}, log: info, tags: [a]}"),
            layer(name = SITE, text = "{server: {port: 443, name: x}, tags: [b]}"),
            layer(name = HOST, text = "{log: debug}"),
        )

        test("merge members deeply and let later layers win") {
            merged.node shouldBe nodeOf("{server: {port: 443, tls: false, name: x}, log: debug, tags: [b]}")
        }

        test("record the layer every value comes from") {
            merged.originOf("/server/port".toJsonPointer()) shouldBe SITE
            merged.originOf("/server/tls".toJsonPointer()) shouldBe DEFAULTS
            merged.originOf("/server/name".toJsonPointer()) shouldBe SITE
            merged.originOf("/log".toJsonPointer()) shouldBe HOST
            merged.originOf("/tags/0".toJsonPointer()) shouldBe SITE
            merged.originOf("".toJsonPointer()) shouldBe HOST
            merged.origins["/server".toJsonPointer()] shouldBe SITE
        }

        test("let a scalar replace a mapping and a mapping replace a scalar") {
            val replaced = YamlMerger().merged(
                layer(name = DEFAULTS, text = "{a: {b: 1}, c: 1}"),
                layer(name = SITE, text = "{a: 2, c: {d: 3}}"),
            )

            replaced.node shouldBe nodeOf("{a: 2, c: {d: 3}}")
        }
    }

    context("deletion") {
        test("removes members tagged as deleted and strips markers from new values") {
            val merged = YamlMerger().merged(
                layer(name = DEFAULTS, text = "{a: 1, b: 2}"),
                layer(name = SITE, text = "{a: !delete , c: {d: !delete x, e: 1}, f: [!delete x, 2]}"),
            )

            merged.node shouldBe nodeOf("{b: 2, c: {e: 1}, f: [2]}")
        }

        test("removes members set to null when null deletes") {
            val options = yamlMergeOptions { deletion = YamlDeletion.Null }
            val merged = YamlMerger(
                options,
            ).merged(layer(name = DEFAULTS, text = "{a: 1, b: 2}"), layer(name = SITE, text = "{a: null, c: ~}"))

            merged.node shouldBe nodeOf("{b: 2}")
        }

        test("keeps markers as values when deletion is off") {
            val options = yamlMergeOptions { deletion = YamlDeletion.None }
            val merged = YamlMerger(
                options,
            ).merged(layer(name = DEFAULTS, text = "{a: 1}"), layer(name = SITE, text = "{a: null}"))

            merged.node shouldBe nodeOf("{a: null}")
        }
    }

    context("sequences") {
        test("are replaced by default") {
            val merged = YamlMerger().merged(layer(name = DEFAULTS, text = "[1, 2]"), layer(name = SITE, text = "[3]"))

            merged.node shouldBe nodeOf("[3]")
        }

        test("are appended where configured") {
            val options = yamlMergeOptions { sequences = YamlSequenceMerge.Append }
            val merged = YamlMerger(
                options,
            ).merged(layer(name = DEFAULTS, text = "{p: [1, 2]}"), layer(name = SITE, text = "{p: [3, !delete 4]}"))

            merged.node shouldBe nodeOf("{p: [1, 2, 3]}")
            merged.originOf("/p/0".toJsonPointer()) shouldBe DEFAULTS
            merged.originOf("/p/2".toJsonPointer()) shouldBe SITE
        }

        test("merge their items by a key per path") {
            val options = yamlMergeOptions {
                sequences("/servers", YamlSequenceMerge.ByKey("name"))
                sequences("/servers/*/ports", YamlSequenceMerge.Append)
            }
            val merged = YamlMerger(options).merged(
                layer(
                    name = DEFAULTS,
                    text = "{servers: [{name: a, ports: [1]}, {name: b, ports: [2]}, {name: c}, x]}",
                ),
                layer(
                    name = SITE,
                    text = "{servers: [{name: a, ports: [3], up: true}, !delete {name: b}, {name: d}, y, {id: 1}]}",
                ),
            )

            merged.node shouldBe nodeOf(
                "{servers: [{name: a, ports: [1, 3], up: true}, {name: c}, x, {name: d}, y, {id: 1}]}",
            )
            merged.originOf("/servers/0/ports/0".toJsonPointer()) shouldBe DEFAULTS
            merged.originOf("/servers/0/up".toJsonPointer()) shouldBe SITE
            merged.originOf("/servers/1".toJsonPointer()) shouldBe DEFAULTS
        }

        test("replace a sequence that meets a mapping") {
            val options = yamlMergeOptions { sequences = YamlSequenceMerge.Append }

            YamlMerger(
                options,
            ).merged(layer(name = DEFAULTS, text = NESTED), layer(name = SITE, text = LISTED)).node shouldBe
                nodeOf(LISTED)
            YamlMerger(
                options,
            ).merged(layer(name = DEFAULTS, text = LISTED), layer(name = SITE, text = NESTED)).node shouldBe
                nodeOf(NESTED)
        }

        test("merge keyed items that were merged before") {
            val options = yamlMergeOptions { sequences = YamlSequenceMerge.ByKey("id") }
            val merged = YamlMerger(options).merged(
                layer(name = DEFAULTS, text = "[{id: 1, a: 1}]"),
                layer(name = SITE, text = "[{id: 1, b: 2}]"),
                layer(name = HOST, text = "[{id: 1, c: 3}]"),
            )

            merged.node shouldBe nodeOf("[{id: 1, a: 1, b: 2, c: 3}]")
        }
    }

    context("options") {
        test("report every invalid option at once") {
            val error = shouldThrow<ValidationException> {
                yamlMergeOptions {
                    sequences = YamlSequenceMerge.ByKey(" ")
                    sequences("x", YamlSequenceMerge.Append)
                    sequences("/a", YamlSequenceMerge.ByKey(""))
                }
            }

            error.report.violations.map { it.code } shouldBe listOf(
                YamlDocumentViolationCodes.INVALID_KEY,
                YamlDocumentViolationCodes.INVALID_POINTER,
                YamlDocumentViolationCodes.INVALID_KEY,
            )
        }

        test("default to deletion by tag and replaced sequences") {
            YamlMergeOptions.Default.deletion shouldBe YamlDeletion.Tag
            YamlMergeOptions.Default.sequences shouldBe YamlSequenceMerge.Replace
            YamlMergeOptions.Default.toString() shouldBe
                "YamlMergeOptions(deletion=Tag, sequences=Replace, sequencesAt={})"
        }

        test("a merge needs a layer") {
            shouldThrow<IllegalArgumentException> { YamlMerger().merge(emptyList()) }
            val merged = YamlMerged(nodeOf("a"), emptyMap())

            merged.originOf("/x".toJsonPointer()).shouldBeNull()
        }
    }

    context("edges") {
        test("replace merged collections of another kind") {
            val options = yamlMergeOptions { sequences = YamlSequenceMerge.Append }
            val merger = YamlMerger(options)

            val toMapping = merger.merged(
                layer(name = DEFAULTS, text = LISTED),
                layer(name = SITE, text = "{a: [2]}"),
                layer(name = HOST, text = NESTED),
            )
            val toSequence = merger.merged(
                layer(name = DEFAULTS, text = NESTED),
                layer(name = SITE, text = "{a: {c: 2}}"),
                layer(name = HOST, text = LISTED),
            )

            toMapping.node shouldBe nodeOf(NESTED)
            toSequence.node shouldBe nodeOf(LISTED)
        }

        test("append keyed items that are no mappings") {
            val options = yamlMergeOptions { sequences = YamlSequenceMerge.ByKey("id") }
            val merged = YamlMerger(
                options,
            ).merged(layer(name = DEFAULTS, text = "[x, [1]]"), layer(name = SITE, text = "[{id: 1}]"))

            merged.node shouldBe nodeOf("[x, [1], {id: 1}]")
            merged.toString() shouldBe "YamlMerged(Sequence, 4 origins)"
        }
    }
})
