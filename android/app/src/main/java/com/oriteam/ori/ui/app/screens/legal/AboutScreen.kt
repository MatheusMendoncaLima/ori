package com.oriteam.ori.ui.app.screens.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.oriteam.ori.BuildConfig
import com.oriteam.ori.R
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.card.OriCard
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.text.Text
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

/**
 * What ORI is, who makes it, that it is not a medical device (J8), the documents and the third-party licenses (J9).
 * Every new font, icon set or library goes into [OpenSourceLicenses] (AGENTS.md, section 14).
 */
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(
            maxWidth = OriDimensions.FormMaxWidth,
            title = stringResource(R.string.settings_about),
            onNavigateBack = onNavigateBack
        )
        // Scrolls, so enlarged text in a short window is never cut.
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                OriCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.about_description), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(OriSpacing.Small))
                    Text(text = stringResource(R.string.about_team), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(OriSpacing.Small))
                    Text(
                        text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(OriSpacing.Medium))
                OriCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.about_not_medical), style = MaterialTheme.typography.bodyMedium)
                }
                OriTextButton(
                    text = stringResource(R.string.consent_read_privacy),
                    onClick = { uriHandler.openUri(LegalLinks.privacyPolicy()) }
                )
                OriTextButton(
                    text = stringResource(R.string.consent_read_terms),
                    onClick = { uriHandler.openUri(LegalLinks.termsOfUse()) }
                )
                SectionHeader(text = stringResource(R.string.about_licenses))
                OpenSourceLicenses.entries.forEach { license ->
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = OriSpacing.Small)) {
                        Text(text = license.component, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = license.license,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.about_emoji_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(OriSpacing.Large))
            }
        }
    }
}

/** The third-party parts shipped in the app and their licenses (names are not translated). */
enum class OpenSourceLicenses(val component: String, val license: String) {
    FONTS("Nunito, Nunito Sans", "SIL Open Font License 1.1"),
    MATERIAL_ICONS("Material Icons, Material Symbols", "Apache License 2.0"),
    ANDROIDX("AndroidX, Jetpack Compose, Room, DataStore, Credentials", "Apache License 2.0"),
    KOTLIN("Kotlin, kotlinx.coroutines", "Apache License 2.0"),
    HILT("Dagger, Hilt", "Apache License 2.0"),
    FIREBASE("Firebase Android SDK, Google Identity", "Apache License 2.0")
}
