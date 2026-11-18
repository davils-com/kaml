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

/**
 * The line break that ends every line the emitter writes.
 *
 * Either one reads back as the same YAML, since a reader turns both into a line feed.
 *
 * @property text the characters of the line break.
 * @since 1.0.0
 */
public enum class YamlLineBreak(public val text: String) {
    /**
     * A line feed, `U+000A`, as on Linux and macOS.
     *
     * @since 1.0.0
     */
    Lf("\n"),

    /**
     * A carriage return followed by a line feed, `U+000D U+000A`, as on Windows.
     *
     * @since 1.0.0
     */
    CrLf("\r\n"),
}
