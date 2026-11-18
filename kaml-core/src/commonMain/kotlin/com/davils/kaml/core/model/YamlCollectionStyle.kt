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
 * The style a sequence or a mapping is written in.
 *
 * The style is presentation only: it never changes what a collection holds, and two collections that differ only in
 * their style are equal.
 *
 * @since 1.0.0
 */
public enum class YamlCollectionStyle {
    /**
     * The block style, which writes every item or entry on lines of its own, indented below its parent.
     *
     * @since 1.0.0
     */
    Block,

    /**
     * The flow style, which writes a collection in brackets or braces, such as `[a, b]` or `{a: 1}`.
     *
     * @since 1.0.0
     */
    Flow,
}
