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

module com.davils.kaml.core {
    requires transitive kotlin.stdlib;
    requires transitive com.davils.kore.core;

    exports com.davils.kaml.core;
    exports com.davils.kaml.core.diagnostic;
    exports com.davils.kaml.core.emit;
    exports com.davils.kaml.core.event;
    exports com.davils.kaml.core.limit;
    exports com.davils.kaml.core.model;
    exports com.davils.kaml.core.node;
    exports com.davils.kaml.core.schema;
    exports com.davils.kaml.core.source;
    exports com.davils.kaml.core.syntax;
    exports com.davils.kaml.core.syntax.ast;
    exports com.davils.kaml.core.syntax.edit;
    exports com.davils.kaml.core.syntax.format;
}
