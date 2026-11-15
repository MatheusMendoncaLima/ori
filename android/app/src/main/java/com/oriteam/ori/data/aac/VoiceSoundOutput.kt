package com.oriteam.ori.data.aac

import android.media.AudioAttributes
import android.media.AudioManager
import com.oriteam.ori.domain.preferences.VoiceSound

/**
 * The stream of Android a voice of [VoiceSound] plays on: its volume decides whether it is heard, and the volume
 * buttons change it while a screen with the voice is shown. "Mídia" is the stream of music and videos; "Toque" the
 * one of the ringtone (the bell of One UI's volume control).
 */
val VoiceSound.streamType: Int
    get() = when (this) {
        VoiceSound.MEDIA -> AudioManager.STREAM_MUSIC
        VoiceSound.RING -> AudioManager.STREAM_RING
    }

/** What the voice engine is told it plays: speech, on the stream of [VoiceSound]. */
internal val VoiceSound.audioAttributes: AudioAttributes
    get() = AudioAttributes.Builder()
        .setUsage(
            when (this) {
                VoiceSound.MEDIA -> AudioAttributes.USAGE_MEDIA
                VoiceSound.RING -> AudioAttributes.USAGE_NOTIFICATION_RINGTONE
            }
        )
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
