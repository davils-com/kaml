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

/**
 * Builds [YamlFormatOptions], starting from the defaults of [YamlFormatOptions.Default].
 *
 * ```kotlin
 * val options = yamlFormatOptions {
 *     indent = 4
 *     maximumEmptyLines = 0
 * }
 * ```
 *
 * @param configure sets the options that differ from the defaults.
 * @return the validated options.
 * @throws com.davils.kore.core.validation.report.ValidationException if an option is out of its range; its report
 * names every invalid option.
 * @since 1.0.0
 */
public fun yamlFormatOptions(configure: YamlFormatOptionsBuilder.() -> Unit): YamlFormatOptions {
    val builder = YamlFormatOptionsBuilder()
    builder.configure()
    return builder.build()
}
