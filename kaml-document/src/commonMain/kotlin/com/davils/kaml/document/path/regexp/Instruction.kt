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

package com.davils.kaml.document.path.regexp

internal sealed interface Instruction {
    class Consume(set: CharSet) : Instruction {
        val set: CharSet = set
    }

    class Split(first: Int, second: Int) : Instruction {
        var first: Int = first

        var second: Int = second
    }

    class Jump(target: Int) : Instruction {
        var target: Int = target
    }

    object Match : Instruction
}
