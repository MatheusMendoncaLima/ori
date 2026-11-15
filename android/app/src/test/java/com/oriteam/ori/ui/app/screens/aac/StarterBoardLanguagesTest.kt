package com.oriteam.ori.ui.app.screens.aac

import com.oriteam.ori.domain.aac.EnglishGrammar
import com.oriteam.ori.domain.aac.PortugueseGrammar
import com.oriteam.ori.domain.aac.SentenceGrammar
import com.oriteam.ori.domain.aac.SpanishGrammar
import com.oriteam.ori.domain.aac.WordClass
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The suggested board of each language is made of words its grammar knows, so it is painted and builds sentences
 * that read right. A translation that changes a word ("tengo" for "estoy") without the grammar fails here.
 */
class StarterBoardLanguagesTest {

    private val languages: Map<String, SentenceGrammar> = mapOf(
        "values" to PortugueseGrammar,
        "values-en" to EnglishGrammar,
        "values-es" to SpanishGrammar
    )

    @Test
    fun `people, actions and feelings of the suggested board are painted in every language`() {
        val expected = mapOf(
            WordClass.PERSON to listOf("i", "you", "he_she", "we", "mom", "dad"),
            WordClass.ACTION to listOf("am", "want", "need", "like", "go", "eat", "drink", "sleep", "play"),
            WordClass.FEELING to listOf("pain", "sleepy", "hungry", "cold", "hot", "scared", "happy", "sad"),
            WordClass.SOCIAL to listOf("no", "yes"),
            WordClass.OTHER to listOf("with", "water", "food", "bathroom", "help", "more", "medicine")
        )
        languages.forEach { (folder, grammar) ->
            val words = starterWords(folder)
            expected.forEach { (wordClass, names) ->
                names.forEach { name ->
                    val word = words.getValue(name)
                    assertEquals("$folder: $word", wordClass, grammar.wordClassOf(word))
                }
            }
        }
    }

    @Test
    fun `the suggested board builds sentences that read right in every language`() {
        val sentences = listOf(
            listOf("i", "am", "pain"),
            listOf("you", "want", "water"),
            listOf("i", "like", "you"),
            listOf("i", "no", "need", "food")
        )
        val expected = mapOf(
            "values" to listOf("Eu estou com dor", "Você quer água", "Eu gosto de você", "Eu não preciso de comida"),
            "values-en" to listOf("I am in pain", "You want water", "I like you", "I don't need food"),
            "values-es" to listOf("Yo tengo dolor", "Tú quieres agua", "A mí me gustas tú", "Yo no necesito comida")
        )
        languages.forEach { (folder, grammar) ->
            val words = starterWords(folder)
            val said = sentences.map { names -> grammar.spoken(grammar.compose(names.map(words::getValue))) }
            assertEquals(folder, expected.getValue(folder), said)
        }
    }

    @Test
    fun `a person and how one feels, from the suggested board and without the verb, say the verb`() {
        val sentences = listOf("i", "you").flatMap { person ->
            listOf("pain", "sleepy", "hungry", "cold").map { feeling -> listOf(person, feeling) }
        } + listOf(listOf("i", "no", "hungry"), listOf("i", "need", "bathroom"))
        val expected = mapOf(
            "values" to listOf(
                "Eu estou com dor", "Eu estou com sono", "Eu estou com fome", "Eu estou com frio",
                "Você está com dor", "Você está com sono", "Você está com fome", "Você está com frio",
                "Eu não estou com fome", "Eu preciso de banheiro"
            ),
            "values-en" to listOf(
                "I am in pain", "I am sleepy", "I am hungry", "I am cold",
                "You are in pain", "You are sleepy", "You are hungry", "You are cold",
                "I am not hungry", "I need the bathroom"
            ),
            "values-es" to listOf(
                "Yo tengo dolor", "Yo tengo sueño", "Yo tengo hambre", "Yo tengo frío",
                "Tú tienes dolor", "Tú tienes sueño", "Tú tienes hambre", "Tú tienes frío",
                "Yo no tengo hambre", "Yo necesito el baño"
            )
        )
        languages.forEach { (folder, grammar) ->
            val words = starterWords(folder)
            val said = sentences.map { names -> grammar.spoken(grammar.compose(names.map(words::getValue))) }
            assertEquals(folder, expected.getValue(folder), said)
        }
    }

    @Test
    fun `the new words of the suggested board build sentences that read right in every language`() {
        val sentences = listOf(
            listOf("he_she", "want", "eat"),
            listOf("mom", "hot"),
            listOf("we", "like", "play"),
            listOf("i", "need", "medicine"),
            listOf("you", "no", "scared"),
            listOf("mom", "like", "water"),
            listOf("dad", "no", "sad")
        )
        val expected = mapOf(
            "values" to listOf(
                "Ele/ela quer comer", "Mamãe está com calor", "Nós gostamos de brincar", "Eu preciso de remédio",
                "Você não está com medo", "Mamãe gosta de água", "Papai não está triste"
            ),
            "values-en" to listOf(
                "He/she wants to eat", "Mom is hot", "We like to play", "I need medicine",
                "You are not scared", "Mom likes water", "Dad is not sad"
            ),
            "values-es" to listOf(
                "Él/ella quiere comer", "Mamá tiene calor", "A nosotros nos gusta jugar", "Yo necesito medicina",
                "Tú no tienes miedo", "A mamá le gusta el agua", "Papá no está triste"
            )
        )
        languages.forEach { (folder, grammar) ->
            val words = starterWords(folder)
            val said = sentences.map { names -> grammar.spoken(grammar.compose(names.map(words::getValue))) }
            assertEquals(folder, expected.getValue(folder), said)
        }
    }

    /** The words of the suggested board in one folder, by the end of their name ("aac_starter_pain" is "pain"). */
    private fun starterWords(folder: String): Map<String, String> {
        val file = File("src/main/res/$folder/strings_aac.xml")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .filter { it.getAttribute("name").startsWith(PREFIX) }
            .associate { it.getAttribute("name").removePrefix(PREFIX) to it.textContent }
    }

    private companion object {
        const val PREFIX = "aac_starter_"
    }
}
