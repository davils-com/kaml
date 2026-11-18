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

package com.davils.kaml.core.schema

import com.davils.kaml.core.YamlUnknownTags
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.core.fixture.problems
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlNodeKind
import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.core.yaml
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val IPV4: String = "!ipv4"
private const val HOSTS: String = "!hosts"

private val coreTags: Map<String, YamlTag> = mapOf(
    "" to YamlTag.NULL,
    "~" to YamlTag.NULL,
    "Null" to YamlTag.NULL,
    "TRUE" to YamlTag.BOOLEAN,
    "False" to YamlTag.BOOLEAN,
    "-12" to YamlTag.INTEGER,
    "+12" to YamlTag.INTEGER,
    "0o17" to YamlTag.INTEGER,
    "0x1F" to YamlTag.INTEGER,
    "1.5" to YamlTag.FLOAT,
    ".5" to YamlTag.FLOAT,
    "5." to YamlTag.FLOAT,
    "1e3" to YamlTag.FLOAT,
    "-1.5E-3" to YamlTag.FLOAT,
    "-.inf" to YamlTag.FLOAT,
    ".NaN" to YamlTag.FLOAT,
    "yes" to YamlTag.STRING,
    "0x" to YamlTag.STRING,
    "0o8" to YamlTag.STRING,
    "1e" to YamlTag.STRING,
    "." to YamlTag.STRING,
    "+" to YamlTag.STRING,
    "<<" to YamlTag.STRING,
)

private val jsonTags: Map<String, YamlTag?> = mapOf(
    "null" to YamlTag.NULL,
    "true" to YamlTag.BOOLEAN,
    "-0" to YamlTag.INTEGER,
    "10" to YamlTag.INTEGER,
    "1.5e3" to YamlTag.FLOAT,
    "0.5" to YamlTag.FLOAT,
    "01" to null,
    "Null" to null,
    "1." to YamlTag.FLOAT,
    "-" to null,
    ".5" to null,
)

private val legacyTags: Map<String, YamlTag> = mapOf(
    "yes" to YamlTag.BOOLEAN,
    "Off" to YamlTag.BOOLEAN,
    "n" to YamlTag.BOOLEAN,
    "0b1010" to YamlTag.INTEGER,
    "0755" to YamlTag.INTEGER,
    "-0x1_F" to YamlTag.INTEGER,
    "1_000" to YamlTag.INTEGER,
    "190:20:30" to YamlTag.INTEGER,
    "1_000.5" to YamlTag.FLOAT,
    "190:20:30.15" to YamlTag.FLOAT,
    "-.INF" to YamlTag.FLOAT,
    "<<" to YamlTag.MERGE,
    "=" to YamlTag.VALUE,
    "2001-12-14" to YamlTag.TIMESTAMP,
    "2001-12-14t21:59:43.10-05:00" to YamlTag.TIMESTAMP,
    "~" to YamlTag.NULL,
    "maybe" to YamlTag.STRING,
)

