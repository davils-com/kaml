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

@Serializable(with = Fatal.Serializer::class)
internal data class Fatal(val text: String) {
    object Serializer : KSerializer<Fatal> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Fatal", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Fatal) {
            encoder.encodeString(value.text)
        }

        override fun deserialize(decoder: Decoder): Fatal {
            val text = decoder.decodeString()
            if (text == "error") throw AssertionError("The serializer broke on $text")
            check(text != "state") { "The serializer is in the wrong state for $text" }
            return Fatal(text)
        }
    }
}
