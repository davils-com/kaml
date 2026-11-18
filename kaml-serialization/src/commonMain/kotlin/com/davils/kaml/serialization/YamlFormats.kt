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

package com.davils.kaml.serialization

/**
 * Builds a [YamlFormat] from the options set in [configure].
 *
 * @param configure sets the options of the format on a [YamlFormatBuilder]; nothing by default, which gives a format
 * like [YamlFormat.Default].
 * @return the configured format, which is immutable and safe to share between threads.
 * @throws com.davils.kore.core.validation.report.ValidationException if an option is invalid, such as a blank class
 * discriminator, or if [YamlFormatBuilder.yaml] is given an invalid YAML configuration.
 * @since 1.0.0
 */
public fun yamlFormat(configure: YamlFormatBuilder.() -> Unit = {}): YamlFormat {
    val builder = YamlFormatBuilder()
    builder.configure()
    return YamlFormat(builder.build())
}
