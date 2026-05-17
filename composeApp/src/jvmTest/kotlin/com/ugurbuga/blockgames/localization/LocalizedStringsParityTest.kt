package com.ugurbuga.blockgames.localization

import com.ugurbuga.blockgames.game.model.AppLanguage
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
    private val stringRegex = Regex("""<string\s+name="([^"]+)"([^>]*)>([\s\S]*?)</string>""")
    private val placeholderRegex = Regex("""%(?:\d+\$)?[a-zA-Z]""")

    private data class StringEntry(
        val value: String,
        val translatable: Boolean,
    )

    @Test
    fun everySupportedLanguageHasAStringsFile() {
        val baseFiles = resourceFiles("values")
        assertTrue(baseFiles.isNotEmpty(), "Base values folder should contain at least one XML resource file.")

        val missingFolders = AppLanguage.entries.mapNotNull { language ->
            val folderName = localizedFolderName(language)
            val files = resourceFiles(folderName)
            folderName.takeIf { files.isEmpty() }
        }

        assertTrue(
            missingFolders.isEmpty(),
            "Every supported language should have localized XML resource files. Missing: ${missingFolders.joinToString()}",
        )
    }

    @Test
    fun localizedStringKeysAndPlaceholdersMatchBase() {
        val baseStrings = parseStrings(resourceFiles("values"))
        val baseTranslatableKeys = baseStrings
            .filterValues(StringEntry::translatable)
            .keys
            .toSet()

        AppLanguage.entries.forEach { language ->
            val localizedStrings = parseStrings(resourceFiles(localizedFolderName(language)))
            val localizedTranslatableStrings = localizedStrings.filterValues(StringEntry::translatable)

            assertEquals(
                baseTranslatableKeys,
                localizedTranslatableStrings.keys.toSet(),
                "${language.localeTag} should define the same string keys as the base resource file.",
            )

            baseTranslatableKeys.forEach { key ->
                val basePlaceholders = placeholders(baseStrings.getValue(key).value)
                val localizedPlaceholders = placeholders(localizedTranslatableStrings.getValue(key).value)
                assertEquals(
                    basePlaceholders,
                    localizedPlaceholders,
                    "${language.localeTag} should preserve format placeholders for '$key'.",
                )
            }
        }
    }

    private fun localizedFolderName(language: AppLanguage): String {
        return when (language) {
            AppLanguage.English -> "values"
            AppLanguage.ChineseSimplified -> "values-zh"
            else -> "values-${language.localeTag.substringBefore('-')}"
        }
    }

    private fun resourceFiles(folderName: String): List<File> {
        val folder = resourcesRoot.resolve(folderName)
        return folder
            .takeIf(File::exists)
            ?.listFiles { file -> file.isFile && file.extension == "xml" }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    private fun parseStrings(files: List<File>): LinkedHashMap<String, StringEntry> {
        return LinkedHashMap<String, StringEntry>().apply {
            files.forEach { file ->
                val content = file.readText()
                stringRegex.findAll(content).forEach { match ->
                    val attributes = match.groupValues[2]
                    put(
                        match.groupValues[1],
                        StringEntry(
                            value = match.groupValues[3].trim(),
                            translatable = !attributes.contains("translatable=\"false\""),
                        ),
                    )
                }
            }
        }
    }

    private fun placeholders(value: String): List<String> =
        placeholderRegex.findAll(value.replace("%%", "")).map { it.value }.toList()
}

