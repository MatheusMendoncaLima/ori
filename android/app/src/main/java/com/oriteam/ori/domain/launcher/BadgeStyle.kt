package com.oriteam.ori.domain.launcher

/*
 * The badges on the icons, as One UI's "Selos dos ícones de apps": a dot, or the number of notifications an app shows
 * now (the number from the start, as on One UI). Android lets a home screen count them only while the person allows
 * it ("Acesso a notificações"): until then nothing shows and nothing is counted. ORI Home then counts how many each
 * app has, in memory, and never reads, keeps or sends what they say.
 */

/** How the badges show: not at all, a dot, or the number. */
enum class BadgeStyle { OFF, DOT, NUMBER }

/** The badges as they show now: as chosen in "Selos dos ícones" (the levels of stimulus leave them to that choice). */
val LauncherSettings.badgesShown: BadgeStyle get() = badges

/** A number badge shows at most this; more is "99+". */
const val MAX_BADGE_NUMBER = 99

/** The notifications of an app are counted by its package and profile, as Android gives them: "package@profile". */
fun badgeKeyOf(packageName: String, profile: Long): String = "$packageName@$profile"

/**
 * How many notifications [app] shows now, from [counts] (by [badgeKeyOf]). A shortcut has none of its own: they are
 * its app's.
 */
fun badgeCountOf(app: LauncherApp, counts: Map<String, Int>): Int {
    val profile = app.key.substringAfterLast('@', "").toLongOrNull()
    return if (isShortcutKey(app.key) || profile == null) 0 else counts[badgeKeyOf(app.packageName, profile)] ?: 0
}

/**
 * Whether a notification counts on its app's badge, as Android's own home screen counts: not one that stays while
 * something runs (music, a download), not the summary of a group (its notifications count one by one), and not one
 * whose app or channel turned badges off.
 */
fun countsOnBadge(isOngoing: Boolean, isGroupSummary: Boolean, canShowBadge: Boolean): Boolean =
    !isOngoing && !isGroupSummary && canShowBadge