class YamlSchemaTest : FunSpec({
    context("the core schema") {
        coreTags.forEach { (text, tag) ->
            test(resolution(text, tag)) {
                YamlSchema.Core.resolvePlain(text) shouldBe tag
            }
        }
    }

    context("the JSON schema") {
        jsonTags.forEach { (text, tag) ->
            test(resolution(text, tag)) {
                YamlSchema.Json.resolvePlain(text) shouldBe (tag ?: YamlTag.STRING)
                YamlSchema.Json.plainTagOf(text) shouldBe tag
            }
        }

        test("reports a plain scalar that is no JSON value") {
            yaml { schema = YamlSchema.Json }.problems("key: value") shouldContain YamlDiagnosticCode.NotJson
        }

        test("refuses no plain scalar in the other schemas") {
            YamlSchema.Core.plainTagOf("no JSON") shouldBe YamlTag.STRING
        }

        test("knows that it is the JSON schema") {
            YamlSchema.Json.isJson shouldBe true
            YamlSchema.Core.isJson shouldBe false
        }
    }

    context("the failsafe schema") {
        test("reads every plain scalar as a string") {
            YamlSchema.Failsafe.resolvePlain("12") shouldBe YamlTag.STRING
        }

        test("refuses a typed value") {
            YamlSchema.Failsafe.valueOf(YamlTag.INTEGER, "12") shouldBe null
        }
    }

    context("the YAML 1.1 schema") {
        legacyTags.forEach { (text, tag) ->
            test(resolution(text, tag)) {
                YamlSchema.Yaml11.resolvePlain(text) shouldBe tag
            }
        }

        test("accepts base64 for a binary") {
            YamlSchema.Yaml11.valueOf(YamlTag.BINARY, "R0lG!") shouldBe null
            (YamlSchema.Yaml11.valueOf(YamlTag.BINARY, "R0lG ODlh") != null) shouldBe true
        }

        test("is selected by a %YAML 1.1 directive") {
            val node = yaml().root(text("%YAML 1.1", "---", "flag: yes"))

            node["flag"]?.tag shouldBe YamlTag.BOOLEAN
        }

        test("is not selected when the version does not select the schema") {
            val node = yaml { isSchemaSelectedByVersion = false }.root(text("%YAML 1.1", "---", "flag: yes"))

            node["flag"]?.tag shouldBe YamlTag.STRING
        }
    }

    context("explicit tags") {
        test("reports a value that does not fit its core tag") {
            yaml().problems("port: !!int eighty") shouldContain YamlDiagnosticCode.InvalidTaggedValue
        }

        test("reports a core tag on the wrong kind of node") {
            yaml().problems("port: !!map eighty") shouldContain YamlDiagnosticCode.TagKindMismatch
        }

        test("keeps an unknown tag by default") {
            yaml().problems("host: !ipv4 10.0.0.1") shouldNotContain YamlDiagnosticCode.UnknownTag
        }

        test("refuses an unknown tag when asked to") {
            val strict = yaml { unknownTags = YamlUnknownTags.Refuse }

            strict.problems("host: !ipv4 10.0.0.1") shouldContain YamlDiagnosticCode.UnknownTag
            strict.problems("hosts: !list [a]") shouldContain YamlDiagnosticCode.UnknownTag
        }

        test("reads a non-specific tag as a string") {
            yaml().root("! 12").tag shouldBe YamlTag.STRING
        }
    }

    context("a custom schema") {
        val custom = yamlSchema {
            name = "network"
            scalar(IPV4) {
                implicit { text -> text.count { it == '.' } == 3 && text.all { it.isDigit() || it == '.' } }
                accepts { text -> text.split('.').size == 4 }
            }
            sequence(HOSTS)
            mapping("!interface")
        }

        test("resolves an implicit custom tag before the core tags") {
            yaml { schema = custom }.root("host: 10.0.0.1")["host"]?.tag shouldBe YamlTag(IPV4)
        }

        test("checks a custom scalar tag") {
            yaml { schema = custom }.problems(
                "host: !ipv4 nope",
            ) shouldContain YamlDiagnosticCode.InvalidTaggedValue
        }

        test("knows custom collection tags") {
            val strict = yaml {
                schema = custom
                unknownTags = YamlUnknownTags.Refuse
            }
            val node = strict.root(text("eth0: !interface {mtu: 1500}", "all: !hosts [a]"))

            node["eth0"]?.tag shouldBe YamlTag("!interface")
            node["all"]?.tag shouldBe YamlTag(HOSTS)
        }

        test("reports a custom collection tag on a scalar") {
            yaml { schema = custom }.problems(
                "eth0: !interface lo",
            ) shouldContain YamlDiagnosticCode.TagKindMismatch
        }

        test("lists its definitions") {
            custom.definitionOf(YamlTag(HOSTS))?.kind shouldBe YamlNodeKind.Sequence
            custom.definitionOf(YamlTag(IPV4))?.isImplicit shouldBe true
            custom.definitionOf(YamlTag("!other")) shouldBe null
            custom.toString() shouldContain "network"
            custom.tags.first().toString() shouldContain IPV4
        }

        test("refuses a blank tag") {
            shouldThrow<ValidationException> { yamlSchema { scalar("") } }
        }

        test("refuses to redefine a core tag") {
            shouldThrow<ValidationException> { yamlSchema { scalar("tag:yaml.org,2002:int") } }
        }

        test("refuses a tag declared twice") {
            shouldThrow<ValidationException> {
                yamlSchema {
                    mapping("!twice")
                    mapping("!twice")
                }
            }
        }

        test("enables merge keys for the core schema") {
            val merging = yamlSchema { hasMergeKeys = true }
            val node = yaml { schema = merging }.root(text("base: &b {a: 1}", "item:", "  <<: *b", "  c: 2"))

            node["item"]["a"].string() shouldBe "1"
        }

        test("derives from YAML 1.1 without refusing its built-in tags") {
            val legacy = yamlSchema(YamlSchema.Yaml11) { scalar("!extra") }

            legacy.hasMergeKeys shouldBe true
        }
    }
})

private fun resolution(text: String, tag: YamlTag?): String = "resolves \"$text\" to ${tag ?: "nothing"}"
