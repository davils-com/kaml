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

package com.davils.kaml.core.node

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.fixture.everyHostileFragment
import com.davils.kaml.core.fixture.hostileText
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.shouldShowSafely
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlAnchor
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlNodeKind
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.model.YamlTagDirective
import com.davils.kaml.core.model.YamlVersion
import com.davils.kore.core.text.TextRange
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val PORT: Long = 8080L
private const val RATIO_KEY: String = "ratio"
private const val PAIR_MAPPING: String = "{a: 1, b: 2}"
private const val SINGLE_MAPPING: String = "{a: 1}"
private const val SMALL_PORT: Int = 22
private const val RATIO: Double = 0.5
private const val HUGE: String = "123456789012345678901234567890"
private const val BEYOND_INT: Long = 3_000_000_000L
private const val SECRET_KEY: String = "secret-key"
private const val TOO_MANY_DIGITS: Int = 400

class YamlNodeTest : FunSpec({
    val yaml = Yaml.Default

    context("reading a mapping") {
        val node = yaml.root(text("port: 8080", "name: web", "ratio: 0.5", "on: true", "none: ~", "list: [a, b]"))

        test("finds a value by its key") {
            node["port"].long() shouldBe PORT
            node["name"].string() shouldBe "web"
            node[RATIO_KEY].double() shouldBe RATIO
            node["on"].boolean() shouldBe true
        }

        test("finds nothing for a missing key or a key on a scalar") {
            node["missing"] shouldBe null
            node["name"]["anything"] shouldBe null
            null["anything"] shouldBe null
        }

        test("finds an item by its index") {
            node["list"][1].string() shouldBe "b"
            node["list"][2] shouldBe null
            node["name"][0] shouldBe null
        }

        test("knows a null value") {
            node["none"].isNull shouldBe true
            node["missing"].isNull shouldBe true
            node["name"].isNull shouldBe false
            node["none"].string() shouldBe null
        }

        test("converts numbers in both directions") {
            node["port"].double() shouldBe PORT.toDouble()
            node["port"].int() shouldBe PORT.toInt()
            node[RATIO_KEY].long() shouldBe null
            node["name"].double() shouldBe null
            node["name"].boolean() shouldBe null
            node["list"].string() shouldBe null
        }

        test("lists the keys") {
            (node as YamlNode.Mapping).keys.map { it.string() } shouldBe listOf(
                "port",
                "name",
                RATIO_KEY,
                "on",
                "none",
                "list",
            )
            node.kind shouldBe YamlNodeKind.Mapping
        }
    }

    context("large numbers") {
        test("returns no long for a number beyond its range") {
            yaml.root(HUGE).long() shouldBe null
            yaml.root(HUGE).double() shouldBe HUGE.toDouble()
        }

        test("returns no int for a number beyond its range") {
            yaml.root(BEYOND_INT.toString()).int() shouldBe null
        }
    }

    context("equality") {
        test("compares scalars by their canonical value") {
            yaml.root("0x10") shouldBe yaml.root("16")
            yaml.root("1.0") shouldBe yaml.root("1.00")
            yaml.root("0x10").hashCode() shouldBe yaml.root("16").hashCode()
            yaml.root("'16'") shouldNotBe yaml.root("16")
        }

        test("compares mappings regardless of their order") {
            yaml.root(PAIR_MAPPING) shouldBe yaml.root("{b: 2, a: 1}")
            yaml.root(PAIR_MAPPING).hashCode() shouldBe yaml.root("{b: 2, a: 1}").hashCode()
            yaml.root(SINGLE_MAPPING) shouldNotBe yaml.root("{a: 2}")
            yaml.root(SINGLE_MAPPING) shouldNotBe yaml.root(PAIR_MAPPING)
            yaml.root(SINGLE_MAPPING) shouldNotBe yaml.root("[a]")
        }

        test("compares sequences in their order") {
            yaml.root("[a, b]") shouldBe yaml.root("- a\n- b")
            yaml.root("[a, b]") shouldNotBe yaml.root("[b, a]")
            yaml.root("[a]") shouldNotBe yaml.root("a")
        }

        test("compares a node to itself") {
            val node = yaml.root("{a: [1]}")

            node shouldBe node
            (node as YamlNode.Mapping).entries.first().value shouldBe node["a"]
        }

        test("finds a value by a node key") {
            val node = yaml.root("? [a]\n: b") as YamlNode.Mapping

            node[yamlSequence(listOf(yamlScalar("a")))].string() shouldBe "b"
        }
    }

    context("text forms") {
        test("describes every kind of node") {
            yaml.root("a").toString() shouldContain "Scalar"
            yaml.root("[a]").toString() shouldContain "Sequence"
            yaml.root("{a: b}").toString() shouldContain "Mapping"
        }
    }

    context("building nodes") {
        test("builds scalars of every type") {
            yamlScalar("text").tag shouldBe YamlTag.STRING
            yamlScalar(PORT).long() shouldBe PORT
            yamlScalar(SMALL_PORT).int() shouldBe SMALL_PORT
            yamlScalar(RATIO).double() shouldBe RATIO
            yamlScalar(true).boolean() shouldBe true
            yamlScalar(false).string() shouldBe "false"
            yamlNull().isNull shouldBe true
            yamlTagged("10.0.0.1", YamlTag("!ipv4")).tag shouldBe YamlTag("!ipv4")
            yamlScalar("quoted", YamlScalarStyle.DoubleQuoted).style shouldBe YamlScalarStyle.DoubleQuoted
        }

        test("writes special floats in their YAML form") {
            yamlScalar(Double.NaN).value shouldBe ".nan"
            yamlScalar(Double.POSITIVE_INFINITY).value shouldBe ".inf"
            yamlScalar(Double.NEGATIVE_INFINITY).value shouldBe "-.inf"
        }

        test("builds a mapping with the DSL") {
            val built = yamlMapping {
                "host" to "web"
                "port" to PORT
                "small" to SMALL_PORT
                RATIO_KEY to RATIO
                "enabled" to true
                "tags" to yamlSequence {
                    add("a")
                    add(PORT)
                    add(SMALL_PORT)
                    add(RATIO)
                    add(false)
                    add(yamlNull())
                }
                yamlScalar(1) to yamlScalar("one")
            }

            built shouldBe yaml.root(
                text(
                    "host: web",
                    "port: 8080",
                    "small: 22",
                    "ratio: 0.5",
                    "enabled: true",
                    "tags: [a, 8080, 22, 0.5, false, null]",
                    "1: one",
                ),
            )
        }

        test("builds collections with a style and an anchor") {
            val anchor = YamlAnchor("shared")
            val sequence = yamlSequence(listOf(yamlScalar("a")), YamlCollectionStyle.Flow, anchor)
            val mapping = yamlMapping(emptyList(), YamlCollectionStyle.Flow, anchor)

            sequence.style shouldBe YamlCollectionStyle.Flow
            sequence.anchor shouldBe anchor
            mapping.anchor shouldBe anchor
            mapping.kind shouldBe YamlNodeKind.Mapping
            sequence.kind shouldBe YamlNodeKind.Sequence
            yamlScalar("a").kind shouldBe YamlNodeKind.Scalar
        }
    }

    context("streams") {
        test("know their single document") {
            YamlStream(listOf(YamlDocument(yamlNull()))).single?.root shouldBe yamlNull()
            YamlStream(emptyList()).single shouldBe null
        }
    }

    context("mappings built in code") {
        test("refuse a key given twice") {
            val entries = listOf(
                YamlEntry(key = yamlScalar(SECRET_KEY), value = yamlScalar(1)),
                YamlEntry(key = yamlScalar(SECRET_KEY), value = yamlScalar(2)),
            )
            val error = shouldThrow<IllegalArgumentException> { yamlMapping(entries) }

            error.message.orEmpty() shouldNotContain SECRET_KEY
            error.message.orEmpty() shouldContain "entry 1"
        }

        test("refuse a key given twice in the builder") {
            val error = shouldThrow<IllegalArgumentException> {
                yamlMapping {
                    SECRET_KEY to 1
                    SECRET_KEY to 2
                }
            }

            error.message.orEmpty() shouldNotContain SECRET_KEY
        }

        test("refuse keys that are equal as nodes") {
            shouldThrow<IllegalArgumentException> {
                yamlMapping {
                    yamlScalar(PORT) to "a"
                    yamlScalar(PORT.toInt()) to "b"
                }
            }
        }

        test("accept keys that only look alike") {
            val mapping = yamlMapping {
                "1" to "text"
                yamlScalar(1) to "number"
            }

            mapping.entries.size shouldBe 2
            mapping[yamlScalar(1)].string() shouldBe "number"
            mapping["1"].string() shouldBe "text"
        }

        test("keep a copy of the entries") {
            val entries = mutableListOf(YamlEntry(key = yamlScalar("a"), value = yamlNull()))
            val mapping = yamlMapping(entries)
            entries.clear()

            mapping.entries.size shouldBe 1
            mapping.keys shouldBe listOf(yamlScalar("a"))
        }
    }

    context("sequences built in code") {
        test("keep a copy of the items") {
            val items = mutableListOf<YamlNode>(yamlScalar("a"))
            val sequence = yamlSequence(items)
            items.clear()

            sequence.items shouldBe listOf(yamlScalar("a"))
        }
    }

    context("composed mappings") {
        test("keep the first of two equal keys") {
            val failed = Yaml.Default.parse(text("a: 1", "a: 2"))
            val mapping = (failed as? YamlParseResult.Failed)?.partial?.single?.root

            mapping.shouldBeInstanceOf<YamlNode.Mapping>().entries.size shouldBe 1
            mapping["a"].int() shouldBe 1
        }
    }

    context("numbers that are not finite") {
        test("give no double") {
            yaml.root(".nan").double() shouldBe null
            yaml.root(".inf").double() shouldBe null
            yaml.root("-.inf").double() shouldBe null
            yamlScalar(Double.POSITIVE_INFINITY).double() shouldBe null
        }

        test("give no double for an integer too large for one") {
            val huge = "1" + "0".repeat(TOO_MANY_DIGITS)

            yaml.root(huge).double() shouldBe null
            yaml.root(huge).long() shouldBe null
        }
    }

    context("hostile text") {
        test("is escaped by every text form of a node") {
            yamlScalar(everyHostileFragment).shouldShowSafely()
            checkAll(hostileText) { value ->
                val tag = YamlTag("!$value")
                val scalar = yamlTagged(value, tag)
                val entry = YamlEntry(key = scalar, value = yamlScalar(value))

                scalar.shouldShowSafely()
                entry.shouldShowSafely()
                yamlSequence(listOf(scalar), tag = tag).shouldShowSafely()
                yamlMapping(listOf(entry), tag = tag).shouldShowSafely()
                YamlDocument(scalar).shouldShowSafely()
            }
        }

        test("shows a scalar quoted") {
            yamlScalar("a").toString() shouldBe "Scalar(<tag:yaml.org,2002:str>, \"a\")"
        }
    }

    context("entries, documents and streams") {
        val entry = YamlEntry(key = yamlScalar("a"), value = yamlScalar(1))
        val document = YamlDocument(
            root = yamlMapping(listOf(entry)),
            version = YamlVersion.V1_2,
            tagDirectives = emptyList(),
            isExplicitStart = true,
            isExplicitEnd = false,
            range = TextRange.at(0),
        )

        test("compare entries by their keys and values") {
            val anything: Any = entry

            entry shouldBe YamlEntry(key = yamlScalar("a"), value = yamlScalar(1))
            entry.hashCode() shouldBe YamlEntry(key = yamlScalar("a"), value = yamlScalar(1)).hashCode()
            entry shouldNotBe YamlEntry(key = yamlScalar("b"), value = yamlScalar(1))
            entry shouldNotBe YamlEntry(key = yamlScalar("a"), value = yamlScalar(2))
            anything shouldNotBe "a"
        }

        test("compare documents by every property") {
            val root = yamlMapping(listOf(entry))
            val start = TextRange.at(0)
            val others = listOf(
                YamlDocument(root = yamlNull(), version = YamlVersion.V1_2, isExplicitStart = true, range = start),
                YamlDocument(root = root, version = null, isExplicitStart = true, range = start),
                YamlDocument(
                    root = root,
                    version = YamlVersion.V1_2,
                    tagDirectives = listOf(YamlTagDirective(handle = "!e!", prefix = "tag:e,2000:")),
                    isExplicitStart = true,
                    range = start,
                ),
                YamlDocument(root = root, version = YamlVersion.V1_2, isExplicitStart = true, range = null),
                YamlDocument(root = root, version = YamlVersion.V1_2, isExplicitStart = false, range = start),
                YamlDocument(
                    root = root,
                    version = YamlVersion.V1_2,
                    isExplicitStart = true,
                    isExplicitEnd = true,
                    range = start,
                ),
            )
            val same = YamlDocument(root = root, version = YamlVersion.V1_2, isExplicitStart = true, range = start)
            val anything: Any = document

            document shouldBe same
            document.hashCode() shouldBe same.hashCode()
            others.forEach { other -> document shouldNotBe other }
            anything shouldNotBe root
        }

        test("show a document with its root") {
            document.toString() shouldBe
                "YamlDocument(root=Mapping(<tag:yaml.org,2002:map>, 1 entries), version=1.2, " +
                "tagDirectives=0, isExplicitStart=true, isExplicitEnd=false, range=0..0)"
            YamlDocument(yamlNull()).toString() shouldContain "version=none"
        }

        test("compare streams by their documents") {
            val stream = YamlStream(listOf(document))
            val anything: Any = stream

            stream shouldBe YamlStream(listOf(document))
            stream.hashCode() shouldBe YamlStream(listOf(document)).hashCode()
            stream shouldNotBe YamlStream(emptyList())
            anything shouldNotBe document
            stream.toString() shouldBe "YamlStream(documents=1)"
        }

        test("keep copies of their lists") {
            val documents = mutableListOf(document)
            val stream = YamlStream(documents)
            documents.clear()

            stream.documents shouldBe listOf(document)
        }
    }
})
