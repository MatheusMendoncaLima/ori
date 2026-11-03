package com.oriteam.ori.ui.app.screens.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.preferences.ContentDensity
import com.oriteam.ori.domain.preferences.ListPresentation
import com.oriteam.ori.domain.preferences.MotionLevel
import com.oriteam.ori.domain.preferences.StartArea
import com.oriteam.ori.domain.preferences.TextScale
import com.oriteam.ori.domain.preferences.ThemeMode
import com.oriteam.ori.ui.adaptive.CenteredContent
import com.oriteam.ori.ui.app.screens.aac.AacPreferenceChoices
import com.oriteam.ori.ui.components.form.FormMessage
import com.oriteam.ori.ui.components.form.FormMessageType
import com.oriteam.ori.ui.components.navigation.OriTopBar
import com.oriteam.ori.ui.components.settings.SettingsChoice
import com.oriteam.ori.ui.components.settings.SettingsChoiceGroup
import com.oriteam.ori.ui.components.settings.SettingsSwitchItem
import com.oriteam.ori.ui.theme.OriDimensions
import com.oriteam.ori.ui.theme.OriSpacing

private val textScaleLabels = mapOf(
    TextScale.NORMAL to R.string.preference_text_normal,
    TextScale.LARGE to R.string.preference_text_large,
    TextScale.LARGER to R.string.preference_text_larger,
    TextScale.LARGEST to R.string.preference_text_largest
)
private val themeLabels = mapOf(
    ThemeMode.SYSTEM to R.string.preference_theme_system,
    ThemeMode.LIGHT to R.string.preference_theme_light,
    ThemeMode.DARK to R.string.preference_theme_dark
)
private val motionLabels = mapOf(
    MotionLevel.SYSTEM to (R.string.preference_motion_system to R.string.preference_motion_system_description),
    MotionLevel.EXPRESSIVE to
        (R.string.preference_motion_expressive to R.string.preference_motion_expressive_description),
    MotionLevel.SUBTLE to (R.string.preference_motion_subtle to R.string.preference_motion_subtle_description),
    MotionLevel.REDUCED to (R.string.preference_motion_reduced to R.string.preference_motion_reduced_description),
    MotionLevel.OFF to (R.string.preference_motion_off to R.string.preference_motion_off_description)
)
private val densityLabels = mapOf(
    ContentDensity.COMPACT to R.string.preference_density_compact,
    ContentDensity.COMFORTABLE to R.string.preference_density_comfortable,
    ContentDensity.SPACIOUS to R.string.preference_density_spacious
)
private val listLabels = mapOf(
    ListPresentation.DETAILED to (R.string.preference_list_detailed to R.string.preference_list_detailed_description),
    ListPresentation.SUMMARY to (R.string.preference_list_summary to R.string.preference_list_summary_description)
)
private val startAreaLabels = mapOf(
    StartArea.HOME to R.string.start_area_home,
    StartArea.ROUTINE to R.string.main_my_routine,
    StartArea.COMMUNICATION to R.string.main_form_sentence
)

/** The groups of options, each one screen, as in the "Configurações" wireframe. */
enum class PreferenceSection { ACCESSIBILITY, THEME, COMMUNICATION, SCREEN }

private val sectionTitles = mapOf(
    PreferenceSection.ACCESSIBILITY to R.string.profile_accessibility,
    PreferenceSection.THEME to R.string.settings_theme,
    PreferenceSection.COMMUNICATION to R.string.profile_communication,
    PreferenceSection.SCREEN to R.string.settings_screen
)

/** Connects one group of options to its ViewModel. */
@Composable
fun PreferenceSectionRoute(
    section: PreferenceSection,
    onNavigateBack: () -> Unit,
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        OriTopBar(title = stringResource(sectionTitles.getValue(section)), onNavigateBack = onNavigateBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CenteredContent(maxWidth = OriDimensions.FormMaxWidth) {
                PreferencesContent(
                    section = section,
                    state = state,
                    actions = PreferencesActions(
                        onTextScale = viewModel::onTextScaleChange,
                        onTheme = viewModel::onThemeChange,
                        onSimplified = viewModel::onSimplifiedChange,
                        onMotion = viewModel::onMotionChange,
                        onDensity = viewModel::onDensityChange,
                        onListPresentation = viewModel::onListPresentationChange,
                        onStartArea = viewModel::onStartAreaChange,
                        onUppercase = viewModel::onUppercaseChange
                    ),
                    symbolChoices = { AacPreferenceChoices() }
                )
            }
        }
    }
}

