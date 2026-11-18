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

import com.davils.kaml.core.model.YamlScalarStyle
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Asks for the scalar style [scalar] when the string or character the property holds is written. The emitter falls
 * back to a style that can hold the value when the requested one cannot, so the text always reads back to the same
 * value.
 *
 * @property scalar the style to write the value in, such as [YamlScalarStyle.Literal] for a script.
 * @since 1.0.0
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class YamlStyle(val scalar: YamlScalarStyle)
