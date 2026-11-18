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

internal class ShiftIndex(shifts: List<Shift>) {
    private val segments: List<Segment> = segmentsOf(shifts.sortedWith(outerFirst))
    private val starts: IntArray = IntArray(segments.size) { index -> segments[index].start }

    fun shiftAt(offset: Int): Int {
        val segment = segments.getOrNull(SortedOffsets.lastAtOrBefore(starts, offset)) ?: return 0
        return segment.shift
    }

    private fun segmentsOf(sorted: List<Shift>): List<Segment> {
        val segments = ArrayList<Segment>()
        val open = ArrayList<Shift>()
        sorted.forEach { shift ->
            close(open, segments, before = shift.range.start)
            segments.add(Segment(start = shift.range.start, shift = shift.shift))
            open.add(shift)
        }
        close(open, segments, before = Int.MAX_VALUE)
        return segments
    }

    private fun close(open: MutableList<Shift>, segments: MutableList<Segment>, before: Int) {
        while (open.isNotEmpty() && open.last().range.end <= before) {
            val closed = open.removeAt(open.lastIndex)
            val outer = open.lastOrNull()?.shift ?: 0
            segments.add(Segment(start = closed.range.end, shift = outer))
        }
    }

    internal data class Shift(val range: TextRange, val shift: Int)

    private data class Segment(val start: Int, val shift: Int)

    private companion object {
        private val outerFirst: Comparator<Shift> = compareBy({ it.range.start }, { -it.range.length })
    }
}