/** What the screen can ask for; grouped so it receives one value instead of eight callbacks. */
data class PreferencesActions(
    val onTextScale: (TextScale) -> Unit,
    val onTheme: (ThemeMode) -> Unit,
    val onSimplified: (Boolean) -> Unit,
    val onMotion: (MotionLevel) -> Unit,
    val onDensity: (ContentDensity) -> Unit,
    val onListPresentation: (ListPresentation) -> Unit,
    val onStartArea: (StartArea) -> Unit,
    val onUppercase: (Boolean) -> Unit
)

/** The options of one section. See docs/funcionalidades/preferencias.md for what follows the account. */
@Composable
fun PreferencesContent(
    section: PreferenceSection,
    state: PreferencesUiState,
    actions: PreferencesActions,
    symbolChoices: @Composable () -> Unit
) {
    FormMessage(messageRes = state.errorRes, type = FormMessageType.ERROR)
    FormMessage(messageRes = state.noticeRes, type = FormMessageType.SUCCESS)
    when (section) {
        PreferenceSection.ACCESSIBILITY -> AccessibilityChoices(state, actions)
        PreferenceSection.THEME -> SettingsChoiceGroup(
            title = stringResource(R.string.preference_theme),
            choices = ThemeMode.entries.map { SettingsChoice(it, stringResource(themeLabels.getValue(it))) },
            selectedValue = state.account.theme,
            onValueSelected = actions.onTheme
        )
        PreferenceSection.COMMUNICATION -> symbolChoices()
        PreferenceSection.SCREEN -> ScreenChoices(state, actions)
    }
    Spacer(modifier = Modifier.height(OriSpacing.Huge))
}

@Composable
private fun AccessibilityChoices(state: PreferencesUiState, actions: PreferencesActions) {
    SettingsChoiceGroup(
        title = stringResource(R.string.preference_text),
        choices = TextScale.entries.map { SettingsChoice(it, stringResource(textScaleLabels.getValue(it))) },
        selectedValue = state.account.textScale,
        onValueSelected = actions.onTextScale
    )
    SettingsSwitchItem(
        title = stringResource(R.string.account_uppercase),
        description = stringResource(R.string.account_uppercase_description),
        checked = state.device.isUppercase,
        onCheckedChange = actions.onUppercase
    )
    SettingsSwitchItem(
        title = stringResource(R.string.preference_simplified),
        description = stringResource(R.string.preference_simplified_description),
        checked = state.account.isSimplified,
        onCheckedChange = actions.onSimplified
    )
    SettingsChoiceGroup(
        title = stringResource(R.string.preference_motion),
        choices = MotionLevel.entries.map { level ->
            val (label, description) = motionLabels.getValue(level)
            SettingsChoice(level, stringResource(label), stringResource(description))
        },
        selectedValue = state.account.motion,
        onValueSelected = actions.onMotion
    )
}

@Composable
private fun ScreenChoices(state: PreferencesUiState, actions: PreferencesActions) {
    SettingsChoiceGroup(
        title = stringResource(R.string.preference_start_area),
        choices = StartArea.entries.map { SettingsChoice(it, stringResource(startAreaLabels.getValue(it))) },
        selectedValue = state.device.startArea,
        onValueSelected = actions.onStartArea
    )
    SettingsChoiceGroup(
        title = stringResource(R.string.preference_density),
        choices = ContentDensity.entries.map { SettingsChoice(it, stringResource(densityLabels.getValue(it))) },
        selectedValue = state.device.density,
        onValueSelected = actions.onDensity
    )
    SettingsChoiceGroup(
        title = stringResource(R.string.preference_lists),
        choices = ListPresentation.entries.map { presentation ->
            val (label, description) = listLabels.getValue(presentation)
            SettingsChoice(presentation, stringResource(label), stringResource(description))
        },
        selectedValue = state.device.listPresentation,
        onValueSelected = actions.onListPresentation
    )
}
