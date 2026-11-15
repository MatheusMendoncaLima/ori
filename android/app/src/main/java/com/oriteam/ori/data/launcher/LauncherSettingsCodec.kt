package com.oriteam.ori.data.launcher

import com.oriteam.ori.domain.launcher.GridChoice
import com.oriteam.ori.domain.launcher.HomeCell
import com.oriteam.ori.domain.launcher.HomeSpan
import com.oriteam.ori.domain.launcher.LauncherWallpaper
import com.oriteam.ori.domain.launcher.WallpaperColor
import com.oriteam.ori.domain.launcher.WallpaperGradient

/*
 * How the ORI Home choices are written in the DataStore: plain text, one entry per line, so a value of a newer
 * version (or a broken one) is simply skipped and never stops the home screen from opening.
 */

private const val THEME = "theme"
private const val SOLID_PREFIX = "color:"
private const val GRADIENT_PREFIX = "gradient:"
private const val PHOTO_PREFIX = "photo:"
internal const val FIELD_SEPARATOR = "\t"
internal const val LINE_SEPARATOR = "\n"
private const val CELL_FIELDS = 4
private const val SPAN_FIELDS = 3

/** Nothing stored is the Android wallpaper, the usual for a home screen. */
internal fun LauncherWallpaper.toStored(): String? = when (this) {
    LauncherWallpaper.System -> null
    LauncherWallpaper.Theme -> THEME
    is LauncherWallpaper.Solid -> SOLID_PREFIX + color.name
    is LauncherWallpaper.Gradient -> GRADIENT_PREFIX + gradient.name
    is LauncherWallpaper.Photo -> PHOTO_PREFIX + fileName
}

/** A value this version does not know is the Android wallpaper. */
internal fun wallpaperOf(stored: String?): LauncherWallpaper = when {
    stored == null -> LauncherWallpaper.System
    stored == THEME -> LauncherWallpaper.Theme
    stored.startsWith(
        SOLID_PREFIX
    ) -> WallpaperColor.entries.firstOrNull { it.name == stored.removePrefix(SOLID_PREFIX) }
        ?.let(LauncherWallpaper::Solid) ?: LauncherWallpaper.System
    stored.startsWith(GRADIENT_PREFIX) ->
        WallpaperGradient.entries.firstOrNull { it.name == stored.removePrefix(GRADIENT_PREFIX) }
            ?.let(LauncherWallpaper::Gradient) ?: LauncherWallpaper.System
    stored.startsWith(PHOTO_PREFIX) -> LauncherWallpaper.Photo(stored.removePrefix(PHOTO_PREFIX))
    else -> LauncherWallpaper.System
}

/** "key<TAB>page<TAB>row<TAB>column", one per line. */
internal fun Map<String, HomeCell>.toStoredCells(): String = entries.joinToString(LINE_SEPARATOR) { (key, cell) ->
    listOf(key, cell.page, cell.row, cell.column).joinToString(FIELD_SEPARATOR)
}

internal fun cellsOf(stored: String?): Map<String, HomeCell> = linesOf(stored, CELL_FIELDS).mapNotNull { fields ->
    val (page, row, column) = fields.drop(1).map {
        it.toIntOrNull()?.takeIf { value -> value >= 0 } ?: return@mapNotNull null
    }
    fields[0] to HomeCell(page, row, column)
}.toMap()

/** "widgetId<TAB>columns<TAB>rows", one per line. */
internal fun Map<Int, HomeSpan>.toStoredSpans(): String = entries.joinToString(LINE_SEPARATOR) { (id, span) ->
    listOf(id, span.columns, span.rows).joinToString(FIELD_SEPARATOR)
}

internal fun spansOf(stored: String?): Map<Int, HomeSpan> = linesOf(stored, SPAN_FIELDS).mapNotNull { fields ->
    val (id, columns, rows) = fields.map { it.toIntOrNull() ?: return@mapNotNull null }
    if (columns < 1 || rows < 1) null else id to HomeSpan(columns, rows)
}.toMap()

internal fun linesOf(stored: String?, fields: Int): List<List<String>> = stored.orEmpty()
    .split(LINE_SEPARATOR)
    .map { it.split(FIELD_SEPARATOR) }
    .filter { it.size == fields }

/** An enum value by its name, or [default] for a name this version does not know. */
internal inline fun <reified T : Enum<T>> enumOf(stored: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == stored } ?: default

/** A grid is stored as "columns x rows" ("6x8"); one this small or this big is not a grid ORI offers. */
private val STORED_GRID_RANGE = 1..12

internal fun GridChoice.toStored(): String = "${columns}x$rows"

/** The grid stored, or null (automatic) for nothing, a broken value or a size ORI does not offer. */
internal fun gridOf(stored: String?): GridChoice? {
    val parts = stored?.split('x')?.map { it.toIntOrNull() }
    val columns = parts?.getOrNull(0)?.takeIf { it in STORED_GRID_RANGE }
    val rows = parts?.getOrNull(1)?.takeIf { it in STORED_GRID_RANGE }
    return if (parts?.size == 2 && columns != null && rows != null) GridChoice(columns, rows) else null
}
