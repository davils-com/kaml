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

package com.davils.kaml.core.syntax.format

/**
 * Why a [YamlFormatter] refused to format a tree; the tree stays as it is.
 *
 * @since 1.0.0
 */
public enum class YamlFormatRefusal {
    /**
     * The tree has errors, such as syntax errors or an exceeded limit, so its layout cannot be trusted.
     *
     * @since 1.0.0
     */
    SyntaxErrors,

    /**
     * The formatted text would compose to other nodes or other diagnostics than the original text.
     *
     * @since 1.0.0
     */
    MeaningChanged,
}
