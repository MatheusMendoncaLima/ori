package com.oriteam.ori.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Light-mode preview on a Pixel 5, shared by every screen. */
@Preview(
    name = "Light",
    showBackground = true,
    device = "id:pixel_5",
    showSystemUi = true
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class OriPreview

/** Dark-mode variant of [OriPreview]. */
@Preview(
    name = "Dark",
    showBackground = true,
    device = "id:pixel_5",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class OriPreviewDark
