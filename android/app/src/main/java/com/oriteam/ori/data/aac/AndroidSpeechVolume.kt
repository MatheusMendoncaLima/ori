package com.oriteam.ori.data.aac

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.oriteam.ori.domain.aac.SpeechVolume
import com.oriteam.ori.domain.preferences.VoiceSound
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "OriSpeechVolume"

/** "Aumentar o volume" brings the voice to at least half of the volume: one step from zero is barely heard. */
private const val HEARD_SHARE = 2

/**
 * The volume of the stream the voice plays on ([streamType]: media, or the ringtone). Only that volume decides
 * whether the voice is heard: on "Mídia" a phone on silent still speaks; on "Toque" it is silent too. Reading it is
 * no data about the person and needs no permission.
 */
class AndroidSpeechVolume @Inject constructor(@ApplicationContext context: Context) : SpeechVolume {
    private val audio = context.getSystemService(AudioManager::class.java)

    override fun isMuted(sound: VoiceSound): Boolean {
        val stream = sound.streamType
        return audio.isStreamMute(stream) || audio.getStreamVolume(stream) == 0
    }

    override fun raise(sound: VoiceSound) {
        val stream = sound.streamType
        val heard = maxOf(audio.getStreamVolume(stream), audio.getStreamMaxVolume(stream) / HEARD_SHARE)
        try {
            if (audio.isStreamMute(stream)) audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
            audio.setStreamVolume(stream, heard, AudioManager.FLAG_SHOW_UI)
        } catch (refused: SecurityException) {
            // Taking the ringtone out of "Não perturbe" needs an access ORI does not ask for: Android's volume
            // control opens on that volume instead, and the person turns it up there.
            Log.w(TAG, "Volume change refused: ${refused.javaClass.simpleName}")
            audio.adjustStreamVolume(stream, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        }
    }
}
