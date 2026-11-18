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

package com.davils.kaml.document.merge

import com.davils.kore.core.validation.report.ValidationException

/**
 * Builds the [YamlMergeOptions] of a [YamlMerger].
 *
 * @param configure sets how layers are merged.
 * @return the options.
 * @throws ValidationException if a key of a [YamlSequenceMerge.ByKey] is blank or a pointer is not valid; the report
 * lists every problem at once.
 * @since 1.0.0
 */
public fun yamlMergeOptions(configure: YamlMergeOptionsBuilder.() -> Unit): YamlMergeOptions {
    val builder = YamlMergeOptionsBuilder()
    builder.configure()
    return builder.build()
}
