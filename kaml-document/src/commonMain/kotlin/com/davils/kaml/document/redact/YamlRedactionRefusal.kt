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

package com.davils.kaml.document.redact

/**
 * Why a redaction refused a syntax tree. A refused tree is never redacted in part.
 *
 * @since 1.0.0
 */
public enum class YamlRedactionRefusal {
    /**
     * The tree does not compose without errors, so what it means, and therefore what is secret in it, cannot be
     * decided.
     *
     * @since 1.0.0
     */
    InvalidDocument,

    /**
     * The redaction needs more than one of its limits allows, such as the steps of a JSONPath, the nodes of the
     * document or the length of a text a caller-supplied pattern would have to read.
     *
     * @since 1.0.0
     */
    LimitExceeded,

    /**
     * The redaction cannot decide how to redact in place, for example because a target has no location in the
     * text, or because the redacted text would no longer compose without errors.
     *
     * @since 1.0.0
     */
    Undecidable,
}
