package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.aac.Person.FIRST
import com.oriteam.ori.domain.aac.Person.FIRST_PLURAL
import com.oriteam.ori.domain.aac.Person.SECOND
import com.oriteam.ori.domain.aac.Person.THIRD
import com.oriteam.ori.domain.aac.Person.THIRD_PLURAL

private fun spanishVerb(
    first: String,
    second: String,
    third: String,
    firstPlural: String,
    thirdPlural: String,
    connectors: Map<WordKind, String> = emptyMap()
) = Verb(
    forms = mapOf(
        FIRST to first,
        SECOND to second,
        THIRD to third,
        FIRST_PLURAL to firstPlural,
        THIRD_PLURAL to thirdPlural
    ),
    connectors = connectors
)

/**
 * "Me gusta" agrees with the person who likes, as "me", "te", "le"… In Spanish that person comes with "a"
 * ("a mí me gusta") and the thing liked with its article ("me gusta el agua").
 */
private val likes = spanishVerb("me gusta", "te gusta", "le gusta", "nos gusta", "les gusta")

/** "tengo hambre", "tengo que ir". */
private val toHave = spanishVerb("tengo", "tienes", "tiene", "tenemos", "tienen", mapOf(WordKind.ACTION to "que"))

/** "estoy con dolor", but "estoy cansada". */
private val toBe = spanishVerb("estoy", "estás", "está", "estamos", "están", mapOf(WordKind.STATE to "con"))

private val spanish = GrammarTables(
    verbs = listOf(
        spanishVerb("quiero", "quieres", "quiere", "queremos", "quieren"),
        spanishVerb("necesito", "necesitas", "necesita", "necesitamos", "necesitan"),
        toHave,
        toBe,
        likes,
        // "puedo ir": the action follows with nothing between.
        spanishVerb("puedo", "puedes", "puede", "podemos", "pueden"),
        // "voy a comer", "voy al baño".
        spanishVerb("voy", "vas", "va", "vamos", "van", mapOf(WordKind.ACTION to "a", WordKind.THING to "a")),
        spanishVerb("prefiero", "prefieres", "prefiere", "preferimos", "prefieren")
    ),
    subjects = mapOf(
        "yo" to FIRST,
        "tú" to SECOND,
        "usted" to THIRD,
        "él" to THIRD,
        "ella" to THIRD,
        "nosotros" to FIRST_PLURAL,
        "nosotras" to FIRST_PLURAL,
        "ustedes" to THIRD_PLURAL,
        "ellos" to THIRD_PLURAL,
        "ellas" to THIRD_PLURAL,
        "él/ella" to THIRD,
        // People named by what they are to the person: "mamá quiere agua", "a mamá le gusta".
        "mamá" to THIRD,
        "papá" to THIRD,
        "abuela" to THIRD,
        "abuelo" to THIRD,
        "hermano" to THIRD,
        "hermana" to THIRD,
        "amigo" to THIRD,
        "amiga" to THIRD,
        "bebé" to THIRD
    ),
    linkingWords = setOf(
        "de", "del", "con", "para", "a", "al", "en", "que", "no", "y", "o", "por", "sin",
        "el", "la", "los", "las", "un", "una"
    ),
    negation = "no",
    states = setOf("dolor", "sueño", "hambre", "frío", "calor", "sed", "miedo", "fiebre", "prisa"),
    qualities = setOf(
        "feliz", "triste", "cansado", "cansada", "enfermo", "enferma", "nervioso", "nerviosa", "aburrido",
        "aburrida", "mareado", "mareada", "tranquilo", "tranquila", "contento", "contenta", "enojado", "enojada",
        "asustado", "asustada", "preocupado", "preocupada", "emocionado", "emocionada", "confundido", "confundida",
        "solo", "sola", "mejor", "peor", "bien", "mal", "listo", "lista", "mojado", "mojada", "sucio", "sucia"
    ),
    actions = setOf(
        "ir", "volver", "salir", "llegar", "comer", "beber", "tomar", "dormir", "descansar", "jugar", "hablar",
        "caminar", "ver", "escuchar", "leer", "esperar", "estudiar", "trabajar", "cantar", "bailar", "dibujar",
        "pintar", "correr", "nadar", "cocinar", "ayudar", "bañarme", "ducharme", "sentarme", "levantarme",
        "abrir", "cerrar", "lavar", "limpiar", "escribir", "llamar", "usar", "terminar", "cambiar", "probar",
        "acostarme", "vestirme", "lavarme", "saltar", "abrazar", "esconderme", "pasear", "orinar"
    ),
    // "yo hambre" says "yo tengo hambre", as Spanish says it; "yo cansada", "yo estoy cansada".
    missingVerbs = mapOf(WordKind.STATE to toHave, WordKind.QUALITY to toBe),
    // "necesito el baño"; "quiero agua" and "necesito ayuda" take none. After "me gusta", see [likedArticles].
    articles = mapOf(
        "baño" to "el", "médico" to "el", "doctor" to "el", "teléfono" to "el", "coche" to "el", "carro" to "el",
        "libro" to "el", "parque" to "el", "juego" to "el", "pelota" to "la", "cama" to "la", "puerta" to "la",
        "ventana" to "la", "luz" to "la", "televisión" to "la", "tele" to "la", "escuela" to "la", "cocina" to "la",
        "silla" to "la", "mesa" to "la", "computadora" to "la", "tableta" to "la", "cuarto" to "el",
        "habitación" to "la", "manta" to "la", "almohada" to "la", "autobús" to "el"
    ),
    social = setOf(
        "sí", "no", "tal vez", "por favor", "gracias", "hola", "adiós", "perdón", "vale", "de nada", "lo siento",
        "buenos días", "buenas noches"
    )
)

