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
import com.davils.kaml.document.pointer.toJsonPointer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val FOO_BAR: String = """{"foo": "bar"}"""
private const val BAZ_FOO: String = """{"baz": "qux", "foo": "bar"}"""
private const val ESCAPED_KEYS: String = """{"/": 9, "~1": 10}"""
private const val MAPPING: String = "{a: 1}"
private const val NESTED: String = "{a: {b: 1}}"
private const val VALUE_FIELDS: String = """the operation needs a valid "path" and a "value""""

private fun patched(document: String, patch: String): JsonPatchResult {
    val reading = JsonPatch.read(nodeOf(patch)) as JsonPatchReading.Read
    return reading.patch.apply(nodeOf(document))
}

private fun applied(text: String): JsonPatchResult = JsonPatchResult.Applied(nodeOf(text))

private fun invalid(text: String): JsonPatchReading = JsonPatch.read(nodeOf(text))

class JsonPatchTest : FunSpec({
    context("the examples of RFC 6902") {
        test("A.1 adds an object member") {
            patched(
                document = FOO_BAR,
                patch = """[{"op": "add", "path": "/baz", "value": "qux"}]""",
            ) shouldBe applied(BAZ_FOO)
        }

        test("A.2 adds an array element") {
            patched(
                document = """{"foo": ["bar", "baz"]}""",
                patch = """[{"op": "add", "path": "/foo/1", "value": "qux"}]""",
            ) shouldBe
                applied("""{"foo": ["bar", "qux", "baz"]}""")
        }

        test("A.3 removes an object member") {
            patched(document = BAZ_FOO, patch = """[{"op": "remove", "path": "/baz"}]""") shouldBe applied(FOO_BAR)
        }

        test("A.4 removes an array element") {
            patched(
                document = """{"foo": ["bar", "qux", "baz"]}""",
                patch = """[{"op": "remove", "path": "/foo/1"}]""",
            ) shouldBe
                applied("""{"foo": ["bar", "baz"]}""")
        }

        test("A.5 replaces a value") {
            patched(document = BAZ_FOO, patch = """[{"op": "replace", "path": "/baz", "value": "boo"}]""") shouldBe
                applied("""{"baz": "boo", "foo": "bar"}""")
        }

        test("A.6 moves a value") {
            patched(
                document = """{"foo": {"bar": "baz", "waldo": "fred"}, "qux": {"corge": "grault"}}""",
                patch = """[{"op": "move", "from": "/foo/waldo", "path": "/qux/thud"}]""",
            ) shouldBe applied("""{"foo": {"bar": "baz"}, "qux": {"corge": "grault", "thud": "fred"}}""")
        }

        test("A.7 moves an array element") {
            patched(
                document = """{"foo": ["all", "grass", "cows", "eat"]}""",
                patch = """[{"op": "move", "from": "/foo/1", "path": "/foo/3"}]""",
            ) shouldBe applied("""{"foo": ["all", "cows", "eat", "grass"]}""")
        }

        test("A.8 passes a test") {
            patched(
                document = """{"baz": "qux", "foo": ["a", 2, "c"]}""",
                patch = lines(
                    """[{"op": "test", "path": "/baz", "value": "qux"},""",
                    """{"op": "test", "path": "/foo/1", "value": 2}]""",
                ),
            ) shouldBe applied("""{"baz": "qux", "foo": ["a", 2, "c"]}""")
        }

        test("A.9 fails a test") {
            patched(
                document = """{"baz": "qux"}""",
                patch = """[{"op": "test", "path": "/baz", "value": "bar"}]""",
            ) shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.TestFailed)
        }

        test("A.10 adds a nested member object") {
            patched(
                document = FOO_BAR,
                patch = """[{"op": "add", "path": "/child", "value": {"grandchild": {}}}]""",
            ) shouldBe
                applied("""{"foo": "bar", "child": {"grandchild": {}}}""")
        }

        test("A.11 ignores unrecognized elements") {
            patched(
                document = FOO_BAR,
                patch = """[{"op": "add", "path": "/baz", "value": "qux", "xyz": 123}]""",
            ) shouldBe
                applied(BAZ_FOO)
        }

        test("A.12 fails to add to a nonexistent target") {
            patched(document = FOO_BAR, patch = """[{"op": "add", "path": "/baz/bat", "value": "qux"}]""") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.MissingParent)
        }

        test("A.14 escapes the tilde") {
            patched(
                document = ESCAPED_KEYS,
                patch = """[{"op": "test", "path": "/~01", "value": 10}]""",
            ) shouldBe applied(ESCAPED_KEYS)
        }

        test("A.15 compares strings and numbers") {
            patched(document = ESCAPED_KEYS, patch = """[{"op": "test", "path": "/~01", "value": "10"}]""") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.TestFailed)
        }

        test("A.16 adds an array value") {
            patched(
                document = """{"foo": ["bar"]}""",
                patch = """[{"op": "add", "path": "/foo/-", "value": ["abc", "def"]}]""",
            ) shouldBe
                applied("""{"foo": ["bar", ["abc", "def"]]}""")
        }
    }

    context("failures") {
        test("name the failing operation and its reason") {
            patched(document = "[1]", patch = "[{op: add, path: /0, value: 0}, {op: add, path: /5, value: 2}]") shouldBe
                JsonPatchResult.Failed(1, JsonPatchRefusal.InvalidIndex)
            patched(document = "[1]", patch = "[{op: remove, path: /1}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.MissingTarget)
            patched(document = MAPPING, patch = "[{op: remove, path: /b}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.MissingTarget)
            patched(document = MAPPING, patch = "[{op: replace, path: /b, value: 1}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.MissingTarget)
            patched(document = MAPPING, patch = "[{op: add, path: /a/b, value: 1}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.NotAContainer)
            patched(document = MAPPING, patch = "[{op: remove, path: /a/b}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.NotAContainer)
            patched(document = MAPPING, patch = "[{op: remove, path: ''}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.RootRemoval)
            patched(document = NESTED, patch = "[{op: move, from: /a, path: /a/b/c}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.MoveIntoItself)
            patched(document = MAPPING, patch = "[{op: copy, from: /x, path: /b}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.MissingTarget)
        }
    }

    context("the whole document") {
        test("is replaced, added and copied at the root pointer") {
            patched(document = MAPPING, patch = "[{op: replace, path: '', value: [1]}]") shouldBe applied("[1]")
            patched(document = MAPPING, patch = "[{op: add, path: '', value: 2}]") shouldBe applied("2")
            patched(
                document = NESTED,
                patch = "[{op: copy, from: /a, path: /c}]",
            ) shouldBe applied("{a: {b: 1}, c: {b: 1}}")
            patched(document = MAPPING, patch = "[{op: move, from: /a, path: /a}]") shouldBe applied(MAPPING)
            patched(document = "[1, 2]", patch = "[{op: replace, path: /1, value: 3}]") shouldBe applied("[1, 3]")
        }
    }

    context("notation") {
        test("reads and writes every operation") {
            val text = lines(
                "[ {op: add, path: /a, value: 1}, {op: remove, path: /b}, {op: replace, path: /c, value: 2},",
                "  {op: move, from: /d, path: /e}, {op: copy, from: /f, path: /g}, {op: test, path: /h, value: 3} ]",
            )
            val patch = (JsonPatch.read(nodeOf(text)) as JsonPatchReading.Read).patch

            patch.operations shouldBe listOf(
                JsonPatchOperation.Add("/a".toJsonPointer(), nodeOf("1")),
                JsonPatchOperation.Remove("/b".toJsonPointer()),
                JsonPatchOperation.Replace("/c".toJsonPointer(), nodeOf("2")),
                JsonPatchOperation.Move(from = "/d".toJsonPointer(), path = "/e".toJsonPointer()),
                JsonPatchOperation.Copy(from = "/f".toJsonPointer(), path = "/g".toJsonPointer()),
                JsonPatchOperation.Test("/h".toJsonPointer(), nodeOf("3")),
            )
            JsonPatch.read(patch.toNode()) shouldBe JsonPatchReading.Read(patch)
            patch.hashCode() shouldBe JsonPatch(patch.operations).hashCode()
            patch.toString() shouldBe "JsonPatch(6 operations)"
            patch.operations.joinToString() shouldBe
                "Add(/a), Remove(/b), Replace(/c), Move(/d -> /e), Copy(/f -> /g), Test(/h)"
        }

        test("names the invalid operation") {
            invalid("{op: add}") shouldBe JsonPatchReading.Invalid(-1, "a patch must be a sequence of operations")
            invalid("[{path: /a}]") shouldBe JsonPatchReading.Invalid(0, """an operation needs an "op"""")
            invalid("[{op: drop, path: /a}]") shouldBe JsonPatchReading.Invalid(0, "the operation is not known")
            invalid("[{op: remove, path: /a}, {op: add, path: /a}]") shouldBe
                JsonPatchReading.Invalid(1, VALUE_FIELDS)
            invalid("[{op: add, value: 1}]") shouldBe JsonPatchReading.Invalid(0, VALUE_FIELDS)
            invalid("[{op: remove, path: x}]") shouldBe
                JsonPatchReading.Invalid(0, """the operation needs a valid "path"""")
            invalid("[{op: move, path: /a}]") shouldBe
                JsonPatchReading.Invalid(0, """the operation needs a valid "from" and "path"""")
            invalid("[{op: copy, from: /a}]") shouldBe
                JsonPatchReading.Invalid(0, """the operation needs a valid "from" and "path"""")
        }
    }

    context("edges") {
        test("refuse an addition below a nested scalar and a move from a missing member") {
            patched(document = NESTED, patch = "[{op: add, path: /a/b/c, value: 1}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.NotAContainer)
            patched(document = MAPPING, patch = "[{op: move, from: /x, path: /b}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.MissingTarget)
            patched(document = "[1]", patch = "[{op: remove, path: /x}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.MissingTarget)
        }

        test("stop at the first failing operation") {
            patched(document = MAPPING, patch = "[{op: remove, path: /x}, {op: add, path: /b, value: 1}]") shouldBe
                JsonPatchResult.Failed(0, JsonPatchRefusal.MissingTarget)
        }

        test("replace an existing member when adding it") {
            patched(document = MAPPING, patch = "[{op: add, path: /a, value: 2}]") shouldBe applied("{a: 2}")
        }

        test("compare as equal only to another patch") {
            val patch = JsonPatch(emptyList())
            val merge = JsonMergePatch(nodeOf(MAPPING))

            patch.equals(MAPPING) shouldBe false
            merge shouldBe JsonMergePatch(nodeOf(MAPPING))
            merge.equals(MAPPING) shouldBe false
            merge.hashCode() shouldBe nodeOf(MAPPING).hashCode()
            merge.toString() shouldBe "JsonMergePatch(Mapping)"
        }
    }
})
