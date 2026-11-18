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

package com.davils.kaml.document.patch

import com.davils.kaml.document.fixture.lines
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.fixture.treeOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val A_B: String = """{"a":"b"}"""
private const val A_C: String = """{"a":"c"}"""
private const val A_FOO: String = """{"a":"foo"}"""
private const val C_LIST: String = """["c"]"""
private const val NULL: String = """null"""
private const val A_LIST: String = """{"a":["b"]}"""
private const val A_ONE: String = """{"a":[1]}"""
private const val B_C: String = """{"b":"c"}"""
private const val LAB: String = "# lab"
private const val SERVER: String = "server:"
private const val PORT: String = "  port: 8080 # default"

private fun merged(target: String, patch: String) = JsonMergePatch(nodeOf(patch)).apply(nodeOf(target))

class JsonMergePatchTest : FunSpec({
    context("the examples of RFC 7386") {
        test("merge every pair of the appendix") {
            listOf(
                Triple(first = A_B, second = A_C, third = A_C),
                Triple(first = A_B, second = B_C, third = """{"a":"b","b":"c"}"""),
                Triple(first = A_B, second = """{"a":null}""", third = """{}"""),
                Triple(first = """{"a":"b","b":"c"}""", second = """{"a":null}""", third = B_C),
                Triple(first = A_LIST, second = A_C, third = A_C),
                Triple(first = A_C, second = A_LIST, third = A_LIST),
                Triple(
                    first = """{"a":{"b":"c"}}""",
                    second = """{"a":{"b":"d","c":null}}""",
                    third = """{"a":{"b":"d"}}""",
                ),
                Triple(first = """{"a":[{"b":"c"}]}""", second = A_ONE, third = A_ONE),
                Triple(first = """["a","b"]""", second = """["c","d"]""", third = """["c","d"]"""),
                Triple(first = A_B, second = C_LIST, third = C_LIST),
                Triple(first = A_FOO, second = NULL, third = NULL),
                Triple(first = A_FOO, second = """"bar"""", third = """"bar""""),
                Triple(first = """{"e":null}""", second = """{"a":1}""", third = """{"e":null,"a":1}"""),
                Triple(first = """[1,2]""", second = """{"a":"b","c":null}""", third = A_B),
                Triple(first = """{}""", second = """{"a":{"bb":{"ccc":null}}}""", third = """{"a":{"bb":{}}}"""),
            ).forEach { (target, patch, result) -> merged(target = target, patch = patch) shouldBe nodeOf(result) }
        }
    }

    context("merging into the syntax tree") {
        val text = lines(LAB, SERVER, PORT, "  host: a", "debug: true")

        test("changes, adds and removes members and keeps the comments") {
            mergedText(text = text, patch = "{server: {port: 9090, host: null, name: n}, debug: null}") shouldBe
                lines(LAB, SERVER, "  port: 9090 # default", "  name: n", "")
        }

        test("replaces a member that is not a mapping") {
            mergedText(text = text, patch = "{debug: {level: 2}}") shouldBe
                lines(LAB, SERVER, PORT, "  host: a", "debug:", "  level: 2")
        }

        test("ignores the removal of a missing member") {
            mergedText(text = text, patch = "{missing: null}") shouldBe text
        }

        test("replaces the whole document when the patch is no mapping") {
            mergedText(text = text, patch = "[1]") shouldBe "# lab\n[1]"
        }

        test("refuses a member with a complex key") {
            JsonMergePatch(nodeOf("{[a]: 1}")).apply(treeOf("a: 1")) shouldBe
                JsonPatchEdit.Failed(0, JsonPatchRefusal.Unsupported)
        }
    }

    context("edges") {
        test("replaces a document that is no mapping") {
            mergedText(text = "[1]", patch = "{a: 1}") shouldBe "a: 1"
        }
    }
})
