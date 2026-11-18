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

package com.davils.kaml.document.internal

private const val SUPPLEMENTARY_START: Int = 0x10000
private const val HIGH_SURROGATE_START: Int = 0xD800
private const val LOW_SURROGATE_START: Int = 0xDC00
private const val SURROGATE_BITS: Int = 10

internal object CodePoints {
    fun of(text: String): IntArray {
        val codePoints = IntArray(text.length)
        var count = 0
        var index = 0
        while (index < text.length) {
            val codePoint = at(text, index)
            codePoints[count] = codePoint
            count++
            index += widthOf(codePoint)
        }
        return codePoints.copyOf(count)
    }

    fun count(text: String): Int {
        var count = 0
        var index = 0
        while (index < text.length) {
            index += widthOf(at(text, index))
            count++
        }
        return count
    }

    fun compare(left: String, right: String): Int {
        var leftIndex = 0
        var rightIndex = 0
        var order = 0
        while (order == 0 && leftIndex < left.length && rightIndex < right.length) {
            val leftPoint = at(left, leftIndex)
            val rightPoint = at(right, rightIndex)
            order = leftPoint.compareTo(rightPoint)
            leftIndex += widthOf(leftPoint)
            rightIndex += widthOf(rightPoint)
        }
        val rest = (left.length - leftIndex).compareTo(right.length - rightIndex)
        return order.takeIf { it != 0 } ?: rest
    }

    private fun at(text: String, index: Int): Int {
        val high = text[index]
        val low = text.getOrNull(index + 1)
        val isPair = high.isHighSurrogate() && low != null && low.isLowSurrogate()
        if (!isPair) return high.code
        val upper = high.code - HIGH_SURROGATE_START shl SURROGATE_BITS
        return SUPPLEMENTARY_START + upper + (low.code - LOW_SURROGATE_START)
    }

    private fun widthOf(codePoint: Int): Int = 2.takeIf { codePoint >= SUPPLEMENTARY_START } ?: 1
}
