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

import com.davils.kaml.core.source.YamlSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val ITERATIONS: Int = 3_000
private const val MOST_FRAGMENTS: Int = 40

private val fragments: List<String> = listOf(
    "%YAML 1.1\n", "%YAML 1.2\n", "%TAG !e! tag:e,2000:\n", "---", "...", "\n", "\r\n", " ", "  ", "\t",
    "-", "- ", "?", "? ", ":", ": ", ",", "[", "]", "{", "}", "#", " #c", "&a ", "*a", "&b ", "*b", "!", "!!int ",
    "!!float ", "!!str ", "!!merge ", "!e!x ", "!<tag:x> ", "!<> ", "|", "|2-", ">+", "'", "\"", "\\", "\\u", "\\x",
    "\uFEFF", "<<", "<<: ", "0x", "0o", "0b", "1", "9", "e", ".", "_", "1:2", ":3", "a", "b", "key", "~", "null",
    "yes", ".inf", "\u0085", "\u00A0", "%", "@", "`", "=", "\uD83D\uDE00", "\uD800",
)

private val documents: Arb<String> =
    Arb.list(Arb.element(fragments), 1..MOST_FRAGMENTS).map { chosen -> chosen.joinToString("") }

class YamlFuzzTest : FunSpec({
    context("documents built from fragments of YAML") {
        test("are parsed and read as events without an exception") {
            checkAll(ITERATIONS, documents) { text ->
                Yaml.Default.parse(text)
                Yaml.Default.events(YamlSource.of(text))
            }
        }
    }
})
