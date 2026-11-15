package com.oriteam.ori.ui.app.screens.aac

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oriteam.ori.R
import com.oriteam.ori.domain.aac.SpeechState
import com.oriteam.ori.domain.aac.VoiceOption
import com.oriteam.ori.domain.preferences.MAX_VOICE_PITCH
import com.oriteam.ori.domain.preferences.MAX_VOICE_RATE
import com.oriteam.ori.domain.preferences.MIN_VOICE_PITCH
import com.oriteam.ori.domain.preferences.MIN_VOICE_RATE
import com.oriteam.ori.domain.preferences.VoicePreset
import com.oriteam.ori.domain.preferences.VoiceSound
import com.oriteam.ori.ui.components.form.EqualWidthRow
import com.oriteam.ori.ui.components.form.OriChoiceChip
import com.oriteam.ori.ui.components.form.OriLabeledSlider
import com.oriteam.ori.ui.components.form.OriNotice
import com.oriteam.ori.ui.components.form.OriPrimaryButton
import com.oriteam.ori.ui.components.form.OriSecondaryButton
import com.oriteam.ori.ui.components.form.OriTextButton
import com.oriteam.ori.ui.components.list.SectionHeader
import com.oriteam.ori.ui.components.settings.SettingsChoice
import com.oriteam.ori.ui.components.settings.SettingsChoiceGroup
import com.oriteam.ori.ui.theme.LocalOriAppearance
import com.oriteam.ori.ui.theme.OriSpacing
import com.oriteam.ori.ui.theme.appear
import com.oriteam.ori.ui.theme.disappear
import kotlin.math.roundToInt

private const val TAG = "OriVoiceSettings"
private const val PERCENT = 100

/** The system screen of the voice engine, where more voices are installed. Not a public constant of Android. */
private const val TTS_SETTINGS_ACTION = "com.android.settings.TTS_SETTINGS"

private val presetLabels = mapOf(
    VoicePreset.DEEP to R.string.aac_voice_preset_deep,
    VoicePreset.NATURAL to R.string.aac_voice_preset_natural,
    VoicePreset.HIGH to R.string.aac_voice_preset_high,
    VoicePreset.CHILD to R.string.aac_voice_preset_child
)

/**
 * "Voz": the voice of the communication boards. A style for a quick choice, the voices installed on this
 * device to listen to and pick, and tone and speed for the fine adjustment. Everything plays an example.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSettingsSection(viewModel: VoiceSettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sample = stringResource(R.string.aac_voice_sample)
    SectionHeader(text = stringResource(R.string.aac_voice_title))
    if (state.speechState == SpeechState.UNAVAILABLE) {
        OriNotice(
            stringResource(R.string.aac_voice_unavailable, stringResource(R.string.aac_speak)),
            isLive = true
        )
        OriTextButton(text = stringResource(R.string.aac_voice_install), onClick = { openVoiceInstaller(context) })
        return
    }
    if (state.speechState == SpeechState.PREPARING) {
        OriNotice(stringResource(R.string.aac_voice_preparing), isLive = true)
    }
    OriNotice(stringResource(R.string.aac_voice_note))

    SectionHeader(text = stringResource(R.string.aac_voice_style), modifier = Modifier.padding(top = OriSpacing.Small))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = Modifier.fillMaxWidth().selectableGroup()
    ) {
        VoicePreset.entries.forEach { preset ->
            OriChoiceChip(
                label = stringResource(presetLabels.getValue(preset)),
                isSelected = state.settings.preset == preset,
                onClick = { viewModel.onPresetSelected(preset, sample) }
            )
        }
    }
    OriNotice(stringResource(R.string.aac_voice_style_note))
    VoiceSoundChoices(state.settings.sound) { viewModel.onSoundSelected(it, sample) }

    OriLabeledSlider(
        label = stringResource(R.string.aac_voice_pitch),
        value = state.settings.pitch,
        valueText = percentText(state.settings.pitch),
        range = MIN_VOICE_PITCH..MAX_VOICE_PITCH,
        startLabel = stringResource(R.string.aac_voice_pitch_low),
        endLabel = stringResource(R.string.aac_voice_pitch_high),
        onValueChange = viewModel::onPitchChange,
        onValueChangeFinished = { viewModel.onAdjustmentFinished(sample) }
    )
    OriLabeledSlider(
        label = stringResource(R.string.aac_voice_rate),
        value = state.settings.rate,
        valueText = percentText(state.settings.rate),
        range = MIN_VOICE_RATE..MAX_VOICE_RATE,
        startLabel = stringResource(R.string.aac_voice_rate_slow),
        endLabel = stringResource(R.string.aac_voice_rate_fast),
        onValueChange = viewModel::onRateChange,
        onValueChangeFinished = { viewModel.onAdjustmentFinished(sample) }
    )
    EqualWidthRow(modifier = Modifier.padding(top = OriSpacing.Small)) {
        OriPrimaryButton(text = stringResource(R.string.aac_voice_preview), onClick = { viewModel.onPreview(sample) })
        OriSecondaryButton(text = stringResource(R.string.aac_voice_reset), onClick = { viewModel.onReset(sample) })
    }
    if (state.voices.isNotEmpty()) {
        DeviceVoices(
            voices = state.voices,
            selectedId = state.settings.voiceId.takeIf { id -> state.voices.any { it.id == id } },
            onVoiceSelected = { viewModel.onVoiceSelected(it, sample) }
        )
    }
    OriTextButton(text = stringResource(R.string.aac_voice_install), onClick = { openVoiceInstaller(context) })
}

/**
 * "Volume da voz": the volume of the device the voice plays at, media or the ringtone, with what each one means. The
 * volume buttons change that one while this screen is shown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceSoundChoices(sound: VoiceSound, onSelect: (VoiceSound) -> Unit) {
    VoiceVolumeKeys(sound)
    SectionHeader(text = stringResource(R.string.aac_voice_sound), modifier = Modifier.padding(top = OriSpacing.Small))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(OriSpacing.Small),
        modifier = Modifier.fillMaxWidth().selectableGroup()
    ) {
        VoiceSound.entries.forEach { option ->
            OriChoiceChip(
                label = stringResource(soundLabels.getValue(option)),
                isSelected = sound == option,
                onClick = { onSelect(option) }
            )
        }
    }
    OriNotice(stringResource(R.string.aac_voice_sound_note))
}

private val soundLabels = mapOf(
    VoiceSound.MEDIA to R.string.aac_voice_sound_media,
    VoiceSound.RING to R.string.aac_voice_sound_ring
)

/**
 * The voices of the device, in a list that opens on request: there may be many, and open they pushed tone
 * and speed far down the screen. Closed, the row says which one is in use.
 */
