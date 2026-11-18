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

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.ScalarValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val NOT_A_NUMBER: String = "abc"
private const val TOO_LARGE: String = "1e400"
private const val SHORT_ZONE_TIMESTAMP: String = "2001-12-14 21:59:43.10 -5"

private val coreValues: List<Triple<YamlTag, String, ScalarValue?>> = listOf(
    Triple(YamlTag.INTEGER, "0o17", ScalarValue.Integer("15")),
    Triple(YamlTag.INTEGER, "0x1F", ScalarValue.Integer("31")),
    Triple(YamlTag.INTEGER, "-12", ScalarValue.Integer("-12")),
    Triple(YamlTag.INTEGER, "+12", ScalarValue.Integer("12")),
    Triple(YamlTag.INTEGER, "0o8", null),
    Triple(YamlTag.FLOAT, ".inf", ScalarValue.Real(Double.POSITIVE_INFINITY)),
    Triple(YamlTag.FLOAT, TOO_LARGE, ScalarValue.Real(Double.POSITIVE_INFINITY)),
    Triple(YamlTag.FLOAT, "-$TOO_LARGE", ScalarValue.Real(Double.NEGATIVE_INFINITY)),
    Triple(YamlTag.FLOAT, "1.5", ScalarValue.Real(1.5)),
    Triple(YamlTag.FLOAT, NOT_A_NUMBER, null),
    Triple(YamlTag.NULL, "~", ScalarValue.Null),
    Triple(YamlTag.NULL, "x", null),
    Triple(YamlTag.BOOLEAN, "True", ScalarValue.Truth(true)),
    Triple(YamlTag.BOOLEAN, "yes", null),
    Triple(YamlTag.STRING, "yes", ScalarValue.Text("yes")),
    Triple(YamlTag.MERGE, "<<", ScalarValue.Text("<<")),
    Triple(YamlTag.MERGE, "x", null),
    Triple(YamlTag.TIMESTAMP, "2001-12-14", null),
)

private val jsonValues: List<Triple<YamlTag, String, ScalarValue?>> = listOf(
    Triple(YamlTag.INTEGER, "12", ScalarValue.Integer("12")),
    Triple(YamlTag.INTEGER, "-3", ScalarValue.Integer("-3")),
    Triple(YamlTag.INTEGER, "1.5", null),
    Triple(YamlTag.FLOAT, "1.5e3", ScalarValue.Real(1500.0)),
    Triple(YamlTag.FLOAT, TOO_LARGE, ScalarValue.Real(Double.POSITIVE_INFINITY)),
    Triple(YamlTag.FLOAT, NOT_A_NUMBER, null),
    Triple(YamlTag.NULL, "null", ScalarValue.Null),
    Triple(YamlTag.NULL, "Null", null),
    Triple(YamlTag.BOOLEAN, "false", ScalarValue.Truth(false)),
    Triple(YamlTag.BOOLEAN, "True", null),
    Triple(YamlTag.STRING, "x", ScalarValue.Text("x")),
)

private val legacyValues: List<Triple<YamlTag, String, ScalarValue?>> = listOf(
    Triple(YamlTag.BOOLEAN, "yes", ScalarValue.Truth(true)),
    Triple(YamlTag.BOOLEAN, "off", ScalarValue.Truth(false)),
    Triple(YamlTag.BOOLEAN, "maybe", null),
    Triple(YamlTag.INTEGER, "0b101", ScalarValue.Integer("5")),
    Triple(YamlTag.INTEGER, "-0b11", ScalarValue.Integer("-3")),
    Triple(YamlTag.INTEGER, "-0x1F", ScalarValue.Integer("-31")),
    Triple(YamlTag.INTEGER, "0755", ScalarValue.Integer("493")),
    Triple(YamlTag.INTEGER, "+017", ScalarValue.Integer("15")),
    Triple(YamlTag.INTEGER, "0", ScalarValue.Integer("0")),
    Triple(YamlTag.INTEGER, "1_000", ScalarValue.Integer("1000")),
    Triple(YamlTag.INTEGER, "190:20:30", ScalarValue.Integer("685230")),
    Triple(YamlTag.INTEGER, "-1:30", ScalarValue.Integer("-90")),
    Triple(YamlTag.INTEGER, NOT_A_NUMBER, null),
    Triple(YamlTag.FLOAT, ".inf", ScalarValue.Real(Double.POSITIVE_INFINITY)),
    Triple(YamlTag.FLOAT, "1:30.5", ScalarValue.Real(90.5)),
    Triple(YamlTag.FLOAT, "-1:30.5", ScalarValue.Real(-90.5)),
    Triple(YamlTag.FLOAT, "1_000.5", ScalarValue.Real(1000.5)),
    Triple(YamlTag.FLOAT, "1.5e+400", ScalarValue.Real(Double.POSITIVE_INFINITY)),
    Triple(YamlTag.FLOAT, NOT_A_NUMBER, null),
    Triple(YamlTag.NULL, "~", ScalarValue.Null),
    Triple(YamlTag.NULL, "x", null),
    Triple(YamlTag.MERGE, "<<", ScalarValue.Text("<<")),
    Triple(YamlTag.MERGE, "x", null),
    Triple(YamlTag.VALUE, "=", ScalarValue.Text("=")),
    Triple(YamlTag.VALUE, "x", null),
    Triple(YamlTag.TIMESTAMP, SHORT_ZONE_TIMESTAMP, ScalarValue.Text(SHORT_ZONE_TIMESTAMP)),
    Triple(YamlTag.TIMESTAMP, "x", null),
    Triple(YamlTag.BINARY, "R0lG\nODlh", ScalarValue.Text("R0lGODlh")),
    Triple(YamlTag.BINARY, "az+/09==", ScalarValue.Text("az+/09==")),
    Triple(YamlTag.BINARY, "[[[[", null),
    Triple(YamlTag.BINARY, "~~~~", null),
    Triple(YamlTag.BINARY, "@@@@", null),
    Triple(YamlTag.BINARY, "abc", null),
    Triple(YamlTag.STRING, "x", ScalarValue.Text("x")),
)

class ScalarFormsTest : FunSpec({
    mapOf(
        YamlSchema.Core to coreValues,
        YamlSchema.Json to jsonValues,
        YamlSchema.Yaml11 to legacyValues,
    ).forEach { (schema, values) ->
        context("the schema ${schema.name}") {
            values.forEach { (tag, text, expected) ->
                test("reads \"$text\" with the tag ${tag.uri} as ${expected ?: "no value"}") {
                    schema.valueOf(tag, text) shouldBe expected
                }
            }
        }
    }

    context("YAML 1.1 resolution") {
        test("resolves the timestamp with a short zone") {
            YamlSchema.Yaml11.resolvePlain(SHORT_ZONE_TIMESTAMP) shouldBe YamlTag.TIMESTAMP
        }

        test("resolves a binary, a hexadecimal and a sexagesimal float") {
            YamlSchema.Yaml11.resolvePlain("-0b11") shouldBe YamlTag.INTEGER
            YamlSchema.Yaml11.resolvePlain("0x1F") shouldBe YamlTag.INTEGER
            YamlSchema.Yaml11.resolvePlain("1:30.5") shouldBe YamlTag.FLOAT
            YamlSchema.Yaml11.resolvePlain(".5") shouldBe YamlTag.FLOAT
        }
    }
})
