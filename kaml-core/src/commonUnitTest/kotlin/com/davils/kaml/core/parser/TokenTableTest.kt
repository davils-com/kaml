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

package com.davils.kaml.core.parser

import com.davils.kaml.core.fixture.collidingText
import com.davils.kaml.core.fixture.distinctText
import com.davils.kaml.core.fixture.timeRatio
import com.davils.kaml.core.syntax.YamlSyntaxKind
import com.davils.kaml.core.syntax.internal.GreenToken
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val FEW: Int = 2_000
private const val MANY: Int = 16_000
private const val MOST_LINEAR_GROWTH: Double = 24.0

private fun sequenceOf(count: Int, scalar: (Int) -> String): String =
    (0 until count).joinToString("") { index -> "- ${scalar(index)}\n" }

private fun read(text: String) {
    SyntaxParser.parse(text)
}

private fun plain(text: String): GreenToken = GreenToken(YamlSyntaxKind.PLAIN_SCALAR, text)

class TokenTableTest : FunSpec({
    context("short tokens whose hash codes collide") {
        test("are parsed in time linear in their number") {
            val few = sequenceOf(FEW, ::collidingText)
            val many = sequenceOf(MANY, ::collidingText)
            timeRatio(small = few, large = many, read = ::read) shouldBeLessThan MOST_LINEAR_GROWTH
        }

        test("keep their text in the syntax tree") {
            val text = sequenceOf(FEW, ::collidingText)
            SyntaxParser.parse(text).root.text shouldBe text
        }
    }

    context("intern") {
        test("returns the token it holds for an equal token") {
            val table = TokenTable()
            val first = plain("a")
            table.intern(first)
            table.intern(plain("a")) shouldBeSameInstanceAs first
        }

        test("keeps the tokens it holds when it grows") {
            val table = TokenTable()
            val first = plain("a")
            table.intern(first)
            for (index in 0 until FEW) {
                val token = plain(distinctText(index))
                table.intern(token) shouldBe token
            }
            table.intern(plain("a")) shouldBeSameInstanceAs first
        }

        test("returns a token it has no room for unchanged") {
            val table = TokenTable()
            for (index in 0 until FEW) {
                table.intern(plain(collidingText(index)))
            }
            val late = plain(collidingText(FEW))
            table.intern(late) shouldBeSameInstanceAs late
            table.intern(plain(collidingText(FEW))) shouldBe late
        }
    }
})
