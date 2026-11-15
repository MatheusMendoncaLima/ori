package com.oriteam.ori.ui.launcher

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot

/** The part of each side of an item outside its middle, where a drop makes a folder. */
private const val MIDDLE_MARGIN = 0.25f

/**
 * Where an item or the grid of a page is, in root coordinates: [visible] is the part on the screen (where a finger
 * can be), [whole] all of it, also what is scrolled out of the list. A grid counts its rows from the top of [whole];
 * a carried item has the size of [whole], not of the part that was showing.
 */
internal data class ScreenPlace(val visible: Rect, val whole: Rect)

private fun screenPlaceOf(coordinates: LayoutCoordinates): ScreenPlace {
    val size = coordinates.size
    val whole = Rect(
        coordinates.localToRoot(Offset.Zero),
        coordinates.localToRoot(Offset(size.width.toFloat(), size.height.toFloat()))
    )
    return ScreenPlace(visible = coordinates.boundsInRoot(), whole = whole)
}

/**
 * Where the things of the home screen are, as each says on every layout: the items (of the pages and the dock), the
 * grid of each page, the dock and "Remover". The finger finds in them what it holds and where it drops.
 *
 * The items, the dock and "Remover" are plain values: nothing is drawn from them. The pages are snapshot state, read
 * only while carrying, so the drop preview follows a page that turns or a list that scrolls under a still finger; a
 * page is written only when it moved, so a still screen writes nothing.
 */
internal class HomeScreenPlaces {
    private val items = mutableMapOf<String, ScreenPlace>()
    private val pages = mutableStateMapOf<Int, ScreenPlace>()
    var dock: Rect? = null
    var remove: Rect? = null

    fun placePage(page: Int, coordinates: LayoutCoordinates) {
        val place = screenPlaceOf(coordinates)
        if (pages[page] != place) pages[page] = place
    }

    fun removePage(page: Int) {
        pages.remove(page)
    }

    fun placeItem(key: String, coordinates: LayoutCoordinates) {
        items[key] = screenPlaceOf(coordinates)
    }

    fun removeItem(key: String) {
        items.remove(key)
    }

    /** The item under [point]: only the part on the screen can be held. */
    fun itemAt(point: Offset): String? = items.entries.firstOrNull { point in it.value.visible }?.key

    /**
     * The item, of the ones [isTarget] takes, whose middle (the middle half, across and down) has [point]: an app held
     * there would go in a folder with it. Near its edges the item only moves, as on One UI.
     */
    fun itemWithMiddleAt(point: Offset, isTarget: (String) -> Boolean): String? = items.entries.firstOrNull { entry ->
        val (key, place) = entry
        val box = place.visible
        val middle = Rect(
            left = box.left + box.width * MIDDLE_MARGIN,
            top = box.top + box.height * MIDDLE_MARGIN,
            right = box.right - box.width * MIDDLE_MARGIN,
            bottom = box.bottom - box.height * MIDDLE_MARGIN
        )
        point in middle && isTarget(key)
    }?.key

    /** The part of the item [key] on the screen (where its menu opens, where the dock counts it). */
    fun visibleBoundsOf(key: String): Rect? = items[key]?.visible

    /** All of the item [key], also a part scrolled out of the list: the size it is carried with. */
    fun wholeBoundsOf(key: String): Rect? = items[key]?.whole

    /** The page whose grid shows [point], and its whole grid. */
    fun pageAt(point: Offset): Pair<Int, Rect>? =
        pages.entries.firstOrNull { point in it.value.visible }?.let { it.key to it.value.whole }
}
