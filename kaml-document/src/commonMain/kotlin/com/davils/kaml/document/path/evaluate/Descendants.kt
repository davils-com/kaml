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

internal class Descendants(private val children: Children) {
    fun visit(start: PathMatch, visit: (PathMatch) -> Unit) {
        val pending = ArrayDeque<PathMatch>()
        pending.addLast(start)
        while (pending.isNotEmpty()) {
            val match = pending.removeLast()
            visit(match)
            val below = children.of(match)
            for (index in below.indices.reversed()) pending.addLast(below[index])
        }
    }
}
