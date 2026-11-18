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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val patterns: Map<String, Map<String, Boolean>> = mapOf(
    "isBinary" to mapOf("0b1010" to true, "-0b1_0" to true, "+0b1" to true, "0b" to false, "0b2" to false),
    "isHex" to mapOf("0x1F" to true, "-0x1_f" to true, "0x" to false, "0xG" to false, "x1" to false),
    "isOctal" to mapOf("0755" to true, "+0_17" to true, "0" to false, "08" to false, "-07a" to false),
    "isDecimal" to mapOf(
        "0" to true,
        "-0" to true,
        "1_000" to true,
        "+5" to true,
        "00" to false,
        "12a" to false,
        "" to false,
        "_1" to false,
    ),
    "isSexagesimal" to mapOf(
        "190:20:30" to true,
        "-1:5" to true,
        "1:60" to false,
        "1:" to false,
        "0:30" to false,
        "1:123" to false,
        "1" to false,
    ),
    "isReal" to mapOf(
        "1.5" to true,
        "1_000.5" to true,
        ".5" to true,
        "1." to true,
        "+1.5e+3" to true,
        "." to false,
        "1.5e3" to false,
        "1e+3" to false,
        "1.5e+" to false,
        "1.5x" to false,
    ),
    "isSexagesimalReal" to mapOf(
        "190:20:30.15" to true,
        "-1:30." to true,
        "0:30.5" to true,
        "1:30" to false,
        "1.5" to false,
        "x:30.5" to false,
    ),
    "isDate" to mapOf(
        "2001-12-14" to true,
        "2001-1-14" to false,
        "01-12-14" to false,
        "2001-12-14x" to false,
        "2001/12/14" to false,
        "2001-12" to false,
        "2001-12-x" to false,
    ),
    "isTimestamp" to mapOf(
        "2001-12-14 21:59:43.10 -5" to true,
        "2001-12-14t21:59:43.10-05:00" to true,
        "2001-12-14T21:59:43Z" to true,
        "2001-12-14 21:59:43" to true,
        "2001-1-4 1:59:43." to true,
        "2001-12-14 21:59:43 " to false,
        "2001-12-14 21:59" to false,
        "2001-12-14 21:59:43 +01:3" to false,
        "2001-12-14 21:59:43 x" to false,
        "2001-12-14x21:59:43" to false,
        "2001-12-14" to false,
        "2001-12-14 21x" to false,
        "2001-12-14 21:x9:43" to false,
        "2001-12-14 21:59x" to false,
        "2001-12-14 21:59:x3" to false,
    ),
)

private val scanners: Map<String, (String) -> Boolean> = mapOf(
    "isBinary" to Yaml11Patterns::isBinary,
    "isHex" to Yaml11Patterns::isHex,
    "isOctal" to Yaml11Patterns::isOctal,
    "isDecimal" to Yaml11Patterns::isDecimal,
    "isSexagesimal" to Yaml11Patterns::isSexagesimal,
    "isReal" to Yaml11Patterns::isReal,
    "isSexagesimalReal" to Yaml11Patterns::isSexagesimalReal,
    "isDate" to Yaml11Patterns::isDate,
    "isTimestamp" to Yaml11Patterns::isTimestamp,
)

class Yaml11PatternsTest : FunSpec({
    patterns.forEach { (name, cases) ->
        context(name) {
            val scanner = scanners.getValue(name)
            cases.forEach { (text, expected) ->
                test("${"accepts".takeIf { expected } ?: "refuses"} \"$text\"") {
                    scanner(text) shouldBe expected
                }
            }
        }
    }
})
