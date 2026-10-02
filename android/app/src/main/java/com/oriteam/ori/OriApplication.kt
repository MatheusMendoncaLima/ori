package com.oriteam.ori

import android.app.Application
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OriApplication : Application()

@Preview(
    name = "homePreview",
    showBackground = true,
    device = "id:pixel_5",
    showSystemUi = true
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class OriPreview
@Preview(
    name = "homePreview",
    showBackground = true,
    device = "id:pixel_5",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class OriPreviewDark