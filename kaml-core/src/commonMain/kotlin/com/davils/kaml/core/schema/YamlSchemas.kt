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
package com.davils.kaml.core.schema

/**
 * Builds a [YamlSchema] that extends [base] with the tags declared in [configure].
 *
 * The matchers declared in [configure] are caller code that kaml runs on scalars of documents that may come from an
 * attacker, as [YamlSchema] describes. Tags never construct types: a tag only selects one of the declared definitions.
 *
 * @param base the schema whose resolution of plain scalars and whose tags the new schema keeps; [YamlSchema.Core]
 * unless given.
 * @param configure declares the tags, the name and the merge keys of the schema.
 * @return the validated schema, which is immutable.
 * @throws com.davils.kore.core.validation.report.ValidationException if a declared tag is blank, is a core tag or is
 * declared twice.
 * @since 1.0.0
 */
public fun yamlSchema(base: YamlSchema = YamlSchema.Core, configure: YamlSchemaBuilder.() -> Unit): YamlSchema {
    val builder = YamlSchemaBuilder(base)
    builder.configure()
    return builder.build()
}
