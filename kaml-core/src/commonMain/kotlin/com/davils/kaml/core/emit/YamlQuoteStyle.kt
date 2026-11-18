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
 * The quotes the emitter prefers for a scalar that cannot be written plain, see [YamlEmitOptions.quoteStyle].
 *
 * @since 1.0.0
 */
public enum class YamlQuoteStyle {
    /**
     * Single quotes, as in `'yes'`, which double a quote inside and cannot escape anything. A scalar with a line break
     * or a character that must be escaped is written in double quotes instead.
     *
     * @since 1.0.0
     */
    Single,

    /**
     * Double quotes, as in `"yes"`, which escape every character that cannot be written as it is.
     *
     * @since 1.0.0
     */
    Double,
}
