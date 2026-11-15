package com.oriteam.ori.domain.launcher

/*
 * Resizing a widget of the home screen, as on One UI: its top start corner stays, and it grows or shrinks by whole
 * cells, within what its app allows and what the page has free.
 */

/**
 * How the app of a widget lets it be resized, in cells: from [min] to [max], across ([isHorizontal]) and down
 * ([isVertical]). An app that allows neither keeps its size.
 */
data class WidgetResize(
    val min: HomeSpan,
    val max: HomeSpan,
    val isHorizontal: Boolean,
    val isVertical: Boolean
) {
    /** The columns [current] may change to on [grid]; the current ones always, even outside what the app asks. */
    fun columnsFor(current: HomeSpan, grid: HomeGrid): IntRange =
        rangeOf(isHorizontal, current.columns, min.columns, minOf(max.columns, grid.columns))

    /** The rows [current] may change to on [grid]. */
    fun rowsFor(current: HomeSpan, grid: HomeGrid): IntRange =
        rangeOf(isVertical, current.rows, min.rows, minOf(max.rows, grid.rows))

    private fun rangeOf(isAllowed: Boolean, current: Int, least: Int, most: Int): IntRange =
        if (isAllowed) minOf(least, current).coerceAtLeast(1)..maxOf(most, current) else current..current
}

/**
 * The widget [widgetId] with [span] cells, on the home screen of [keys] laid out on [grid]. Free: only when the cells
 * it then takes are free (or its own), and every item keeps its cell, so nothing moves on its own; packed: the items
 * after it make room. Nothing changes on a locked home screen.
 */
fun LauncherSettings.withWidgetResized(
    keys: List<String>,
    widgetId: Int,
    span: HomeSpan,
    grid: HomeGrid
): LauncherSettings {
    if (!canResizeWidget(keys, widgetId, span, grid)) return this
    val resized = copy(widgetSpans = widgetSpans + (widgetId to span.fittedTo(grid)))
    if (arrangement == HomeArrangement.PACKED) return resized
    // Every item keeps the cell it is shown in (or the one stored for it), so nothing moves on its own.
    val shown = arranged(keys, grid).associate { it.key to it.cell }
    val cells = shown + positionsFor(grid.orientation).filterKeys { it in shown }
    return resized.withPositions(grid.orientation, cells)
}

/** Whether the widget [widgetId] can take [span] cells now: a size is offered only then. */
fun LauncherSettings.canResizeWidget(keys: List<String>, widgetId: Int, span: HomeSpan, grid: HomeGrid): Boolean {
    val key = widgetKey(widgetId)
    if (isLayoutLocked || key !in keys) return false
    return arrangement == HomeArrangement.PACKED || fitsResized(arranged(keys, grid), key, span.fittedTo(grid), grid)
}
