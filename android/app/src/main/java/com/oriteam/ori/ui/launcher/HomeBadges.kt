package com.oriteam.ori.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.oriteam.ori.R
import com.oriteam.ori.domain.launcher.BadgeStyle
import com.oriteam.ori.domain.launcher.LauncherApp
import com.oriteam.ori.domain.launcher.MAX_BADGE_NUMBER
import com.oriteam.ori.domain.launcher.badgeCountOf
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OnOriBadge
import com.oriteam.ori.ui.theme.OriBadge
import com.oriteam.ori.ui.theme.OriSpacing

/** The dot is a fifth of the icon, and the number a third: seen at a glance, never covering the icon. */
private const val DOT_SHARE = 0.2f
private const val NUMBER_SHARE = 0.33f

/** The number takes this part of the height of its bubble, in bold, as on One UI. */
private const val NUMBER_TEXT_SHARE = 0.6f

/** The badge sits on the top end corner of the icon, a little out of it, as on One UI. */
private const val OUTSIDE_SHARE = 0.08f

/**
 * The badges of the icons now: how many notifications each app shows ([counts], by "package@profile") and how they
 * show ([style]). Given once by the home screen, read by each icon; off, every icon reads none.
 */
@Immutable
internal data class HomeBadges(val counts: Map<String, Int> = emptyMap(), val style: BadgeStyle = BadgeStyle.OFF) {
    fun countOf(app: LauncherApp): Int = if (style == BadgeStyle.OFF) 0 else badgeCountOf(app, counts)

    /** A folder shows the sum of its apps, as on One UI. */
    fun countOf(apps: List<LauncherApp>): Int = apps.sumOf(::countOf)
}

internal val LocalHomeBadges = compositionLocalOf { HomeBadges() }

/**
 * An icon of [iconSize] with its badge on the top end corner, as on One UI: a red dot, or a red bubble with the
 * number in white (at most "99+"), the same in the light and the dark theme. The bubble and its number grow with the
 * icon. Only a picture: the name of the tile says how many ([tileDescription]).
 */
@Composable
internal fun BadgedIcon(count: Int, iconSize: Dp, icon: @Composable () -> Unit) {
    val style = LocalHomeBadges.current.style
    Box {
        icon()
        if (count > 0 && style != BadgeStyle.OFF) {
            val corner = Modifier
                .align(Alignment.TopEnd)
                .offset(x = iconSize * OUTSIDE_SHARE, y = -iconSize * OUTSIDE_SHARE)
                .background(OriBadge, CircleShape)
            if (style == BadgeStyle.DOT) {
                Box(modifier = corner.size(iconSize * DOT_SHARE))
            } else {
                BadgeNumber(count, iconSize * NUMBER_SHARE, corner)
            }
        }
    }
}

@Composable
private fun BadgeNumber(count: Int, bubble: Dp, modifier: Modifier) {
    val size = with(LocalDensity.current) { (bubble * NUMBER_TEXT_SHARE).toSp() }
    Text(
        text = if (count > MAX_BADGE_NUMBER) "$MAX_BADGE_NUMBER+" else count.toString(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = size,
            lineHeight = size,
            fontWeight = FontWeight.Bold
        ),
        color = OnOriBadge,
        textAlign = TextAlign.Center,
        maxLines = 1,
        // At least round; a number of two or three digits widens it.
        modifier = modifier
            .sizeIn(minWidth = bubble, minHeight = bubble)
            .padding(horizontal = OriSpacing.ExtraSmall)
            .wrapContentSize(Alignment.Center)
    )
}

/** What the screen reader says for a tile: its name, and how many notifications when a badge shows. */
@Composable
internal fun tileDescription(label: String, count: Int): String {
    val isShown = count > 0 && LocalHomeBadges.current.style != BadgeStyle.OFF
    if (!isShown) return label
    return label + ", " + pluralStringResource(R.plurals.launcher_badge_notifications, count, count)
}
