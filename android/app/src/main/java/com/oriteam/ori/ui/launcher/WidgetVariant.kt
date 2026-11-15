package com.oriteam.ori.ui.launcher

import com.oriteam.ori.domain.launcher.WidgetProvider

/**
 * What tells apart widgets of one app that have the same name and size: on the tablet of the test, five "Current
 * weather and forecast", all 2 × 2, looked the same in the list. The description the app gives, when it differs;
 * otherwise a number ("Opção 2").
 */
internal sealed interface WidgetVariant {
    data class Described(val text: String) : WidgetVariant

    data class Numbered(val number: Int) : WidgetVariant
}

/**
 * The note of each widget of [providers] that needs one, by id: only widgets that share their app, name and size with
 * another one get it. A unique description is used; the others are numbered in the order of the list.
 */
internal fun variantsOf(providers: List<WidgetProvider>): Map<String, WidgetVariant> = providers
    .groupBy { Triple(it.packageName, it.label, it.span) }
    .values
    .filter { it.size > 1 }
    .flatMap { same ->
        val descriptions = same.mapNotNull { it.description?.takeIf(String::isNotBlank) }
        val isTold = descriptions.size == same.size && descriptions.toSet().size == same.size
        same.mapIndexed { index, provider ->
            val description = provider.description
            provider.id to if (isTold && description != null) {
                WidgetVariant.Described(description)
            } else {
                WidgetVariant.Numbered(index + 1)
            }
        }
    }
    .toMap()
