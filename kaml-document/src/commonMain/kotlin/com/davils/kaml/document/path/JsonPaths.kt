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

package com.davils.kaml.document.path

import com.davils.kaml.core.limit.YamlLimits

/**
 * Parses this text as a JSONPath query of RFC 9535.
 *
 * @receiver the query, such as `$.a[0]`.
 * @param limits the limits the query is parsed with and evaluates under; [YamlLimits.Untrusted] by default.
 * @return the query.
 * @throws IllegalArgumentException if the text is not a query or exceeds a limit; see [JsonPath.parse].
 * @since 1.0.0
 */
public fun String.toJsonPath(limits: YamlLimits = YamlLimits.Untrusted): JsonPath = JsonPath.parse(this, limits)

/**
 * Parses this text as a JSONPath query of RFC 9535 and never throws.
 *
 * @receiver the query, such as `$.a[0]`.
 * @param limits the limits the query is parsed with and evaluates under; [YamlLimits.Untrusted] by default.
 * @return the query, or `null` if the text is not one or exceeds a limit.
 * @since 1.0.0
 */
public fun String.toJsonPathOrNull(limits: YamlLimits = YamlLimits.Untrusted): JsonPath? =
    JsonPath.parseOrNull(this, limits)
