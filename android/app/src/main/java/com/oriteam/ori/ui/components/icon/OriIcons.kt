package com.oriteam.ori.ui.components.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val ICON_SIZE = 24f

/**
 * Icons missing from the Material core set, each with one meaning across the app. The extended
 * icon library has thousands of them, too heavy for the few needed here.
 */
object OriIcons {
    val Accessibility = icon(
        "Accessibility",
        "M12,2c1.1,0 2,0.9 2,2s-0.9,2 -2,2 -2,-0.9 -2,-2 0.9,-2 2,-2zM21,9h-6v13h-2v-6h-2v6L9,22L9,9L3,9L3,7h18v2z"
    )
    val Chat = icon(
        "Chat",
        "M20,2L4,2c-1.1,0 -1.99,0.9 -1.99,2L2,22l4,-4h14c1.1,0 2,-0.9 2,-2L22,4c0,-1.1 -0.9,-2 -2,-2z" +
            "M18,14L6,14v-2h12v2zM18,11L6,11L6,9h12v2zM18,8L6,8L6,6h12v2z"
    )
    val Palette = icon(
        "Palette",
        "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9c0.83,0 1.5,-0.67 1.5,-1.5 0,-0.39 -0.15,-0.74 -0.39,-1.01 " +
            "-0.23,-0.26 -0.38,-0.61 -0.38,-0.99 0,-0.83 0.67,-1.5 1.5,-1.5L16,16c2.76,0 5,-2.24 5,-5 0,-4.42 " +
            "-4.03,-8 -9,-8zM6.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,9 6.5,9 8,9.67 8,10.5 7.33,12 6.5,12z" +
            "M9.5,8C8.67,8 8,7.33 8,6.5S8.67,5 9.5,5s1.5,0.67 1.5,1.5S10.33,8 9.5,8z" +
            "M14.5,8c-0.83,0 -1.5,-0.67 -1.5,-1.5S13.67,5 14.5,5s1.5,0.67 1.5,1.5S15.33,8 14.5,8z" +
            "M17.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S16.67,9 17.5,9s1.5,0.67 1.5,1.5S18.33,12 17.5,12z"
    )
    val People = icon(
        "People",
        "M16,11c1.66,0 2.99,-1.34 2.99,-3S17.66,5 16,5c-1.66,0 -3,1.34 -3,3s1.34,3 3,3z" +
            "M8,11c1.66,0 2.99,-1.34 2.99,-3S9.66,5 8,5C6.34,5 5,6.34 5,8s1.34,3 3,3z" +
            "M8,13c-2.33,0 -7,1.17 -7,3.5L1,19h14v-2.5c0,-2.33 -4.67,-3.5 -7,-3.5z" +
            "M16,13c-0.29,0 -0.62,0.02 -0.97,0.05 1.16,0.84 1.97,1.97 1.97,3.45L17,19h6v-2.5" +
            "c0,-2.33 -4.67,-3.5 -7,-3.5z"
    )
    val Smartphone = icon(
        "Smartphone",
        "M17,1.01L7,1c-1.1,0 -2,0.9 -2,2v18c0,1.1 0.9,2 2,2h10c1.1,0 2,-0.9 2,-2V3c0,-1.1 -0.9,-1.99 -2,-1.99z" +
            "M17,19H7V5h10v14z"
    )

    private fun icon(name: String, path: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = ICON_SIZE.dp,
        defaultHeight = ICON_SIZE.dp,
        viewportWidth = ICON_SIZE,
        viewportHeight = ICON_SIZE
    ).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()
}
