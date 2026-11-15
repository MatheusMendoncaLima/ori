package com.oriteam.ori.data.launcher

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A widget waits for its app's setup only when the app requires it: one whose setup is optional (the clocks of One UI)
 * goes on the home screen at once, as One UI does, and its setup opens later from its menu.
 */
@RunWith(AndroidJUnit4::class)
class WidgetSetupRuleTest {
    private fun widget(hasSetup: Boolean, features: Int = 0) = AppWidgetProviderInfo().apply {
        if (hasSetup) configure = ComponentName("app.clock", "app.clock.Setup")
        widgetFeatures = features
    }

    @Test
    fun `a required setup is waited for, an optional one or none is not`() {
        assertTrue(needsSetupOf(widget(hasSetup = true)))
        assertFalse(needsSetupOf(widget(hasSetup = true, AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL)))
        assertFalse(needsSetupOf(widget(hasSetup = false)))
    }
}
