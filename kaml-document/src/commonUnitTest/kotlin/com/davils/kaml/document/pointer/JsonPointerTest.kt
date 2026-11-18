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

package com.davils.kaml.document.pointer

import com.davils.kaml.core.syntax.ast.YamlValueSyntax
import com.davils.kaml.document.fixture.lines
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.fixture.treeOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val example = nodeOf(
    lines(
        """{"foo": ["bar", "baz"], "": 0, "a/b": 1, "c%d": 2, "e^f": 3, "g|h": 4, "i\\j": 5,""",
        """"k\"l": 6, " ": 7, "m~n": 8}""",
    ),
)

class JsonPointerTest : FunSpec({
    context("parsing") {
        test("splits a pointer into its unescaped tokens") {
            "/foo/0".toJsonPointer().tokens shouldBe listOf("foo", "0")
            "/a~1b/m~0n".toJsonPointer().tokens shouldBe listOf("a/b", "m~n")
            "/~01".toJsonPointer().tokens shouldBe listOf("~1")
            "/".toJsonPointer().tokens shouldBe listOf("")
            "".toJsonPointer() shouldBe JsonPointer.ROOT
        }

        test("refuses a pointer without a leading slash or with a broken escape") {
            "foo".toJsonPointerOrNull().shouldBeNull()
            "/a~2".toJsonPointerOrNull().shouldBeNull()
            "/a~".toJsonPointerOrNull().shouldBeNull()
            shouldThrow<IllegalArgumentException> { JsonPointer.parse("x") }.message shouldBe
                "\"x\" is not a JSON pointer"
        }

        test("writes the escaped form") {
            JsonPointer.of("a/b", "m~n", "").text shouldBe "/a~1b/m~0n/"
            JsonPointer.of("a/b", "m~n", "").toString() shouldBe "/a~1b/m~0n/"
            val child = JsonPointer.of(listOf("x")).child(2)

            child.toString() shouldBe "/x/2"
            JsonPointer.ROOT.toString() shouldBe ""
        }

        test("knows its parent, its last token and its prefixes") {
            val pointer = JsonPointer.of("a", "b")

            pointer.parent shouldBe JsonPointer.of("a")
            pointer.last shouldBe "b"
            JsonPointer.ROOT.parent.shouldBeNull()
            JsonPointer.ROOT.isRoot shouldBe true
            pointer.startsWith(JsonPointer.of("a")) shouldBe true
            pointer.startsWith(JsonPointer.of("b")) shouldBe false
            JsonPointer.of("a").startsWith(pointer) shouldBe false
            pointer.hashCode() shouldBe JsonPointer.of("a", "b").hashCode()
        }
    }

    context("resolving on the graph") {
        test("follows the examples of RFC 6901") {
            example.at("".toJsonPointer()) shouldBe example
            example.at("/foo/0".toJsonPointer()) shouldBe nodeOf("bar")
            example.at("/".toJsonPointer()) shouldBe nodeOf("0")
            example.at("/a~1b".toJsonPointer()) shouldBe nodeOf("1")
            example.at("/c%d".toJsonPointer()) shouldBe nodeOf("2")
            example.at("/e^f".toJsonPointer()) shouldBe nodeOf("3")
            example.at("/g|h".toJsonPointer()) shouldBe nodeOf("4")
            example.at("/i\\j".toJsonPointer()) shouldBe nodeOf("5")
            example.at("/k\"l".toJsonPointer()) shouldBe nodeOf("6")
            example.at("/ ".toJsonPointer()) shouldBe nodeOf("7")
            example.at("/m~0n".toJsonPointer()) shouldBe nodeOf("8")
        }

        test("finds nothing for a missing member, a bad index or a scalar") {
            example.at("/missing".toJsonPointer()).shouldBeNull()
            example.at("/foo/2".toJsonPointer()).shouldBeNull()
            example.at("/foo/01".toJsonPointer()).shouldBeNull()
            example.at("/foo/-".toJsonPointer()).shouldBeNull()
            example.at("/foo/0/x".toJsonPointer()).shouldBeNull()
        }
    }

    context("resolving on the syntax tree") {
        val tree = treeOf(lines("a:", "  - x", "  - {b: 1}", "c: *z", "---", "d: 2"))

        test("finds the value syntax of a pointer") {
            val value = tree.at("/a/1/b".toJsonPointer())

            value.shouldBeInstanceOf<YamlValueSyntax.Scalar>().value shouldBe "1"
            tree.at("/d".toJsonPointer(), document = 1).shouldBeInstanceOf<YamlValueSyntax.Scalar>()
        }

        test("finds nothing through an alias or a scalar or in a missing document") {
            tree.at("/c/x".toJsonPointer()).shouldBeNull()
            tree.at("/a/0/x".toJsonPointer()).shouldBeNull()
            tree.at("/a".toJsonPointer(), document = 2).shouldBeNull()
        }

        test("falls back to the nearest existing ancestor") {
            val nearest = tree.nearest("/a/1/missing/deeper".toJsonPointer())

            nearest.shouldBeInstanceOf<YamlValueSyntax.Mapping>().entries.size shouldBe 1
            tree.nearest("/a".toJsonPointer(), document = 2).shouldBeNull()
        }
    }

    context("edges") {
        test("finds nothing below a missing member or for an empty index") {
            example.at("/missing/deeper".toJsonPointer()).shouldBeNull()
            example.at("/foo/".toJsonPointer()).shouldBeNull()
            val tree = treeOf(lines("e:", "f: 1"))

            tree.at("/e/x".toJsonPointer()).shouldBeNull()
        }

        test("compares as equal only to another pointer") {
            JsonPointer.ROOT.equals("") shouldBe false
        }
    }
})
