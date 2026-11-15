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
    /** All the apps (a grid of dots), as on the home screens of Android. */
    val Apps = icon(
        "Apps",
        "M4,8h4V4H4V8zM10,20h4v-4h-4V20zM4,20h4v-4H4V20zM4,14h4v-4H4V14zM10,14h4v-4h-4V14zM16,4v4h4V4H16z" +
            "M10,8h4V4h-4V8zM16,14h4v-4h-4V14zM16,20h4v-4h-4V20z"
    )

    /** Widgets of other apps on the home screen. */
    val Widgets = icon(
        "Widgets",
        "M13,13v8h8v-8h-8zM3,21h8v-8L3,13v8zM3,3v8h8L11,3L3,3zM16.66,1.69L11,7.34 16.66,13l5.66,-5.66 -5.66,-5.65z"
    )

    /** A new folder on the home screen ("Criar pasta"). */
    val CreateFolder = icon(
        "CreateFolder",
        "M20,6h-8l-2,-2L4,4c-1.11,0 -1.99,0.89 -1.99,2L2,18c0,1.11 0.89,2 2,2h16c1.11,0 2,-0.89 2,-2L22,8" +
            "c0,-1.11 -0.89,-2 -2,-2zM19,14h-3v3h-2v-3h-3v-2h3L14,9h2v3h3v2z"
    )

    /** An app taken out of its folder, onto the home screen ("Tirar da pasta"). */
    val TakeOutOfFolder = icon(
        "TakeOutOfFolder",
        "M20,6h-8l-2,-2L4,4c-1.11,0 -1.99,0.89 -1.99,2L2,18c0,1.11 0.89,2 2,2h16c1.11,0 2,-0.89 2,-2L22,8" +
            "c0,-1.11 -0.89,-2 -2,-2zM16,13h-3v4h-2v-4L8,13l4,-4 4,4z"
    )

    /** Apps hidden from the home screen and all the apps ("Ocultar apps"). */
    val HideApps = icon(
        "HideApps",
        "M12,7c2.76,0 5,2.24 5,5 0,0.65 -0.13,1.26 -0.36,1.83l2.92,2.92c1.51,-1.26 2.7,-2.89 3.43,-4.75 " +
            "-1.73,-4.39 -6,-7.5 -11,-7.5 -1.4,0 -2.74,0.25 -3.98,0.7l2.16,2.16C10.74,7.13 11.35,7 12,7z" +
            "M2,4.27l2.28,2.28 0.46,0.46C3.08,8.3 1.78,10.02 1,12c1.73,4.39 6,7.5 11,7.5 1.55,0 3.03,-0.3 " +
            "4.38,-0.84l0.42,0.42L19.73,22 21,20.73 3.27,3 2,4.27zM7.53,9.8l1.55,1.55c-0.05,0.21 -0.08,0.43 " +
            "-0.08,0.65 0,1.66 1.34,3 3,3 0.22,0 0.44,-0.03 0.65,-0.08l1.55,1.55c-0.67,0.33 -1.41,0.53 -2.2,0.53 " +
            "-2.76,0 -5,-2.24 -5,-5 0,-0.79 0.2,-1.53 0.53,-2.2zM11.84,9.02l3.15,3.15 0.02,-0.16c0,-1.66 " +
            "-1.34,-3 -3,-3l-0.17,0.01z"
    )

    /** A widget grows or shrinks ("Redimensionar"). */
    val Resize = icon("Resize", "M21,11V3h-8l3.29,3.29l-10,10L3,13v8h8l-3.29,-3.29l10,-10L21,11z")

    /** One less (the size of a widget). */
    val Remove = icon("Remove", "M19,13H5v-2h14v2z")

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
