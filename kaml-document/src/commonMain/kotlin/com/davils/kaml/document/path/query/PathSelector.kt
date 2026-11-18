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

package com.davils.kaml.document.path.query

import com.davils.kaml.document.path.filter.FilterExpression

internal sealed interface PathSelector {
    data class Name(val name: String) : PathSelector

    data object Wildcard : PathSelector

    data class Index(val index: Long) : PathSelector

    data class Slice(val start: Long?, val end: Long?, val step: Long?) : PathSelector

    data class Filter(val expression: FilterExpression) : PathSelector
}
