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

package com.davils.kaml.document.path.evaluate

import com.davils.kaml.document.path.query.PathSelector

internal object SliceIndices {
    fun each(slice: PathSelector.Slice, size: Int, visit: (Int) -> Unit) {
        val step = slice.step ?: 1L
        if (step == 0L) return
        val bounds = Bounds(slice, step, size.toLong())
        var index = bounds.first
        while (bounds.contains(index)) {
            visit(index.toInt())
            index += step
        }
    }

    private class Bounds(slice: PathSelector.Slice, step: Long, length: Long) {
        private val isForward = step > 0
        private val defaultStart = 0L.takeIf { isForward } ?: length - 1
        private val defaultEnd = length.takeIf { isForward } ?: -length - 1
        private val start = normalized(slice.start ?: defaultStart, length)
        private val end = normalized(slice.end ?: defaultEnd, length)
        private val lower: Long = clamped(forward = start, backward = end, length = length)
        private val upper: Long = clamped(forward = end, backward = start, length = length)

        val first: Long = lower.takeIf { isForward } ?: upper

        fun contains(index: Long): Boolean = (index < upper).takeIf { isForward } ?: (index > lower)

        private fun clamped(forward: Long, backward: Long, length: Long): Long {
            val forwardBound = forward.coerceIn(minimumValue = 0, maximumValue = length)
            val backwardBound = backward.coerceIn(minimumValue = -1, maximumValue = length - 1)
            return forwardBound.takeIf { isForward } ?: backwardBound
        }

        private fun normalized(index: Long, length: Long): Long = (length + index).takeIf { index < 0 } ?: index
    }
}
