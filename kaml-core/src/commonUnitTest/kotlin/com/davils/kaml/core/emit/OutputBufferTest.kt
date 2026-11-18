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

package com.davils.kaml.core.emit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val LIMIT: Int = 4
private const val LONG: String = "abcde"

class OutputBufferTest : FunSpec({
    context("a strict buffer") {
        test("holds text up to its limit") {
            val buffer = OutputBuffer(LIMIT)
            buffer.append("ab")
            buffer.append('c')
            buffer.spaces(1)

            buffer.toString() shouldBe "abc "
        }

        test("throws as soon as the text would pass its limit") {
            listOf<(OutputBuffer) -> Unit>(
                { buffer -> buffer.append(LONG) },
                { buffer -> buffer.append(LONG, start = 0, end = LIMIT + 1) },
                { buffer -> buffer.spaces(LIMIT + 1) },
                { buffer ->
                    buffer.append("abcd")
                    buffer.append('e')
                },
            ).forEach { write ->
                shouldThrow<IllegalStateException> { write(OutputBuffer(LIMIT)) }.message shouldBe OutputBuffer.TOO_LONG
            }
        }
    }

    context("a lenient buffer") {
        test("stops at its limit, remembers it and keeps what fitted") {
            val buffer = OutputBuffer(LIMIT, isStrict = false)
            buffer.append("abc")
            buffer.append(LONG, start = 0, end = 2)
            buffer.append('x')
            buffer.spaces(1)
            buffer.append("")

            buffer.isOverflowed shouldBe true
            buffer.toString() shouldBe "abc"
        }

        test("stays below its limit without overflowing") {
            val buffer = OutputBuffer(LIMIT, isStrict = false)
            buffer.append('a')
            buffer.spaces(2)
            buffer.append(LONG, start = 1, end = 2)

            buffer.isOverflowed shouldBe false
            buffer.toString() shouldBe "a  b"
        }
    }
})
