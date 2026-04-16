package com.ugurbuga.stackshift.localization

import com.ugurbuga.stackshift.game.model.AppLanguage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalizedStringsParityTest {
    private val resourcesRoot = sequenceOf(
        File("src/commonMain/composeResources"),
        File("composeApp/src/commonMain/composeResources"),
    ).firstOrNull(File::exists)
        ?: error("Could not locate compose resources directory from the current test working directory.")
    private val baseStringsFile = resourcesRoot.resolve("values/strings.xml")
    private val placeholderRegex = Regex("""%(?:\d+\$)?[a-zA-Z]""")
    private val stringRegex = Regex("""<string\s+name="([^"]+)">([\s\S]*?)</string>""")

    @Test
    fun everySupportedLanguageHasAStringsFile() {
        assertTrue(baseStringsFile.exists(), "Base strings.xml should exist at ${baseStringsFile.path}")

        val missingFolders = AppLanguage.entries.mapNotNull { language ->
            val file = localizedStringsFile(language)
            file.takeUnless(File::exists)?.path
        }

        assertTrue(
            missingFolders.isEmpty(),
            "Every supported language should have a strings.xml file. Missing: ${missingFolders.joinToString()}",
        )
    }

    @Test
    fun localizedStringKeysAndPlaceholdersMatchBase() {
        val baseStrings = parseStrings(baseStringsFile)
        val baseKeys = baseStrings.keys.toSet()

        AppLanguage.entries.forEach { language ->
            val localizedFile = localizedStringsFile(language)
            val localizedStrings = parseStrings(localizedFile)

            assertEquals(
                baseKeys,
                localizedStrings.keys.toSet(),
                "${language.localeTag} should define the same string keys as the base resource file.",
            )

            baseStrings.keys.forEach { key ->
                val basePlaceholders = placeholders(baseStrings.getValue(key))
                val localizedPlaceholders = placeholders(localizedStrings.getValue(key))
                assertEquals(
                    basePlaceholders,
                    localizedPlaceholders,
                    "${language.localeTag} should preserve format placeholders for '$key'.",
                )
            }
        }
    }

    private fun localizedStringsFile(language: AppLanguage): File {
        val folderName = when (language) {
            AppLanguage.English -> "values"
            AppLanguage.ChineseSimplified -> "values-zh"
            else -> "values-${language.localeTag.substringBefore('-')}"
        }
        return resourcesRoot.resolve("$folderName/strings.xml")
    }

    private fun parseStrings(file: File): LinkedHashMap<String, String> {
        val content = file.readText()
        return LinkedHashMap<String, String>().apply {
            stringRegex.findAll(content).forEach { match ->
                put(match.groupValues[1], match.groupValues[2].trim())
            }
        }
    }

    private fun placeholders(value: String): List<String> =
        placeholderRegex.findAll(value.replace("%%", "")).map { it.value }.toList()
}

