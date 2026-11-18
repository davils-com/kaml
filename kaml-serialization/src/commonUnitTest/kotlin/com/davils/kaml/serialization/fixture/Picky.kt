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
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Picky.Serializer::class)
internal data class Picky(val text: String) {
    object Serializer : KSerializer<Picky> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Picky", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Picky) {
            encoder.encodeString(value.text)
        }

        override fun deserialize(decoder: Decoder): Picky {
            val text = decoder.decodeString()
            if (text == "silent") throw NumberFormatException()
            require(text != "loud") { "loud is not allowed" }
            return Picky(text)
        }
    }
}
