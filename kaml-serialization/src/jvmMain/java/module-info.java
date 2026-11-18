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

module com.davils.kaml.serialization {
    requires transitive kotlin.stdlib;
    requires transitive com.davils.kaml.core;
    requires transitive kotlinx.serialization.core;
    requires transitive kotlinx.serialization.json;

    exports com.davils.kaml.serialization;
    exports com.davils.kaml.serialization.annotation;
    exports com.davils.kaml.serialization.json;
}
