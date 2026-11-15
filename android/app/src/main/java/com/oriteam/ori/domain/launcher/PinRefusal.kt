package com.oriteam.ori.domain.launcher

/*
 * An app asks to put one of its shortcuts or widgets on the home screen (Android's "Adicionar à tela inicial"). ORI
 * Home asks the person first and adds it at the first free place, as One UI does; a locked or full home screen takes
 * none, and the person is told why.
 */

/** Why an item an app asks for cannot go on the home screen now. */
enum class PinRefusal { LOCKED, FULL }

/** Null when an item asked for can go on the home screen now. */
val LauncherSettings.pinRefusal: PinRefusal?
    get() = when {
        isLayoutLocked -> PinRefusal.LOCKED
        isHomeFull -> PinRefusal.FULL
        else -> null
    }

/** The widget [widgetId] at the end of the home screen, taking the cells it asks for ([span]). */
fun LauncherSettings.withWidget(widgetId: Int, span: HomeSpan): LauncherSettings = copy(
    favorites = favorites + widgetKey(widgetId),
    widgetSpans = widgetSpans + (widgetId to span)
)
