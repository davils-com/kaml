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

package com.davils.kaml.document.patch

import com.davils.kaml.document.fixture.nodeOf
import com.davils.kaml.document.fixture.requireConsistent
import com.davils.kaml.document.fixture.treeOf

internal fun patchedText(text: String, patch: String, document: Int = 0): String {
    val edit = patchedSyntax(text, patch, document) as JsonPatchEdit.Edited
    requireConsistent(text, edit.edits, edit.tree.text)
    return edit.tree.text
}

internal fun patchedSyntax(text: String, patch: String, document: Int = 0): JsonPatchEdit {
    val reading = JsonPatch.read(nodeOf(patch)) as JsonPatchReading.Read
    return reading.patch.apply(treeOf(text), document)
}

internal fun mergedText(text: String, patch: String): String {
    val edit = JsonMergePatch(nodeOf(patch)).apply(treeOf(text)) as JsonPatchEdit.Edited
    requireConsistent(text, edit.edits, edit.tree.text)
    return edit.tree.text
}
