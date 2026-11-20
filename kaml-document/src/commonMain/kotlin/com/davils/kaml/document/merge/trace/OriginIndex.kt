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

@file:OptIn(DavilsInternal::class)

package com.davils.kaml.document.merge.trace

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedIndex

internal class OriginIndex(origins: Map<JsonPointer, String>) {
    private val edges = OrderedIndex<String>()
    private val originsAt = arrayListOf<String?>(null)

    init {
        origins.forEach { (pointer, origin) -> originsAt[locationOf(pointer)] = origin }
    }

    fun originOf(pointer: JsonPointer): String? {
        var location = 0
        var origin = originsAt[location]
        val tokens = pointer.tokens.iterator()
        while (tokens.hasNext()) {
            val edge = edges.ordinalOf(edgeOf(location, tokens.next())) ?: return origin
            location = edge + 1
            origin = originsAt[location] ?: origin
        }
        return origin
    }

    private fun locationOf(pointer: JsonPointer): Int {
        var location = 0
        pointer.tokens.forEach { token ->
            val edge = edges.ordinalFor(edgeOf(location, token))
            if (edge + 1 == originsAt.size) originsAt.add(null)
            location = edge + 1
        }
        return location
    }

    private fun edgeOf(location: Int, token: String): String = "$location/$token"
}
