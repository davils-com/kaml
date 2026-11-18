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

package com.davils.kaml.document.patch.notation

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.toJsonPointerOrNull

internal class OperationFields(item: YamlNode) {
    val path: JsonPointer? = item["path"].string()?.toJsonPointerOrNull()

    val from: JsonPointer? = item["from"].string()?.toJsonPointerOrNull()

    val value: YamlNode? = item["value"]
}
