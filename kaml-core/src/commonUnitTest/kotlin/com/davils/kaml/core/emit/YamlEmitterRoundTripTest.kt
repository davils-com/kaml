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

package com.davils.kaml.core.emit

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.fixture.root
import com.davils.kaml.core.fixture.text
import com.davils.kaml.core.model.YamlCollectionStyle
import com.davils.kaml.core.model.YamlScalarStyle
import com.davils.kaml.core.model.YamlVersion
import com.davils.kaml.core.node.YamlDocument
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.schema.yamlSchema
import com.davils.kaml.core.yaml
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val LONGEST_TEXT: Int = 12
private const val ITERATIONS: Int = 300

private val yamlFragments: List<String> = listOf(
    "\n---\n", "---", "--- ", "...", "\n...\n", "# ", "#", " #", ": ", ":", "- ", "-", "? ", "?", "&a", "*a", "!t",
    "!!str", "%YAML 1.2", "%TAG ! x", "%", "|", ">", "|-", "|+", ">2", "  ", "\t", "\uFEFF", "\u0085", "\u2028",
    "\u2029", "\uD800", "\uDC00", "\"", "'", "\\", "{", "}", ",", "@", "`", "<<", "=", "~", "null", "true", "yes",
    "on", "0x1F", "012", "1:00", "1e3", ".5", ".nan", "\r\n", "\r", "a", "b c",
)

private val fragments: List<String> = HostileFragments.all + yamlFragments

private val hostileText: Arb<String> = Arb.list(Arb.element(fragments), 0..LONGEST_TEXT).map { parts ->
    parts.joinToString(separator = "")
}

private val setups: List<EmitSetup> = YamlScalarStyle.entries.flatMap { style ->
    YamlQuoteStyle.entries.flatMap { quoteStyle ->
        listOf(YamlVersion.V1_1, YamlVersion.V1_2).flatMap { compatibility ->
            listOf(true, false).map { isStylePreserved ->
                EmitSetup(
                    style = style,
                    quoteStyle = quoteStyle,
                    compatibility = compatibility,
                    isStylePreserved = isStylePreserved,
                )
            }
        }
    }
}

private val legacy: Yaml = yaml { schema = YamlSchema.Yaml11 }

private val words: YamlSchema = yamlSchema {
    scalar("!word") { implicit { text -> text.isNotEmpty() && text.all { it in 'a'..'z' } } }
}

private val schemas: List<YamlSchema> =
    listOf(YamlSchema.Failsafe, YamlSchema.Json, YamlSchema.Core, YamlSchema.Yaml11, words)

private val readers: List<Yaml> = schemas.flatMap { schema ->
    listOf(YamlVersion.V1_1, YamlVersion.V1_2).map { version ->
        yaml {
            this.schema = schema
            emitter { compatibility = version }
        }
    }
}

private val legacyTexts: List<String> = listOf(
    "yes", "No", "on", "OFF", "y", "n", "Y", "0777", "1_000", "1:20", "0b11", "<<", "=", "2001-12-14",
    "2001-12-14t21:59:43.10-05:00", ".Inf", "0x_1F", "+12", "~", "Null", "190:20:30.15", "1.", ".5", "1e3",
    "12e03", "-0", "0o17", "+.inf", "1__0", "0.", "-1:30", "0b", "0x", "1,000", "NULL", "TRUE", "+0.5e+3",
)

