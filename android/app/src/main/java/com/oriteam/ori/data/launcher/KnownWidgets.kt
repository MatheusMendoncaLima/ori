package com.oriteam.ori.data.launcher

/**
 * The widgets of the last list read, by id. The picture of each widget in the list, and adding one, look here instead
 * of asking Android for the whole list again: with many widgets (77 on the test tablet) that answer is too big for the
 * system's message buffer, it failed and closed ORI Home, and it was asked once per row of the list.
 *
 * A widget missing here (an app installed since the list was read) is asked to its own app only ([find]).
 */
internal class KnownWidgets<T>(private val idOf: (T) -> String) {
    @Volatile
    private var byId: Map<String, T> = emptyMap()

    /** The list just read from Android. */
    fun remember(widgets: List<T>) {
        byId = widgets.associateBy(idOf)
    }

    /**
     * The widget [id]: from the last list, or else from [readItsApp] (the widgets of its app, null when Android could
     * not answer). Null when it is in neither.
     */
    fun find(id: String, readItsApp: () -> List<T>?): T? = byId[id]
        ?: readItsApp()?.firstOrNull { idOf(it) == id }?.also { found -> byId = byId + (id to found) }
}
