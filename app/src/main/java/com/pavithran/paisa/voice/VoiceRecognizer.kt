package com.pavithran.paisa.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Hearing(val partial: String) : VoiceState
    data object Processing : VoiceState
    data class Failed(val message: String) : VoiceState
}

/**
 * Thin wrapper over [SpeechRecognizer]. Every callback is implemented — an
 * unimplemented onError is why a mic button silently does nothing.
 */
class VoiceRecognizer(
    private val context: Context,
    private val onState: (VoiceState) -> Unit,
    private val onFinalText: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var usingOnDevice = false

    private fun create(preferOnDevice: Boolean): SpeechRecognizer? = try {
        if (preferOnDevice &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            usingOnDevice = true
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            usingOnDevice = false
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Could not create recognizer", e)
        null
    }

    fun start(preferOnDevice: Boolean = true) {
        stop()
        val speech = create(preferOnDevice)
        if (speech == null) {
            onState(VoiceState.Failed("Speech recognition is not available on this phone"))
            return
        }
        recognizer = speech
        speech.setRecognitionListener(listener)
        speech.startListening(buildIntent())
        onState(VoiceState.Listening)
    }

    private fun buildIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        // Indian English handles local words and accents markedly better than the default.
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
    }

    fun stop() {
        recognizer?.let {
            runCatching { it.stopListening() }
            runCatching { it.cancel() }
            runCatching { it.destroy() }
        }
        recognizer = null
    }

    /** Must be called from onDispose — a leaked recognizer holds the microphone. */
    fun destroy() = stop()

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "ready for speech")
            onState(VoiceState.Listening)
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            Log.d(TAG, "end of speech")
            onState(VoiceState.Processing)
        }

        override fun onError(error: Int) {
            Log.w(TAG, "recognizer error $error (onDevice=$usingOnDevice)")
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH ->
                    onState(VoiceState.Failed("Didn't catch that — try again"))

                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    onState(VoiceState.Failed("No speech detected"))

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    onState(VoiceState.Failed("Microphone permission is needed to log by voice"))

                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    if (usingOnDevice) {
                        onState(VoiceState.Failed("Offline recognition failed — try again"))
                    } else {
                        // On-device pack may exist even when the network path fails.
                        Log.d(TAG, "network error, retrying on-device")
                        start(preferOnDevice = true)
                    }
                }

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    stop()
                    onState(VoiceState.Failed("Recogniser was busy — tap the mic again"))
                }

                SpeechRecognizer.ERROR_AUDIO ->
                    onState(VoiceState.Failed("Microphone problem — try again"))

                SpeechRecognizer.ERROR_CLIENT ->
                    onState(VoiceState.Failed("Recognition stopped"))

                SpeechRecognizer.ERROR_SERVER ->
                    onState(VoiceState.Failed("Speech service error"))

                else -> onState(VoiceState.Failed("Could not hear that (error $error)"))
            }
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            Log.d(TAG, "final result: '$text'")
            if (text.isBlank()) {
                onState(VoiceState.Failed("Didn't catch that — try again"))
            } else {
                onState(VoiceState.Processing)
                onFinalText(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) onState(VoiceState.Hearing(text))
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private companion object {
        const val TAG = "PaisaVoice"
    }
}
