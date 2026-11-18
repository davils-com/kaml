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

package com.davils.kaml.core.emit

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlNode

internal fun Yaml.emitValue(node: YamlNode, version: YamlVersion?): String {
    val document = Representer(configuration).document(YamlDocument(node), version)
    return YamlWriter(configuration.emitter, configuration.limits).write(listOf(document))
}
