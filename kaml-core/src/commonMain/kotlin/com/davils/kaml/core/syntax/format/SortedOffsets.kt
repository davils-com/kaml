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

internal object SortedOffsets {
    fun lastAtOrBefore(starts: IntArray, offset: Int): Int {
        var low = 0
        var high = starts.size
        while (low < high) {
            val middle = low + (high - low) / 2
            val isAtOrBefore = starts[middle] <= offset
            low = (middle + 1).takeIf { isAtOrBefore } ?: low
            high = high.takeIf { isAtOrBefore } ?: middle
        }
        return low - 1
    }
}
