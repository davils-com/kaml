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
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

internal const val REFUSED: String = "forbidden-router"

@Serializable(with = ManualRouter.Serializer::class)
internal data class ManualRouter(val name: String, val router: Device.Router) {
    object Serializer : KSerializer<ManualRouter> {
        override val descriptor: SerialDescriptor = Device.serializer().descriptor

        override fun serialize(encoder: Encoder, value: ManualRouter) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.name)
                encodeSerializableElement(
                    descriptor = descriptor,
                    index = 1,
                    serializer = Device.Router.serializer(),
                    value = value.router,
                )
            }
        }

        override fun deserialize(decoder: Decoder): ManualRouter = decoder.decodeStructure(descriptor) {
            decodeElementIndex(descriptor)
            val name = decodeStringElement(descriptor, 0)
            decodeElementIndex(descriptor)
            val router = decodeSerializableElement(descriptor, 1, Device.Router.serializer())
            require(name != REFUSED) { "The type $name is refused after its value" }
            ManualRouter(name, router)
        }
    }
}
