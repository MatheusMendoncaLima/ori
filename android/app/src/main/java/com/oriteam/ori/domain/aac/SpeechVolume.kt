package com.oriteam.ori.domain.aac

import com.oriteam.ori.domain.preferences.VoiceSound

/**
 * The volume the voice of the boards plays at (the one of its [VoiceSound]). With it at zero (or muted), a message is
 * "said" and nobody hears it: the screen warns, as a messaging app does with a voice note on a silent phone, and
 * offers to turn it up.
 */
interface SpeechVolume {
    /** Whether a voice playing at [sound] can not be heard now: that volume at zero, or muted. */
    fun isMuted(sound: VoiceSound): Boolean

    /** Turns the volume of [sound] up to a volume that is heard, showing Android's volume control. */
    fun raise(sound: VoiceSound)
}
