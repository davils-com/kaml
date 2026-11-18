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

@Serializable(with = Port.Serializer::class)
internal data class Port(val number: Int) {
    object Serializer : KSerializer<Port> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Port", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Port) {
            encoder.encodeInline(descriptor).encodeInt(value.number)
        }

        override fun deserialize(decoder: Decoder): Port = Port(decoder.decodeInline(descriptor).decodeInt())
    }
}
