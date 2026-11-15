package com.oriteam.ori.domain.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/** A translation touches three places; these tests fail when one of them is forgotten. */
class AppLanguagesTest {

    private val resources = File("src/main/res")

    /** "values-en", "values-es-rMX", "values-b+es+419": a folder of texts in another language. */
    private val languageFolder = Regex("""^values-(?:b\+)?([a-z]{2,3})(?:[-+].*)?$""")

    @Test
    fun `every values-xx folder is a translated language, and every translated language has its folder`() {
        assertTrue("run from the app module", resources.isDirectory)
        val folders = resources.listFiles().orEmpty()
            .filter { it.isDirectory && File(it, "strings.xml").exists() }
            .mapNotNull { languageFolder.matchEntire(it.name)?.groupValues?.get(1) }
            .toSet()

        assertEquals(TranslatedLanguages, folders)
    }

    @Test
    fun `the languages offered in the system settings are the default and the translations`() {
        val declared = Regex("""android:name="([^"]+)"""")
            .findAll(File(resources, "xml/locales_config.xml").readText())
            .map { Locale.forLanguageTag(it.groupValues[1]).language }
            .toSet()

        assertEquals(TranslatedLanguages + DefaultAppLocale.language, declared)
    }

    @Test
    fun `values-pt is the same Portuguese as values, so Portuguese wins over a later language of the device`() {
        assertEquals(textsOf("values"), textsOf("values-pt"))
    }

    @Test
    fun `every translation has every text, and nothing else`() {
        val names = textsOf("values").keys
        (TranslatedLanguages - "pt").forEach { language ->
            assertEquals("values-$language", names, textsOf("values-$language").keys)
        }
    }

    /** Each translatable text of a folder by name: a string's text, or the items of a plural. */
    private fun textsOf(folder: String): Map<String, String> {
        val files = File(resources, folder).listFiles { file ->
            file.name.startsWith("strings") && file.extension == "xml"
        }.orEmpty()
        return files.flatMap { file ->
            val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
            listOf("string", "plurals").flatMap { tag ->
                val nodes = root.getElementsByTagName(tag)
                (0 until nodes.length).map { nodes.item(it) as Element }
            }
        }.filter { it.getAttribute("translatable") != "false" }
            .associate { it.getAttribute("name") to it.textContent.trim() }
    }

    @Test
    fun `without a translation for the device, dates and voice stay in the language of the texts`() {
        assertEquals(DefaultAppLocale, appLocaleFor(listOf(Locale.US), translated = emptySet()))
        assertEquals(DefaultAppLocale, appLocaleFor(emptyList(), translated = setOf("en")))
    }

    @Test
    fun `the first device language with a translation wins, with the device's region`() {
        val translated = setOf("en", "es")
        val mexico = Locale.forLanguageTag("es-MX")

        assertEquals(mexico, appLocaleFor(listOf(Locale.GERMANY, mexico, Locale.US), translated))
        assertEquals(Locale.UK, appLocaleFor(listOf(Locale.UK, mexico), translated))
        assertEquals(DefaultAppLocale, appLocaleFor(listOf(Locale.GERMANY), translated))
    }
}
