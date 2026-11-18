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

package com.davils.kaml.core.limit

/**
 * Builds limits that start from [base] and change what [configure] sets, such as
 * `yamlLimits(YamlLimits.Relaxed) { maximumDepth = 64 }`.
 *
 * @param base the limits every property starts from; [YamlLimits.Untrusted] by default.
 * @param configure the block that sets the limits to change.
 * @return the validated limits.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit is below 1 or above its hard ceiling
 * of [YamlLimits]; the report names every such limit.
 * @since 1.0.0
 */
public fun yamlLimits(base: YamlLimits = YamlLimits.Untrusted, configure: YamlLimitsBuilder.() -> Unit): YamlLimits {
    val builder = YamlLimitsBuilder()
    builder.from(base)
    builder.configure()
    return builder.build()
}
