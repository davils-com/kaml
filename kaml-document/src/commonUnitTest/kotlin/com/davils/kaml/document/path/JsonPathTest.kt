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

package com.davils.kaml.document.path

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.fixture.lines
import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.fixture.nodesOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private val store = nodeOf(
    lines(
        """{ "store": {""",
        """"book": [""",
        """{ "category": "reference", "author": "Nigel Rees", "title": "Sayings of the Century", "price": 8.95 },""",
        """{ "category": "fiction", "author": "Evelyn Waugh", "title": "Sword of Honour", "price": 12.99 },""",
        """{ "category": "fiction", "author": "Herman Melville", "title": "Moby Dick",""",
        """"isbn": "0-553-21311-3", "price": 8.99 },""",
        """{ "category": "fiction", "author": "J. R. R. Tolkien", "title": "The Lord of the Rings",""",
        """"isbn": "0-395-19395-8", "price": 22.99 }""",
        "],",
        """"bicycle": { "color": "red", "price": 399 }""",
        "}",
        "}",
    ),
)

private val letters = nodeOf("""["a", "b", "c", "d", "e", "f", "g"]""")

private val filtered = nodeOf(
    lines(
        """{ "a": [3, 5, 1, 2, 4, 6, {"b": "j"}, {"b": "k"}, {"b": {}}, {"b": "kilo"}],""",
        """"o": {"p": 1, "q": 2, "r": 3, "s": 5, "t": {"u": 6}},""",
        """"e": "f" }""",
    ),
)

private const val BOOKS: Int = 4
private const val LETTERS: Int = 7
private const val WITHOUT_B: Int = 6
private const val ITEMS: Int = 10
private const val PEOPLE: Int = 3
private const val NOT_NULL: Int = 6
private const val DOTTED: String = "'a.b'"

private fun select(expression: String, node: YamlNode = store): List<YamlNode> {
    val matches = JsonPath.parse(expression).query(node)
    return matches.map(JsonPathMatch::node)
}

private fun locations(expression: String): List<String> {
    val matches = JsonPath.parse(expression).query(store)
    return matches.map { match -> match.location.text }
}

