package com.oriteam.ori.ui.launcher

import android.content.pm.ActivityInfo

/** The smallest side of a tablet, in dp, as Android counts it. */
private const val TABLET_SMALLEST_WIDTH_DP = 600

/**
 * How ORI Home turns, as One UI does by default: on a phone (smallest side under 600 dp) the home screen stays
 * upright, unless the person turns on "Girar a tela inicial no celular"; a tablet turns, with its grid transposed
 * (HomeMetrics). Android itself
 * lets large screens turn whatever an app asks, so the tablet rule is only Android's own.
 */
internal fun homeOrientationFor(smallestWidthDp: Int, turnsOnPhone: Boolean = false): Int =
    if (isPhone(smallestWidthDp) && !turnsOnPhone) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

/** A phone, as Android counts it: the smallest side under 600 dp. */
internal fun isPhone(smallestWidthDp: Int): Boolean = smallestWidthDp < TABLET_SMALLEST_WIDTH_DP
