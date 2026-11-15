package com.oriteam.ori.ui.app.screens.aac

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oriteam.ori.domain.aac.AacBoard
import com.oriteam.ori.domain.aac.AacSymbol
import com.oriteam.ori.domain.aac.SymbolImage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The vocabulary read from the app's own texts turns the suggested board made in Portuguese into each language. */
@RunWith(AndroidJUnit4::class)
class StarterVocabularyResourcesTest {
    private val vocabulary = starterVocabularyOf(ApplicationProvider.getApplicationContext())

    /** The suggested board as the app created it in Portuguese: each word with its picture and no phrase. */
    private val portuguese = listOf(
        "eu" to "🙋", "você" to "🧑", "estou" to "🌙", "não" to "🚫",
        "quero" to "👉", "preciso" to "💡", "gosto" to "❤️", "com" to null,
        "água" to "🥛", "comida" to "🍎", "banheiro" to "🚻", "ajuda" to "🆘",
        "dor" to "🤕", "sono" to "😴", "fome" to "🍽️", "frio" to "🥶",
        "sim" to "👍", "mais" to "➕", "ele/ela" to "🧍", "nós" to "👥",
        "ir" to "🚶", "comer" to "🥄", "beber" to "🥤", "dormir" to "🛏️",
        "brincar" to "🧸", "mamãe" to "👩", "papai" to "👨", "remédio" to "💊",
        "calor" to "🥵", "medo" to "😨", "feliz" to "😊", "triste" to "😢"
    ).mapIndexed { index, (label, emoji) ->
        AacSymbol("s$index", "b", label, null, emoji?.let(SymbolImage::Emoji), index, "u")
    }
    private val board = AacBoard("b", "Básico", 0, "u", language = "pt-BR")

    @Test
    fun `the suggested board made in Portuguese is shown whole in English`() {
        val shown = vocabulary.symbolsIn(board, portuguese, "en-US")

        assertEquals(
            listOf(
                "I", "you", "am", "not", "want", "need", "like", "with",
                "water", "food", "bathroom", "help", "pain", "sleepy", "hungry", "cold",
                "yes", "more", "he/she", "we", "go", "eat", "drink", "sleep",
                "play", "mom", "dad", "medicine", "hot", "scared", "happy", "sad"
            ),
            shown.symbols.map { it.label }
        )
        assertEquals("en-US", shown.language)
        assertEquals("Basics", vocabulary.boardIn(board, "en-US").name)
    }

    @Test
    fun `the suggested board made in Portuguese is shown whole in Spanish`() {
        val shown = vocabulary.symbolsIn(board, portuguese, "es-MX")

        assertEquals(
            listOf(
                "yo", "tú", "tengo", "no", "quiero", "necesito", "me gusta", "con",
                "agua", "comida", "baño", "ayuda", "dolor", "sueño", "hambre", "frío",
                "sí", "más", "él/ella", "nosotros", "ir", "comer", "beber", "dormir",
                "jugar", "mamá", "papá", "medicina", "calor", "miedo", "feliz", "triste"
            ),
            shown.symbols.map { it.label }
        )
        assertEquals("es-MX", shown.language)
        assertEquals("Básico", vocabulary.boardIn(board, "es-MX").name)
    }
}
