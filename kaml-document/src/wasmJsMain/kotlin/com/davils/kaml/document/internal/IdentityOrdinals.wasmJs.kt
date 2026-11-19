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

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.davils.kaml.document.internal

import com.davils.kaml.core.node.YamlNode

internal actual fun identityOrdinals(): (YamlNode) -> Int {
    val ordinals = identityTable()
    return { node -> ordinalIn(ordinals, node.toJsReference()) }
}

@JsFun("() => new Map()")
private external fun identityTable(): JsAny

@JsFun(
    """(table, key) => {
        let ordinal = table.get(key);
        if (ordinal === undefined) {
            ordinal = table.size;
            table.set(key, ordinal);
        }
        return ordinal;
    }""",
)
private external fun ordinalIn(table: JsAny, key: JsReference<YamlNode>): Int