/** "Me gusta" also agrees with whoever is liked: "me gustas tú", "me gustan ellos". */
private val likedForms = mapOf(
    FIRST to "gusto",
    SECOND to "gustas",
    THIRD to "gusta",
    FIRST_PLURAL to "gustamos",
    THIRD_PLURAL to "gustan"
)

/** After "a", "mí" and "ti" take the place of "yo" and "tú"; the other persons do not change. */
private val afterA = mapOf("yo" to "mí", "tú" to "ti")

/** After "me gusta" every noun takes its article, also those one does not count ("el agua"); only certain genders. */
private val likedArticles = mapOf(
    "agua" to "el", "comida" to "la", "baño" to "el", "ayuda" to "la", "música" to "la", "leche" to "la",
    "fruta" to "la", "pan" to "el", "chocolate" to "el", "helado" to "el", "jugo" to "el", "café" to "el",
    "casa" to "la", "escuela" to "la", "parque" to "el", "playa" to "la", "televisión" to "la", "juego" to "el",
    "pelota" to "la", "libro" to "el", "perro" to "el", "gato" to "el", "calor" to "el", "frío" to "el"
)

/** Ways of saying two neighboring words that Spanish always joins or changes. */
private val contractions = mapOf(
    "a el" to "al",
    "de el" to "del",
    "con yo" to "conmigo",
    "con tú" to "contigo",
    "a yo" to "a mí",
    "a tú" to "a ti",
    "de yo" to "de mí",
    "de tú" to "de ti",
    "para yo" to "para mí",
    "para tú" to "para ti",
    "por yo" to "por mí",
    "por tú" to "por ti",
    "sin yo" to "sin mí",
    "sin tú" to "sin ti"
)

/**
 * Spanish: the agreement ("tú tienes"), "que" before an action after "tengo", "con" before a state after "estoy",
 * and "me gusta" as Spanish says it: "yo" + "me gusta" + "agua" says "a mí me gusta el agua", and "yo" +
 * "me gusta" + "tú" says "a mí me gustas tú", on the screen too.
 */
object SpanishGrammar : SentenceGrammar {
    override fun compose(words: List<String>, classes: List<WordClass?>): List<SentencePiece> {
        val read = spanish.read(words, classes)
        return read.mapIndexed { index, (reading, piece) ->
            when {
                read.likerAt(index) -> piece.copy(text = afterA[reading.key] ?: piece.text, connector = "a")
                reading.verb == likes ->
                    piece.copy(text = piece.text.agreeingWith(read.getOrNull(index + 1)?.first))
                read.getOrNull(index - 1)?.first?.verb == likes ->
                    piece.copy(connector = likedArticles[reading.key] ?: piece.connector)
                else -> piece
            }
        }
    }

    override fun spoken(pieces: List<SentencePiece>) = pieces.spoken(contractions)

    override fun wordClassOf(word: String) = spanish.classOf(word)

    /** "me gusta" + "tú" says "me gustas tú"; a thing liked ("agua") leaves it as it is. */
    private fun String.agreeingWith(liked: Reading?): String {
        val person = liked?.key?.let(spanish.subjects::get)
        return if (person != null && endsWith("gusta")) removeSuffix("gusta") + likedForms.getValue(person) else this
    }

    /** The subject right before "me gusta", with or without "no" between them. */
    private fun List<Pair<Reading, SentencePiece>>.likerAt(index: Int): Boolean {
        val reading = this[index].first
        if (!reading.isPerson || reading.isAfterVerb) return false
        val next = getOrNull(index + 1)?.first
        val verb = if (next?.key == spanish.negation) getOrNull(index + 2)?.first?.verb else next?.verb
        return verb == likes
    }
}
