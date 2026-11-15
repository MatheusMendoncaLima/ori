package com.oriteam.ori.domain.launcher

/*
 * How much ORI Home stimulates, for whoever is stimulated too easily: levels that set every choice at once, as the
 * presets of a game's graphics, and the choices themselves, which the person can still change one by one (the level
 * then is "Personalizado").
 */

/** How much ORI Home moves: as the person chose for the app, less (only what helps notice a change), or not at all. */
enum class StimulusMotion { NORMAL, REDUCED, OFF }

/**
 * The choices a level sets: the motion, whether the wallpaper shows (the Android one too: without it, the plain
 * background of the theme), whether the icons keep their colors, and fewer decorations (the simplified interface).
 */
data class StimulusChoices(
    val motion: StimulusMotion = StimulusMotion.NORMAL,
    val showsWallpaper: Boolean = true,
    val colorfulIcons: Boolean = true,
    val isSimplified: Boolean = false
)

/** The levels of stimulus, from all of it to none. */
enum class StimulusLevel(val choices: StimulusChoices) {
    /** Nothing taken away. */
    STANDARD(StimulusChoices()),

    /** Fewer animations. */
    MEDIUM(StimulusChoices(motion = StimulusMotion.REDUCED)),

    /** No animations; the wallpaper and the colors stay. */
    LOW(StimulusChoices(motion = StimulusMotion.OFF)),

    /** No animations, no wallpaper (not even Android's), apps without colors and fewer decorations. */
    NONE(
        StimulusChoices(
            motion = StimulusMotion.OFF,
            showsWallpaper = false,
            colorfulIcons = false,
            isSimplified = true
        )
    )
}

/** The level whose choices these are; null when they were chosen one by one ("Personalizado"). */
val StimulusChoices.level: StimulusLevel? get() = StimulusLevel.entries.firstOrNull { it.choices == this }

/** The icons are drawn grey: the apps lose their colors. */
val LauncherSettings.isMonochrome: Boolean get() = !stimulus.colorfulIcons

/** The wallpaper the home screen shows: the chosen one, or the plain background of the theme when it is hidden. */
val LauncherSettings.wallpaperShown: LauncherWallpaper
    get() = if (stimulus.showsWallpaper) wallpaper else LauncherWallpaper.Theme
