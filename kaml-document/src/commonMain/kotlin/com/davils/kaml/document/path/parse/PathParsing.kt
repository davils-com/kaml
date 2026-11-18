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

package com.davils.kaml.document.path.parse

import com.davils.kaml.core.limit.YamlLimits

internal object PathParsing {
    fun read(expression: String, limits: YamlLimits): PathReading {
        if (expression.length > limits.maximumQueryLength) return PathReading.Refused(0, "the expression is too long")
        return parsed(expression, limits)
    }

    private fun parsed(expression: String, limits: YamlLimits): PathReading = try {
        val cursor = PathCursor(expression, limits.maximumQueryNesting)
        PathReading.Read(PathParser(cursor, limits.maximumNumberLength).wholeQuery())
    } catch (error: PathSyntaxError) {
        PathReading.Refused(error.offset, error.reason)
    }
}