class YamlEmitterRoundTripTest : FunSpec({
    context("the schema that reads the text") {
        test("reads back the same graph for every fragment, whatever schema the configuration has") {
            readers.forEach { reader ->
                fragments.filterNot(UnsafeCharacters::hasLoneSurrogate).forEach { fragment ->
                    readBack(reader, YamlDocument(graphOf(key = fragment, value = fragment, YamlScalarStyle.Plain)))
                }
            }
        }

        test("reads back the same graph in a document that declares YAML 1.1") {
            fragments.filterNot(UnsafeCharacters::hasLoneSurrogate).forEach { fragment ->
                val node = graphOf(key = fragment, value = "a${fragment}b", YamlScalarStyle.Plain)
                readBack(Yaml.Default, YamlDocument(node, version = YamlVersion.V1_1))
            }
        }

        test("reads back every type of YAML 1.1 as the string it is") {
            legacyTexts.forEach { text ->
                val node = yamlMapping {
                    text to text
                    "list" to yamlSequence(listOf(yamlScalar(text)), style = YamlCollectionStyle.Flow)
                }
                readBack(legacy, YamlDocument(node))
                readBack(yaml { emitter { compatibility = YamlVersion.V1_1 } }, YamlDocument(node))
            }
        }

        test("quotes a merge key and the booleans of YAML 1.1 for a reader with the YAML 1.1 schema") {
            val node = yamlMapping {
                "<<" to yamlMapping { "admin" to "true" }
                "name" to "x"
                "flag" to "yes"
            }
            val text = legacy.emit(node)

            text shouldBe text(""""<<":""", """  admin: "true"""", "name: x", """flag: "yes"""", "")
            legacy.root(text) shouldBe node
        }
    }

    context("hostile scalars and keys") {
        test("read back as the same graph for every fragment in every setup") {
            setups.forEach { setup ->
                fragments.forEach { fragment -> roundTrip(setup, key = fragment, value = "a${fragment}b") }
            }
        }

        test("read back as the same graph for combined fragments") {
            val setup = Arb.element(setups)
            checkAll(
                iterations = ITERATIONS,
                genA = hostileText,
                genB = hostileText,
                genC = setup,
            ) { key, value, chosen ->
                roundTrip(chosen, key = key, value = value)
            }
        }
    }
})

private fun roundTrip(setup: EmitSetup, key: String, value: String) {
    val node = graphOf(key = key, value = value, style = setup.style)
    val isRepresentable = !UnsafeCharacters.hasLoneSurrogate(key) && !UnsafeCharacters.hasLoneSurrogate(value)
    if (!isRepresentable) {
        shouldThrow<IllegalArgumentException> { setup.yaml.emit(node) }
        return
    }
    val written = setup.yaml.emit(node)
    val root = setup.yaml.emit(yamlScalar(value, setup.style))
    withClue("$setup wrote:\n$written\n$root") {
        Yaml.Default.root(written) shouldBe node
        Yaml.Default.root(root) shouldBe yamlScalar(value)
        if (setup.compatibility == YamlVersion.V1_1) legacy.root(written) shouldBe node
        (written + root).lines().forEach { line -> unsafeCodePointsIn(line, allowTabs = true).shouldBeEmpty() }
    }
}

private fun readBack(reader: Yaml, document: YamlDocument) {
    val written = reader.emit(document)
    withClue("${reader.configuration.schema} wrote:\n$written") {
        val parsed = reader.parse(written).shouldBeInstanceOf<YamlParseResult.Parsed>()
        val documents = parsed.stream.documents
        documents.single().root shouldBe document.root
    }
}

private fun graphOf(key: String, value: String, style: YamlScalarStyle): YamlNode {
    fun scalar(text: String): YamlNode.Scalar = yamlScalar(text, style)

    fun pair(): YamlNode.Mapping = yamlMapping(listOf(YamlEntry(key = scalar(key), value = scalar(value))))
    val flowItems = listOf(scalar(value), scalar(key), pair())
    val flowEntries = listOf(YamlEntry(key = scalar(key), value = scalar(value)))
    return yamlSequence(
        listOf(
            pair(),
            scalar(value),
            yamlSequence(flowItems, style = YamlCollectionStyle.Flow),
            yamlMapping(flowEntries, style = YamlCollectionStyle.Flow),
            yamlMapping(listOf(YamlEntry(key = scalar(key), value = yamlSequence(listOf(scalar(value), pair()))))),
            yamlMapping(listOf(YamlEntry(key = scalar(key), value = pair()))),
            yamlSequence(listOf(yamlSequence(listOf(scalar(value))), scalar(key))),
        ),
    )
}
