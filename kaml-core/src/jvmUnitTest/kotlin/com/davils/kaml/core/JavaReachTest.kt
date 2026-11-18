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

package com.davils.kaml.core

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.limit.YamlLimitsBuilder
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.internal.NodeFactory
import com.davils.kaml.core.node.internal.NodeIdentity
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.lang.reflect.InvocationTargetException

private const val INTERNAL_PACKAGE: String = "com.davils.kaml.core.node.internal"
private const val TOO_DEEP: Int = 5_000

class JavaReachTest : FunSpec({
    context("a Java caller") {
        test("cannot construct limits beyond a hard ceiling through the constructor") {
            val builder = YamlLimitsBuilder::class.java.getDeclaredConstructor().newInstance()
            builder.maximumDepth = TOO_DEEP
            builder.maximumInputLength = Int.MAX_VALUE
            val constructor = YamlLimits::class.java.getDeclaredConstructor(YamlLimitsBuilder::class.java)
            val failure = shouldThrow<InvocationTargetException> { constructor.newInstance(builder) }
            failure.cause.shouldBeInstanceOf<ValidationException>()
        }

        test("finds the helpers that build and identify nodes only in an internal package") {
            NodeFactory::class.java.packageName shouldBe INTERNAL_PACKAGE
            NodeIdentity::class.java.packageName shouldBe INTERNAL_PACKAGE
        }

        test("needs a type of an internal package for every constructor of a scalar") {
            YamlNode.Scalar::class.java.declaredConstructors.forEach { constructor ->
                val packages = constructor.parameterTypes.map { type -> type.packageName }
                packages shouldContain INTERNAL_PACKAGE
            }
        }
    }
})
