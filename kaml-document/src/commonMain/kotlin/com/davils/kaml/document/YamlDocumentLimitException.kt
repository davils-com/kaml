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

package com.davils.kaml.document

/**
 * Thrown when a piece of work of kaml-document needs more than one of the [com.davils.kaml.core.limit.YamlLimits]
 * it runs under allows, such as the steps or results of a JSONPath query, the depth of a walk or the nodes a merge
 * or a diff creates.
 *
 * The work stops at the first excess and returns nothing: a query never answers with a part of its result, and a
 * merge never with a part of its layers. The message names only the limit, never the input.
 *
 * @property limit the name of the property of [com.davils.kaml.core.limit.YamlLimits] that was exceeded, such as
 * `maximumQuerySteps`.
 * @since 1.0.0
 */
public class YamlDocumentLimitException internal constructor(public val limit: String) :
    IllegalStateException("The work needs more than the limit $limit of YamlLimits allows")
