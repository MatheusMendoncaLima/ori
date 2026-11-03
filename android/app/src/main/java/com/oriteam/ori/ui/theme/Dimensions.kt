package com.oriteam.ori.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing scale. Use these steps instead of arbitrary values so screens keep the same rhythm. */
object OriSpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val Huge = 48.dp
}

/** Fixed sizes shared by components. */
object OriDimensions {
    /** Minimum touch area recommended by Android accessibility guidelines. */
    val MinTouchTarget = 48.dp

    /** Minimum height of fields and primary buttons; they grow with enlarged text. */
    val ControlMinHeight = 52.dp

    /** Forms are easier to read and fill when lines do not span a whole tablet. */
    val FormMaxWidth = 480.dp

    /** Maximum width of reading content (lists, settings) inside a large pane. */
    val ContentMaxWidth = 720.dp
}

/** Corner radii of the original screens: fields 14 dp, cards 20 dp, buttons fully rounded. */
internal val OriShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