@Composable
private fun DeviceVoices(voices: List<VoiceOption>, selectedId: String?, onVoiceSelected: (String?) -> Unit) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    val choices = voiceChoices(voices)
    val current = choices.first { it.value == selectedId }.label
    SectionHeader(text = stringResource(R.string.aac_voice_choice), modifier = Modifier.padding(top = OriSpacing.Small))
    OriNotice(stringResource(R.string.aac_voice_current, current))
    OriTextButton(
        text = if (isOpen) {
            stringResource(R.string.aac_voice_hide_list)
        } else {
            pluralStringResource(R.plurals.aac_voice_show_list, voices.size, voices.size)
        },
        onClick = { isOpen = !isOpen }
    )
    val motion = LocalOriAppearance.current.motion
    AnimatedVisibility(visible = isOpen, enter = motion.appear(), exit = motion.disappear(), label = "Voices") {
        SettingsChoiceGroup(
            title = stringResource(R.string.aac_voice_choice),
            choices = choices,
            selectedValue = selectedId,
            onValueSelected = onVoiceSelected
        )
    }
}

/** Engines give technical names only ("pt-br-x-afs-local"), so the voices are numbered, local ones first. */
@Composable
private fun voiceChoices(voices: List<VoiceOption>): List<SettingsChoice<String?>> =
    listOf(SettingsChoice<String?>(null, stringResource(R.string.aac_voice_default))) +
        voices.mapIndexed { index, voice ->
            val label = if (voice.needsNetwork) R.string.aac_voice_numbered_network else R.string.aac_voice_numbered
            SettingsChoice<String?>(voice.id, stringResource(label, index + 1))
        }

/** 1 is said as "100%": the voice as it comes. */
@Composable
private fun percentText(
    value: Float
): String = stringResource(R.string.aac_voice_percent, (value * PERCENT).roundToInt())

/** The voice engine's own screen, where more voices are downloaded; the engine's installer if there is none. */
private fun openVoiceInstaller(context: Context) {
    val intents = listOf(Intent(TTS_SETTINGS_ACTION), Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (exception: ActivityNotFoundException) {
            Log.w(TAG, "No screen for ${intent.action}: ${exception.javaClass.simpleName}")
        }
    }
}
