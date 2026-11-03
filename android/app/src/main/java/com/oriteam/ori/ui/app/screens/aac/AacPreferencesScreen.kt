package com.oriteam.ori.ui.app.screens.aac

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.preferences.MessageMode
import com.oriteam.ori.domain.preferences.SymbolLabelMode
import com.oriteam.ori.domain.preferences.SymbolSize
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.settings.SettingsChoice
import com.oriteam.ori.ui.components.settings.SettingsChoiceGroup
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val sizeLabels = mapOf(
    SymbolSize.SMALL to R.string.aac_size_small,
    SymbolSize.MEDIUM to R.string.aac_size_medium,
    SymbolSize.LARGE to R.string.aac_size_large
)

private val labelModeLabels = mapOf(
    SymbolLabelMode.BELOW_IMAGE to R.string.aac_label_below,
    SymbolLabelMode.ABOVE_IMAGE to R.string.aac_label_above,
    SymbolLabelMode.TEXT_ONLY to R.string.aac_label_text_only,
    SymbolLabelMode.IMAGE_ONLY to R.string.aac_label_image_only
)

private val messageModeLabels = mapOf(
    MessageMode.COMPOSE to (R.string.aac_mode_compose to R.string.aac_mode_compose_description),
    MessageMode.COMPOSE_WITH_FEEDBACK to (R.string.aac_mode_feedback to R.string.aac_mode_feedback_description),
    MessageMode.SPEAK_IMMEDIATELY to (R.string.aac_mode_immediate to R.string.aac_mode_immediate_description)
)

/** How this device shows symbols and what a tap does, plus where the pictures come from. */
@Composable
fun AacPreferencesRoute(onNavigateBack: () -> Unit, viewModel: AacPreferencesViewModel = hiltViewModel()) {
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(R.string.aac_preferences), onNavigateBack = onNavigateBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                AacPreferenceChoices(viewModel)
                OriNotice(stringResource(R.string.aac_preferences_device_note))
                SectionHeader(text = stringResource(R.string.aac_licenses_title))
                OriNotice(
                    stringResource(R.string.aac_licenses_text),
                    modifier = Modifier.padding(bottom = OriSpacing.Huge)
                )
            }
        }
    }
}

/** The symbol choices; the general preferences screen shows the same ones. */
@Composable
fun AacPreferenceChoices(viewModel: AacPreferencesViewModel = hiltViewModel()) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    SettingsChoiceGroup(
        title = stringResource(R.string.aac_preference_mode),
        choices = MessageMode.entries.map { mode ->
            val (label, description) = messageModeLabels.getValue(mode)
            SettingsChoice(mode, stringResource(label), stringResource(description))
        },
        selectedValue = preferences.messageMode,
        onValueSelected = viewModel::onMessageModeChange
    )
    SettingsChoiceGroup(
        title = stringResource(R.string.aac_preference_size),
        choices = SymbolSize.entries.map { SettingsChoice(it, stringResource(sizeLabels.getValue(it))) },
        selectedValue = preferences.symbolSize,
        onValueSelected = viewModel::onSymbolSizeChange
    )
    SettingsChoiceGroup(
        title = stringResource(R.string.aac_preference_labels),
        choices = SymbolLabelMode.entries.map {
            SettingsChoice(it, stringResource(labelModeLabels.getValue(it)))
        },
        selectedValue = preferences.labelMode,
        onValueSelected = viewModel::onLabelModeChange
    )
}
