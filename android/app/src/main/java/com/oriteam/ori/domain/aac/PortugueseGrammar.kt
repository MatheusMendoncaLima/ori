package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.aac.Person.FIRST
import com.oriteam.ori.domain.aac.Person.FIRST_PLURAL
import com.oriteam.ori.domain.aac.Person.SECOND
import com.oriteam.ori.domain.aac.Person.THIRD
import com.oriteam.ori.domain.aac.Person.THIRD_PLURAL

/** "gosto de você", "gosto de comer". */
private val ofEverything = WordKind.entries.associateWith { "de" }

/** "preciso de ajuda", but "preciso comer". */
private val ofUnlessAction = ofEverything - WordKind.ACTION

private val infinitiveEnding = Regex("(ar|er|ir)$")

/** "Você" takes the verb of "ele", so the second person has the forms of the third. */
private fun portugueseVerb(
    first: String,
    third: String,
    firstPlural: String,
    thirdPlural: String,
    connectors: Map<WordKind, String> = emptyMap()
) = Verb(
    forms = mapOf(
        FIRST to first,
        SECOND to third,
        THIRD to third,
        FIRST_PLURAL to firstPlural,
        THIRD_PLURAL to thirdPlural
    ),
    connectors = connectors
)

/** "estou com dor", but "estou feliz". */
private val toBe = portugueseVerb("estou", "está", "estamos", "estão", mapOf(WordKind.STATE to "com"))

private val portuguese = object : GrammarTables(
    verbs = listOf(
        portugueseVerb("quero", "quer", "queremos", "querem"),
        portugueseVerb("preciso", "precisa", "precisamos", "precisam", ofUnlessAction),
        portugueseVerb("gosto", "gosta", "gostamos", "gostam", ofEverything),
        toBe,
        portugueseVerb("tenho", "tem", "temos", "têm")
    ),
    subjects = mapOf(
        "eu" to FIRST,
        "você" to THIRD,
        "ele" to THIRD,
        "ela" to THIRD,
        "ele/ela" to THIRD,
        "a gente" to THIRD,
        "nós" to FIRST_PLURAL,
        "vocês" to THIRD_PLURAL,
        "eles" to THIRD_PLURAL,
        "elas" to THIRD_PLURAL,
        // People named by what they are to the person: "mamãe quer água".
        "mamãe" to THIRD,
        "papai" to THIRD
    ),
    linkingWords = setOf("de", "do", "da", "com", "para", "a", "ao", "à", "em", "no", "na", "que", "não", "e"),
    negation = "não",
    states = setOf("dor", "sono", "fome", "frio", "calor", "sede", "medo", "febre", "pressa", "raiva"),
    qualities = setOf(
        "bem", "mal", "feliz", "triste", "doente", "cansado", "cansada", "nervoso", "nervosa", "calmo", "calma",
        "enjoado", "enjoada", "assustado", "assustada"
    ),
    // Painted as actions; any infinitive is still known by its ending, below.
    actions = setOf(
        "ir", "vir", "sair", "voltar", "comer", "beber", "tomar", "dormir", "descansar", "brincar", "falar", "andar",
        "ver", "ouvir", "ler", "esperar", "estudar", "trabalhar", "cantar", "dançar", "desenhar", "correr", "nadar",
        "ajudar", "tomar banho"
    ),
    // "eu fome" says "eu estou com fome"; "eu cansada", "eu estou cansada".
    missingVerbs = mapOf(WordKind.STATE to toBe, WordKind.QUALITY to toBe),
    social = setOf("sim", "não", "talvez", "por favor", "obrigado", "obrigada", "oi", "olá", "tchau", "desculpa")
) {
    // Only ever to leave "de" out ("preciso comer"), never to add a word: "lugar" taken for a verb costs at most
    // a missing "de". Only the infinitives of [actions] are painted, which needs certainty.
    override fun isAction(key: String) = key in actions || infinitiveEnding.containsMatchIn(key)
}

/** Ways of saying two neighboring words that Portuguese always joins or changes. */
private val contractions = mapOf(
    "de ele" to "dele",
    "de ela" to "dela",
    "de eles" to "deles",
    "de elas" to "delas",
    "de eu" to "de mim",
    "com eu" to "comigo",
    "com nós" to "conosco"
)

/** Portuguese: "eu gosto de você", "você quer água", "estou com dor", and "dele" when it is said. */
object PortugueseGrammar : SentenceGrammar {
    override fun compose(words: List<String>, classes: List<WordClass?>) =
        portuguese.read(words, classes).map { it.second }

    override fun spoken(pieces: List<SentencePiece>) = pieces.spoken(contractions)

    override fun wordClassOf(word: String) = portuguese.classOf(word)
}
