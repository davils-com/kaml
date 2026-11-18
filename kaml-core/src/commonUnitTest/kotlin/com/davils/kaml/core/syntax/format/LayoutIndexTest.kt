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

package com.davils.kaml.core.syntax.format

import com.davils.kore.core.text.TextRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val FIRST: Int = 1
private const val TWICE: Int = 3
private const val LAST: Int = 7
private const val LAST_INDEX: Int = 3
private const val OUTER_SHIFT: Int = 2
private const val INNER_SHIFT: Int = -1
private const val INNER_START: Int = 4
private const val INNER_END: Int = 8
private const val OUTER_END: Int = 12
private const val NEXT_START: Int = 20
private const val NEXT_END: Int = 30
private const val NEXT_SHIFT: Int = 5

class LayoutIndexTest : FunSpec({
    context("sorted offsets") {
        test("find the last offset at or before another") {
            val starts = intArrayOf(FIRST, TWICE, TWICE, LAST)

            SortedOffsets.lastAtOrBefore(starts, FIRST - 1) shouldBe -1
            SortedOffsets.lastAtOrBefore(starts, FIRST) shouldBe 0
            SortedOffsets.lastAtOrBefore(starts, TWICE) shouldBe 2
            SortedOffsets.lastAtOrBefore(starts, TWICE + 1) shouldBe 2
            SortedOffsets.lastAtOrBefore(starts, LAST + 1) shouldBe LAST_INDEX
            SortedOffsets.lastAtOrBefore(IntArray(0), LAST) shouldBe -1
        }
    }

    context("range indexes") {
        test("find the range that contains an offset") {
            val index = RangeIndex(
                listOf(
                    TextRange(start = NEXT_START, end = NEXT_END),
                    TextRange(start = 0, end = INNER_START),
                    TextRange.at(2),
                ),
            )

            index.containing(0) shouldBe TextRange(start = 0, end = INNER_START)
            index.containing(2) shouldBe TextRange(start = 0, end = INNER_START)
            index.containing(INNER_START) shouldBe null
            index.containing(NEXT_START) shouldBe TextRange(start = NEXT_START, end = NEXT_END)
            index.containing(-1) shouldBe null
            RangeIndex(emptyList()).containing(0) shouldBe null
        }
    }

    context("shift indexes") {
        test("find the shift of the innermost range that contains an offset") {
            val index = ShiftIndex(
                listOf(
                    ShiftIndex.Shift(TextRange(start = INNER_START, end = INNER_END), INNER_SHIFT),
                    ShiftIndex.Shift(TextRange(start = 0, end = OUTER_END), OUTER_SHIFT),
                    ShiftIndex.Shift(TextRange(start = NEXT_START, end = NEXT_END), NEXT_SHIFT),
                ),
            )

            index.shiftAt(0) shouldBe OUTER_SHIFT
            index.shiftAt(INNER_START) shouldBe INNER_SHIFT
            index.shiftAt(INNER_END) shouldBe OUTER_SHIFT
            index.shiftAt(OUTER_END) shouldBe 0
            index.shiftAt(NEXT_START) shouldBe NEXT_SHIFT
            index.shiftAt(NEXT_END) shouldBe 0
            index.shiftAt(-1) shouldBe 0
        }

        test("prefer the inner of two ranges that start together") {
            val index = ShiftIndex(
                listOf(
                    ShiftIndex.Shift(TextRange(start = 0, end = INNER_END), INNER_SHIFT),
                    ShiftIndex.Shift(TextRange(start = 0, end = OUTER_END), OUTER_SHIFT),
                ),
            )

            index.shiftAt(0) shouldBe INNER_SHIFT
            index.shiftAt(INNER_END) shouldBe OUTER_SHIFT
        }
    }
})
