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

package com.davils.kaml.document.path.regexp

internal actual fun categoryTest(category: String): (Int) -> Boolean {
    val pattern = categoryPattern(category)
    return { codePoint -> isMatchedBy(pattern, codePoint) }
}

@JsFun("""(category) => new RegExp("^\\p{" + category + "}$", "u")""")
private external fun categoryPattern(category: String): JsAny

@JsFun("(pattern, codePoint) => pattern.test(String.fromCodePoint(codePoint))")
private external fun isMatchedBy(pattern: JsAny, codePoint: Int): Boolean
