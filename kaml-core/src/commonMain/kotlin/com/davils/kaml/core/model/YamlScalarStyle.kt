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

package com.davils.kaml.core.model

/**
 * The style a scalar is written in.
 *
 * The style is presentation only: two scalars with the same tag and value are equal whatever their styles. A style
 * may still decide the tag a scalar resolves to, because only a plain scalar is resolved by its text, such as `1` to
 * an integer, while a quoted scalar is a string.
 *
 * @since 1.0.0
 */
public enum class YamlScalarStyle {
    /**
     * The plain style, without quotes, such as `text`.
     *
     * @since 1.0.0
     */
    Plain,

    /**
     * The single-quoted style, such as `'text'`, which escapes nothing but the quote itself.
     *
     * @since 1.0.0
     */
    SingleQuoted,

    /**
     * The double-quoted style, such as `"text"`, which may hold escapes such as `\n` and so any character.
     *
     * @since 1.0.0
     */
    DoubleQuoted,

    /**
     * The literal block style `|`, which keeps every line break of the text.
     *
     * @since 1.0.0
     */
    Literal,

    /**
     * The folded block style `>`, which folds single line breaks of the text into spaces.
     *
     * @since 1.0.0
     */
    Folded,
}
