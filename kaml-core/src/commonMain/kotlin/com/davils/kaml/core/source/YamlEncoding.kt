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
package com.davils.kaml.core.source

/**
 * The encodings of YAML 1.2 that kaml decodes from bytes.
 *
 * The encoding of a byte source is detected from its byte order mark or from the zero bytes among its first four
 * bytes; a source without either is UTF-8.
 *
 * @since 1.0.0
 */
public enum class YamlEncoding {
    /**
     * UTF-8, the default.
     *
     * @since 1.0.0
     */
    Utf8,

    /**
     * UTF-16 with the low byte first.
     *
     * @since 1.0.0
     */
    Utf16LittleEndian,

    /**
     * UTF-16 with the high byte first.
     *
     * @since 1.0.0
     */
    Utf16BigEndian,

    /**
     * UTF-32 with the low byte first.
     *
     * @since 1.0.0
     */
    Utf32LittleEndian,

    /**
     * UTF-32 with the high byte first.
     *
     * @since 1.0.0
     */
    Utf32BigEndian,
}
