package com.oriteam.ori.domain.launcher

/**
 * A kind of widget an installed app offers (a clock, the weather, a note). [id] names the provider and the profile,
 * as the app keys do; [span] is the size it asks for, in home screen cells.
 */
data class WidgetProvider(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val label: String,
    val span: HomeSpan,
    /** The app asks the person to set the widget up (choose a city, a note) before it shows. */
    val needsSetup: Boolean,
    /** A widget of the clock app of the device (the time and the date as the phone shows them), listed first. */
    val isClock: Boolean = false,
    /** What the app says the widget shows (Android 12+), to tell apart widgets with the same name; null without it. */
    val description: String? = null
)

/**
 * The widgets the installed apps offer. [isComplete] is false when Android could not give some of them (an answer too
 * big, or an app that failed): those are left out, and the list says so instead of failing.
 */
data class WidgetProviders(val providers: List<WidgetProvider>, val isComplete: Boolean = true)

/**
 * The widgets Android lets a home screen show. A widget is an id Android gives the home screen; the app behind it
 * draws it. Binding the id to a provider needs the person's yes, which Android asks in its own window when
 * [bindIfAllowed] is false. ORI keeps only the id and the size; what the widget shows never passes through ORI.
 */
interface HomeWidgetHost {
    /** The widgets the installed apps offer, in alphabetical order of the app; never fails (see [WidgetProviders]). */
    suspend fun providers(): WidgetProviders

    /** A new id, still empty. */
    fun allocate(): Int

    /** Binds [widgetId] to the provider when the person already allowed this home screen; false asks Android. */
    fun bindIfAllowed(widgetId: Int, providerId: String): Boolean

    /** Gives the id back to Android, when the widget leaves the home screen or was never finished. */
    fun release(widgetId: Int)

    /** False when the app of the widget was removed: the widget can only be taken out. */
    fun isAvailable(widgetId: Int): Boolean

    /** How the app of the widget lets it be resized, read off the main thread; null when the widget is gone. */
    suspend fun resizeOf(widgetId: Int): WidgetResize?

    /** Whether the app of the widget has a setup screen to open again ("Configurar o widget", as One UI's menu). */
    suspend fun canSetUp(widgetId: Int): Boolean

    /** Every id Android keeps for this home screen, on it or not (one left behind by a step that never ended). */
    suspend fun boundIds(): List<Int>
}
