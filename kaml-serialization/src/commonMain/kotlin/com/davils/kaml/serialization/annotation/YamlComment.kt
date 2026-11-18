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

package com.davils.kaml.serialization.annotation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Writes [lines] as a comment above the property when a [com.davils.kaml.serialization.YamlFormat] encodes it to
 * text. Decoding ignores it, and so does encoding to a node.
 *
 * Each line becomes one comment line; a line break inside a line starts another. A line with a control, separator or
 * bidirectional character is refused with an [IllegalArgumentException] when the property is encoded, so a comment can
 * neither end early and inject YAML nor hide text from a reader.
 *
 * @property lines the lines of the comment, without the leading `#`.
 * @since 1.0.0
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class YamlComment(vararg val lines: String)
