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

package com.davils.kaml.serialization.fixture

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable(with = Span.Serializer::class)
internal data class Span(val start: Int, val end: Int) {
    object Serializer : KSerializer<Span> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Span") {
            element<Int>("start")
            element<Int>("end")
        }

        override fun serialize(encoder: Encoder, value: Span) {
            encoder.encodeStructure(descriptor) {
                encodeInlineElement(descriptor, 0).encodeInt(value.start)
                encodeIntElement(descriptor = descriptor, index = 1, value = value.end)
            }
        }

        override fun deserialize(decoder: Decoder): Span = decoder.decodeStructure(descriptor) {
            decodeElementIndex(descriptor)
            val start = decodeInlineElement(descriptor, 0).decodeInt()
            decodeElementIndex(descriptor)
            Span(start, decodeIntElement(descriptor, 1))
        }
    }
}
