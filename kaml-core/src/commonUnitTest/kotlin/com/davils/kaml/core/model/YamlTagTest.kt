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
package com.davils.kaml.core.model

import com.davils.kaml.core.fixture.everyHostileFragment
import com.davils.kaml.core.fixture.hostileText
import com.davils.kaml.core.fixture.shouldShowSafely
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll

private const val HANDLE: String = "!e!"
private const val PREFIX: String = "tag:example.com,2000:"
private const val LOCAL: String = "!local"
private const val LONG_URI: Int = 4_096

class YamlTagTest : FunSpec({
    context("a tag") {
        test("knows whether it is non-specific, local or core") {
            YamlTag.NON_SPECIFIC.isNonSpecific shouldBe true
            YamlTag.NON_SPECIFIC.isLocal shouldBe false
            YamlTag(LOCAL).isLocal shouldBe true
            YamlTag(LOCAL).isNonSpecific shouldBe false
            YamlTag(LOCAL).isCore shouldBe false
            YamlTag.STRING.isCore shouldBe true
            YamlTag.STRING.isLocal shouldBe false
            YamlTag("tag:example.com,2000:x").isLocal shouldBe false
        }

        test("names every tag of YAML") {
            val tags = listOf(
                YamlTag.STRING,
                YamlTag.INTEGER,
                YamlTag.FLOAT,
                YamlTag.BOOLEAN,
                YamlTag.NULL,
                YamlTag.SEQUENCE,
                YamlTag.MAPPING,
                YamlTag.BINARY,
                YamlTag.TIMESTAMP,
                YamlTag.MERGE,
                YamlTag.SET,
                YamlTag.ORDERED_MAP,
                YamlTag.PAIRS,
                YamlTag.VALUE,
            )

            tags.all(YamlTag::isCore) shouldBe true
            tags.toSet().size shouldBe tags.size
            YamlTag.core("str") shouldBe YamlTag.STRING
            YamlTag.STRING.uri shouldBe YamlTag.CORE_TAG_PREFIX + "str"
        }

        test("refuses an empty URI") {
            shouldThrow<IllegalArgumentException> { YamlTag("") }
        }

        test("shows its URI in angle brackets") {
            YamlTag.STRING.toString() shouldBe "<tag:yaml.org,2002:str>"
        }

        test("shows a hostile URI escaped") {
            YamlTag("!$everyHostileFragment").shouldShowSafely()
            checkAll(hostileText) { text -> YamlTag("!$text").shouldShowSafely() }
        }

        test("shows a long URI shortened") {
            val shown = YamlTag("!" + "x".repeat(LONG_URI)).toString()

            (shown.length < LONG_URI) shouldBe true
            shown shouldContain "chars)"
        }
    }

    context("a tag directive") {
        val directive = YamlTagDirective(handle = HANDLE, prefix = PREFIX)

        test("compares by its handle and its prefix") {
            directive shouldBe YamlTagDirective(handle = HANDLE, prefix = PREFIX)
            directive.hashCode() shouldBe YamlTagDirective(handle = HANDLE, prefix = PREFIX).hashCode()
            directive shouldNotBe YamlTagDirective(handle = "!f!", prefix = PREFIX)
            directive shouldNotBe YamlTagDirective(handle = HANDLE, prefix = "tag:other,2000:")
            val anything: Any = directive
            anything shouldNotBe HANDLE
        }

        test("shows its handle and its prefix quoted") {
            directive.toString() shouldBe """YamlTagDirective(handle="!e!", prefix="tag:example.com,2000:")"""
        }

        test("shows a hostile handle and prefix escaped") {
            checkAll(hostileText, hostileText) { handle, prefix ->
                YamlTagDirective(handle = handle, prefix = prefix).shouldShowSafely()
            }
        }
    }

    context("a version") {
        test("compares by both numbers") {
            YamlVersion(major = 1, minor = 2) shouldBe YamlVersion.V1_2
            YamlVersion(major = 1, minor = 2).hashCode() shouldBe YamlVersion.V1_2.hashCode()
            YamlVersion.V1_1 shouldNotBe YamlVersion.V1_2
            YamlVersion(major = 2, minor = 2) shouldNotBe YamlVersion.V1_2
            val anything: Any = YamlVersion.V1_2
            anything shouldNotBe "1.2"
        }

        test("orders by the major number first") {
            (YamlVersion.V1_1 < YamlVersion.V1_2) shouldBe true
            (YamlVersion(major = 2, minor = 0) > YamlVersion.V1_2) shouldBe true
            YamlVersion.V1_2.compareTo(YamlVersion(major = 1, minor = 2)) shouldBe 0
        }

        test("is written as a directive writes it") {
            YamlVersion.V1_2.toString() shouldBe "1.2"
        }

        test("refuses negative numbers") {
            shouldThrow<IllegalArgumentException> { YamlVersion(major = -1, minor = 0) }
            shouldThrow<IllegalArgumentException> { YamlVersion(major = 1, minor = -1) }
        }
    }
})
