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
 * Lets the property also be read from the keys [names], next to its own YAML name. Encoding always writes the own
 * name.
 *
 * An alias that equals the YAML name of another property, or an alias of another property, is a mistake in the class
 * and fails with an [IllegalStateException] when the class is encoded or decoded, instead of letting one of them win
 * silently. A document that gives the same property under two of its names is refused with
 * [com.davils.kaml.core.diagnostic.YamlDiagnosticCode.DuplicateKey].
 *
 * @property names the alternative names of the property.
 * @since 1.0.0
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class YamlNames(vararg val names: String)