class JsonPathTest : FunSpec({
    context("the bookstore of RFC 9535") {
        test("selects the authors of every book") {
            select("$.store.book[*].author") shouldBe nodesOf(
                "Nigel Rees",
                "Evelyn Waugh",
                "Herman Melville",
                "J. R. R. Tolkien",
            )
            select("$..author") shouldHaveSize BOOKS
        }

        test("selects every price below the store in document order") {
            select("$.store..price") shouldBe nodesOf("8.95", "12.99", "8.99", "22.99", "399")
        }

        test("selects books by index, from the end and by union") {
            select("$..book[2].author") shouldBe nodesOf("Herman Melville")
            select("$..book[2].publisher").shouldBeEmpty()
            select("$..book[-1].title") shouldBe nodesOf("The Lord of the Rings")
            select("$..book[0,1].title") shouldBe select("$..book[:2].title")
        }

        test("filters books by the existence and the value of a member") {
            select("$..book[?@.isbn].title") shouldBe nodesOf("Moby Dick", "The Lord of the Rings")
            select("$..book[?@.price<10].title") shouldBe nodesOf("Sayings of the Century", "Moby Dick")
        }

        test("selects the members of the store") {
            select("$.store.*") shouldHaveSize 2
        }

        test("reports the location of every match") {
            locations("$..book[2].author") shouldBe listOf("/store/book/2/author")
            locations("$.store.bicycle['color']") shouldBe listOf("/store/bicycle/color")
            locations("$") shouldBe listOf("")
        }
    }

    context("slices") {
        test("follow start, end and step") {
            select("$[1:3]", letters) shouldBe nodesOf("b", "c")
            select("$[5:]", letters) shouldBe nodesOf("f", "g")
            select("$[1:5:2]", letters) shouldBe nodesOf("b", "d")
            select("$[5:1:-2]", letters) shouldBe nodesOf("f", "d")
            select("$[::-1]", letters) shouldBe nodesOf("g", "f", "e", "d", "c", "b", "a")
        }

        test("select nothing for a step of zero or a mapping") {
            select("$[::0]", letters).shouldBeEmpty()
            select("$[0:1]", filtered).shouldBeEmpty()
        }

        test("clamp indices beyond the sequence") {
            select("$[-100:100]", letters) shouldHaveSize LETTERS
            select("$[100]", letters).shouldBeEmpty()
            select("$[-1]", letters) shouldBe nodesOf("g")
        }
    }

    context("filters") {
        test("compare members with literals") {
            select("$.a[?@.b == 'kilo']", filtered) shouldBe nodesOf("{b: kilo}")
            select("$.a[?(@.b == 'kilo')]", filtered) shouldBe nodesOf("{b: kilo}")
            select("$.a[?@>3.5]", filtered) shouldBe nodesOf("5", "4", "6")
            select("$.a[?@.b]", filtered) shouldHaveSize BOOKS
        }

        test("nest filters and existence tests") {
            select("$[?@.*]", filtered) shouldHaveSize 2
            select("$[?@[?@.b]]", filtered) shouldHaveSize 1
            select("$.o[?@<3, ?@<3]", filtered) shouldBe nodesOf("1", "2", "1", "2")
        }

        test("combine tests with logical operators") {
            select("$.a[?@<2 || @.b == \"k\"]", filtered) shouldBe nodesOf("1", "{b: k}")
            select("$.o[?@>1 && @<4]", filtered) shouldBe nodesOf("2", "3")
            select("$.o[?@.u || @.x]", filtered) shouldBe nodesOf("{u: 6}")
            select("$.a[?!(@ > 2)]", filtered) shouldHaveSize WITHOUT_B
            select("$.a[?!@.b]", filtered) shouldHaveSize WITHOUT_B
        }

        test("treat two missing values as equal") {
            select("$.a[?@.b == $.x]", filtered) shouldHaveSize WITHOUT_B
            select("$.a[?@ == @]", filtered) shouldHaveSize ITEMS
        }

        test("compare every kind of value") {
            val values = nodeOf("[1, 1.0, '1', true, null, [1], {a: 1}, ~]")

            select("$[?@ == 1]", values) shouldBe nodesOf("1", "1.0")
            select("$[?@ == '1']", values) shouldBe nodesOf("'1'")
            select("$[?@ == true]", values) shouldBe nodesOf("true")
            select("$[?@ == null]", values) shouldHaveSize 2
            select("$[?@ != null]", values) shouldHaveSize NOT_NULL
            select("$[?@ <= 1]", values) shouldBe nodesOf("1", "1.0")
            select("$[?@ >= 'a']", values).shouldBeEmpty()
            select("$[?@ == $[5]]", values) shouldBe nodesOf("[1]")
            select("$[?@ == $[6]]", values) shouldBe nodesOf("{a: 1}")
        }

        test("order strings by their code points") {
            val words = nodeOf("""["b", "a", "ab", "\U0001F600", "\uFFFD"]""")

            select("$[?@ < 'b']", words) shouldBe nodesOf("a", "ab")
            select("$[?@ > '\\uFFFD']", words) shouldBe nodesOf(""""\U0001F600"""")
        }
    }

    context("functions") {
        val people = nodeOf(
            lines(
                """[ {"name": "ann", "tags": ["a", "b", "c"], "color": "red"},""",
                """{"name": "bob", "tags": [], "nested": {"color": "red"}},""",
                """{"name": "eve", "tags": ["x"], "a": {"color": "blue"}, "b": {"color": "red"}} ]""",
            ),
        )

        test("length counts characters, items and entries") {
            select("$[?length(@.tags) >= 3].name", people) shouldBe nodesOf("ann")
            select("$[?length(@.name) == 3].name", people) shouldHaveSize PEOPLE
            select("$[?length(@) == 3].name", people) shouldBe nodesOf("ann", "bob")
            select("$[?length(1) == 1]", people).shouldBeEmpty()
        }

        test("count counts the nodes of a query") {
            select("$[?count(@.tags[*]) == 0].name", people) shouldBe nodesOf("bob")
        }

        test("value takes the only node of a query") {
            select("$[?value(@..color) == 'red'].name", people) shouldBe nodesOf("ann", "bob")
        }

        test("match tests the whole text and search any part") {
            select("$[?match(@.name, 'a.n')].name", people) shouldBe nodesOf("ann")
            select("$[?search(@.name, 'b')].name", people) shouldBe nodesOf("bob")
            select("$[?match(@.name, '[a-z]+')].name", people) shouldHaveSize PEOPLE
            select("$[?match(@.name, '(')].name", people).shouldBeEmpty()
            select("$[?match(@.tags, 'a')].name", people).shouldBeEmpty()
            select("$[?!search(@.name, $[0].name)].name", people) shouldBe nodesOf("bob", "eve")
        }

        test("a dot of a pattern does not match a line break") {
            val texts = nodeOf("['a\\nb', 'a.b']")

            select("$[?match(@, 'a.b')]", texts) shouldBe nodesOf(DOTTED)
            select("$[?match(@, 'a[.]b')]", texts) shouldBe nodesOf(DOTTED)
            select("$[?match(@, 'a\\\\.b')]", texts) shouldBe nodesOf(DOTTED)
        }

        test("a logical expression can be negated in parentheses") {
            select("$[?!(match(@.name, 'ann') || match(@.name, 'bob'))].name", people) shouldBe nodesOf("eve")
        }
    }

    context("strings and names") {
        test("decode escapes in names") {
            val node = nodeOf(
                """{"a'b": 1, "\u00e9": 2, "\U0001F600": 3, "tab\t": 4, "q\"": 5, "_x9": 6}""",
            )

            select("$['a\\'b']", node) shouldBe nodesOf("1")
            select("""$["\u00e9"]""", node) shouldBe nodesOf("2")
            select("$.é", node) shouldBe nodesOf("2")
            select("$['\\uD83D\\uDE00']", node) shouldBe nodesOf("3")
            select("$['tab\\t']", node) shouldBe nodesOf("4")
            select("""$["q\""]""", node) shouldBe nodesOf("5")
            select("$._x9", node) shouldBe nodesOf("6")
            select("""$['\b\f\n\r\/\\']""", node).shouldBeEmpty()
        }

        test("accept whitespace between the parts of a query") {
            select("$ .store [ 'bicycle' ] .color") shouldBe nodesOf("red")
            JsonPath.parseOrNull("$.store. color").shouldBeNull()
            select("$..book[ ?  @.price  <  9 ] .title") shouldHaveSize 2
            select("$[ 1 : 3 : 1 ]", letters) shouldBe nodesOf("b", "c")
        }

        test("accept number literals with fractions and exponents") {
            val numbers = nodeOf("[100, 0.5, -2]")

            select("$[?@ == 1e2]", numbers) shouldBe nodesOf("100")
            select("$[?@ == 5E-1]", numbers) shouldBe nodesOf("0.5")
            select("$[?@ == -2.0]", numbers) shouldBe nodesOf("-2")
            select("$[?@ > -0]", numbers) shouldHaveSize 2
            select("$[?@ == 1e+2]", numbers) shouldBe nodesOf("100")
        }
    }

    context("parsing") {
        test("accepts every valid query") {
            listOf(
                "$",
                "$.*",
                "$..*",
                "$..[0]",
                "$[*, 1, 'a', 1:2, ?@]",
                "$[?@.a==true]",
                "$[?@.a==false || @.b==null]",
                "$[?$.a]",
                "$[?(@.a)]",
                "$[?count(@..*) > 1]",
                "$[?match(@.timezone, 'Europe/.*')]",
                "$[-9007199254740991]",
            ).forEach { expression -> JsonPath.parseOrNull(expression).shouldNotBeNull() }
        }

        test("refuses every invalid query") {
            listOf(
                "",
                "@.a",
                "$.",
                "$..",
                "$ ",
                "$[",
                "$[]",
                "$[01]",
                "$[-0]",
                "$[9007199254740992]",
                "$['a'",
                "$['a",
                "$['\\x']",
                "$['\\uD83D']",
                "$['\\uDE00']",
                "$['\\uD83Da']",
                "$['\\u00G0']",
                "$['\\u00g0']",
                "$['\\u00@0']",
                "$['\\u00:0']",
                "$['\\u00/0']",
                "$[?length(!@) == 1]",
                "$[?length((@)) == 1]",
                "$.\uD800a",
                "$['\\u\u0660\u0660\u0664\u0661']",
                "$['\\u\uFF10\uFF10\uFF14\uFF41']",
                "$['\u0001']",
                "$.1a",
                "$[?1]",
                "$[?true]",
                "$[?foo(@)]",
                "$[?Length(@)]",
                "$[?length(@.*) < 3]",
                "$[?count(1) == 1]",
                "$[?length(@)]",
                "$[?match(@.a, 'x') == true]",
                "$[?value(@..color)]",
                "$[?@.* == 1]",
                "$[?@ == 01]",
                "$[?@ == 1.]",
                "$[?@ == 1e]",
                "$[?@ == truex]",
                "$[?count(@, @) == 1]",
                "$[?count() == 1]",
                "$[?count(@ == 1) == 1]",
                "$[?(@.a]",
                "$[?!1]",
            ).forEach { expression -> JsonPath.parseOrNull(expression).shouldBeNull() }
        }

        test("names the offset of the problem") {
            val error = shouldThrow<IllegalArgumentException> { JsonPath.parse("$[01]") }

            error.message shouldBe
                "\"$[01]\" is not a JSONPath query: expected an integer without leading zeros at offset 4"
        }

        test("parses through the string extensions") {
            "$.a".toJsonPath() shouldBe JsonPath.parse("$.a")
            "$.a".toJsonPath().hashCode() shouldBe "$.a".hashCode()
            "$.a".toJsonPath().toString() shouldBe "$.a"
            "$.".toJsonPathOrNull().shouldBeNull()
        }
    }

    context("cycles") {
        test("descendants visit a shared node once per path but never loop") {
            val node = nodeOf("a: &x [1, 2]\nb: *x")

            select("$..[0]", node) shouldBe nodesOf("1", "1")
        }
    }

    context("edges") {
        test("refuses incomplete and ill-typed queries") {
            listOf(
                "$[?",
                "$[?!",
                "$['a\\",
                "$['\\uD83D\\u0041']",
                "$['\\u12",
                "$[?@ == -]",
                "$[99999999999999999999]",
                "$[?~]",
                "$[?a_1(@)]",
                "$[?a{",
                "$[?@ ==",
                "$[?@==true",
                "$[?length(!@.a) == 1]",
                "$[?length((@.a)) == 1]",
                "$[?@..a == 1]",
            ).forEach { expression -> JsonPath.parseOrNull(expression).shouldBeNull() }
        }

        test("reads integers too large for a long as floating point numbers") {
            select("$[?@ > 123456789012345678901]", nodeOf("[1]")).shouldBeEmpty()
        }

        test("compares with missing values and values that have no order") {
            val numbers = nodeOf("[0, 1, 2, true]")

            select("$[?@ <= 1]", numbers) shouldBe nodesOf("0", "1")
            select("$[?@ >= 1]", numbers) shouldBe nodesOf("1", "2")
            select("$[?@.missing < 1]", numbers).shouldBeEmpty()
            select("$[?1 < @.missing]", numbers).shouldBeEmpty()
            select("$[?@ < true]", numbers).shouldBeEmpty()
            select("$[-100]", numbers).shouldBeEmpty()
        }

        test("functions return nothing for missing or mistyped arguments") {
            val node = nodeOf("[{n: 1, s: abc}]")

            select("$[?match(@.s, @.missing)]", node).shouldBeEmpty()
            select("$[?match(@.n, '1')]", node).shouldBeEmpty()
            select("$[?length(@.missing) == 1]", node).shouldBeEmpty()
            select("$[?length(@.n) == 1]", node).shouldBeEmpty()
        }

        test("compares as equal only to another query") {
            val path = JsonPath.parse("$")

            (path == JsonPath.parse("$")) shouldBe true
            path.equals("$") shouldBe false
        }

        test("names members with complex keys by their kind") {
            locations("$.store.bicycle.*") shouldBe listOf("/store/bicycle/color", "/store/bicycle/price")
            val complex = nodeOf("{[a]: 1}") as YamlNode.Mapping
            val matches = JsonPath.parse("$.*").query(complex)
            val match = matches.single()

            match.location.last shouldBe "Sequence"
        }
    }
})
