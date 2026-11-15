package com.oriteam.ori.ui.app.screens.aac

import android.media.AudioManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.oriteam.ori.data.aac.streamType
import com.oriteam.ori.domain.preferences.VoiceSound

/**
 * While a screen with the voice is shown, the volume buttons of the device change the volume the voice plays at
 * ([sound]). Without it, with nothing playing, One UI's buttons moved the ringtone (the bell), whatever the voice
 * played at. When the screen goes, Android chooses again.
 */
@Composable
internal fun VoiceVolumeKeys(sound: VoiceSound) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, sound) {
        activity.volumeControlStream = sound.streamType
        onDispose { activity.volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE }
    }
}
