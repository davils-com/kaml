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

package com.davils.kaml.core

/**
 * Creates a [Yaml] from the settings [configure] makes, such as
 * `yaml { schema = YamlSchema.Json; limits { maximumDepth = 64 } }`.
 *
 * Every setting starts from its default: the core schema, selected by the `%YAML` version of a document, duplicate
 * keys refused, unknown tags kept, [com.davils.kaml.core.limit.YamlLimits.Untrusted] and the default emitter
 * options. [Yaml.Default] is the instance with every default.
 *
 * @param configure the block that changes the settings; it changes nothing by default.
 * @return a [Yaml] with the validated configuration, safe to share between threads.
 * @throws com.davils.kore.core.validation.report.ValidationException if a setting is invalid, such as a limit above
 * its hard ceiling or an indentation out of range; the report names every invalid setting.
 * @since 1.0.0
 */
public fun yaml(configure: YamlBuilder.() -> Unit = {}): Yaml {
    val builder = YamlBuilder()
    builder.configure()
    return Yaml(builder.build())
}
