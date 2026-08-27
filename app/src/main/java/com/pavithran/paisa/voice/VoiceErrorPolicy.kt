package com.pavithran.paisa.voice

/**
 * Which recogniser is being used for an attempt.
 *
 * [ON_DEVICE] needs a downloaded language pack, [SYSTEM_SERVICE] is the bound
 * recognition service, and [SYSTEM_DIALOG] is the system's own "Speak now"
 * screen, which works on phones that expose no bindable service at all.
 */
enum class VoiceEngine { ON_DEVICE, SYSTEM_SERVICE, SYSTEM_DIALOG }

/** What to do after a failed attempt. */
sealed interface VoiceRecovery {
    /** Try again with [engine]; [notice] explains the switch to the user. */
    data class Retry(val engine: VoiceEngine, val notice: String? = null) : VoiceRecovery

    /** Give up on this attempt and show [message]. */
    data class Fail(val message: String) : VoiceRecovery

    /** The microphone permission has to be granted before anything else. */
    data object NeedsPermission : VoiceRecovery
}

/**
 * The error codes [android.speech.SpeechRecognizer] reports, plus a synthetic
 * code for "no recogniser could even be created". Mirrored here so the policy
 * stays free of Android imports and unit-testable on the JVM.
 */
object VoiceError {
    const val NONE_AVAILABLE = -1
    const val NETWORK_TIMEOUT = 1
    const val NETWORK = 2
    const val AUDIO = 3
    const val SERVER = 4
    const val CLIENT = 5
    const val SPEECH_TIMEOUT = 6
    const val NO_MATCH = 7
    const val RECOGNIZER_BUSY = 8
    const val INSUFFICIENT_PERMISSIONS = 9
    const val TOO_MANY_REQUESTS = 10
    const val SERVER_DISCONNECTED = 11
    const val LANGUAGE_NOT_SUPPORTED = 12
    const val LANGUAGE_UNAVAILABLE = 13
    const val CANNOT_CHECK_SUPPORT = 14
    const val CANNOT_LISTEN_TO_DOWNLOAD_EVENTS = 15
}

/**
 * Decides what happens after a recognition failure.
 *
 * The important case is the offline recogniser: a phone can report on-device
 * recognition as available while the language pack is missing, so the first
 * attempt fails with a language error. Falling back to the bound service, and
 * then to the system dialog, is what makes the mic work on a real phone
 * instead of showing a raw error code.
 */
object VoiceErrorPolicy {

    /** Attempts are capped so a failing phone can never loop forever. */
    const val MAX_ATTEMPTS = 3

    fun recover(error: Int, engine: VoiceEngine, attempt: Int): VoiceRecovery {
        if (error == VoiceError.INSUFFICIENT_PERMISSIONS) return VoiceRecovery.NeedsPermission

        if (attempt >= MAX_ATTEMPTS) return VoiceRecovery.Fail(messageFor(error, engine))

        return when (error) {
            VoiceError.NONE_AVAILABLE -> when (engine) {
                VoiceEngine.SYSTEM_DIALOG -> VoiceRecovery.Fail(
                    "This phone has no speech recognition. Type the expense instead."
                )
                else -> VoiceRecovery.Retry(VoiceEngine.SYSTEM_DIALOG)
            }

            // The offline pack is missing or unusable: this is the common one.
            VoiceError.LANGUAGE_NOT_SUPPORTED,
            VoiceError.LANGUAGE_UNAVAILABLE,
            VoiceError.CANNOT_CHECK_SUPPORT,
            VoiceError.CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> when (engine) {
                VoiceEngine.ON_DEVICE -> VoiceRecovery.Retry(VoiceEngine.SYSTEM_SERVICE)
                VoiceEngine.SYSTEM_SERVICE -> VoiceRecovery.Retry(VoiceEngine.SYSTEM_DIALOG)
                VoiceEngine.SYSTEM_DIALOG -> VoiceRecovery.Fail(
                    "Indian English isn't installed for speech. Install it in Settings › Language."
                )
            }

            VoiceError.NETWORK, VoiceError.NETWORK_TIMEOUT -> when (engine) {
                VoiceEngine.ON_DEVICE -> VoiceRecovery.Fail(
                    "Offline recognition failed — try again"
                )
                else -> VoiceRecovery.Retry(
                    VoiceEngine.ON_DEVICE,
                    notice = "No network — trying offline recognition"
                )
            }

            VoiceError.SERVER, VoiceError.SERVER_DISCONNECTED -> when (engine) {
                VoiceEngine.ON_DEVICE -> VoiceRecovery.Fail("Speech service error — try again")
                else -> VoiceRecovery.Retry(VoiceEngine.ON_DEVICE)
            }

            // Some phones abort the first bind; move down the chain rather than
            // showing the user a bare "client error".
            VoiceError.CLIENT -> when (engine) {
                VoiceEngine.ON_DEVICE -> VoiceRecovery.Retry(VoiceEngine.SYSTEM_SERVICE)
                VoiceEngine.SYSTEM_SERVICE -> VoiceRecovery.Retry(VoiceEngine.SYSTEM_DIALOG)
                VoiceEngine.SYSTEM_DIALOG -> VoiceRecovery.Fail("Recognition stopped — try again")
            }

            // Retrying the same engine is right here: the previous session just
            // needed tearing down first.
            VoiceError.RECOGNIZER_BUSY -> VoiceRecovery.Retry(engine)

            VoiceError.TOO_MANY_REQUESTS -> VoiceRecovery.Fail(
                "Too many requests just now — try again in a moment"
            )

            // Not worth a retry: the user simply didn't say anything usable.
            VoiceError.NO_MATCH -> VoiceRecovery.Fail("Didn't catch that — try again")
            VoiceError.SPEECH_TIMEOUT -> VoiceRecovery.Fail("No speech detected")
            VoiceError.AUDIO -> VoiceRecovery.Fail("Microphone problem — try again")

            else -> VoiceRecovery.Fail(messageFor(error, engine))
        }
    }

    fun messageFor(error: Int, engine: VoiceEngine): String = when (error) {
        VoiceError.NO_MATCH -> "Didn't catch that — try again"
        VoiceError.SPEECH_TIMEOUT -> "No speech detected"
        VoiceError.AUDIO -> "Microphone problem — try again"
        VoiceError.RECOGNIZER_BUSY -> "The recogniser is busy — tap the mic again"
        VoiceError.INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed to log by voice"
        VoiceError.NETWORK, VoiceError.NETWORK_TIMEOUT ->
            if (engine == VoiceEngine.ON_DEVICE) {
                "Offline recognition failed — try again"
            } else {
                "No network for speech recognition"
            }
        VoiceError.SERVER, VoiceError.SERVER_DISCONNECTED -> "Speech service error — try again"
        VoiceError.TOO_MANY_REQUESTS -> "Too many requests just now — try again in a moment"
        VoiceError.LANGUAGE_NOT_SUPPORTED,
        VoiceError.LANGUAGE_UNAVAILABLE,
        VoiceError.CANNOT_CHECK_SUPPORT,
        VoiceError.CANNOT_LISTEN_TO_DOWNLOAD_EVENTS ->
            "Speech recognition for Indian English isn't installed on this phone"
        VoiceError.NONE_AVAILABLE ->
            "This phone has no speech recognition. Type the expense instead."
        VoiceError.CLIENT -> "Recognition stopped — try again"
        else -> "Speech recognition failed (error $error)"
    }
}
