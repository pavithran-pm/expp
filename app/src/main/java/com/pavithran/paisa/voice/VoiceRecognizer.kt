package com.pavithran.paisa.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Hearing(val partial: String) : VoiceState
    data object Processing : VoiceState

    /** Falling back to another engine; the notice explains why. */
    data class Retrying(val notice: String) : VoiceState
    data class Failed(val message: String) : VoiceState
}

/**
 * Drives [SpeechRecognizer] through the fallback chain in [VoiceErrorPolicy]:
 * the offline recogniser first, then the bound service, then the system's own
 * dialog. A phone that reports offline recognition as available but has no
 * language pack fails on the first attempt — without the fallback that reaches
 * the user as a bare error code.
 */
class VoiceRecognizer(
    context: Context,
    private val onState: (VoiceState) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onNeedsPermission: () -> Unit = {},
    private val onUseSystemDialog: () -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var engine = VoiceEngine.SYSTEM_SERVICE
    private var attempt = 0
    private var settled = false

    private val watchdog = Runnable {
        Log.w(TAG, "no callback within ${WATCHDOG_MS}ms on $engine")
        handleError(VoiceError.CLIENT)
    }

    fun start() {
        attempt = 0
        settled = false
        startWith(firstEngine())
    }

    /** Offline first when the phone claims to support it; the policy handles the rest. */
    private fun firstEngine(): VoiceEngine = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext) }
                .getOrDefault(false) -> VoiceEngine.ON_DEVICE

        runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }
            .getOrDefault(false) -> VoiceEngine.SYSTEM_SERVICE

        else -> VoiceEngine.SYSTEM_DIALOG
    }

    private fun startWith(next: VoiceEngine) {
        engine = next
        attempt++
        releaseRecognizer()

        if (next == VoiceEngine.SYSTEM_DIALOG) {
            Log.d(TAG, "handing over to the system dialog")
            onUseSystemDialog()
            return
        }

        val speech = createRecognizer(next)
        if (speech == null) {
            handleError(VoiceError.NONE_AVAILABLE)
            return
        }
        recognizer = speech
        speech.setRecognitionListener(listener)
        runCatching { speech.startListening(buildIntent()) }
            .onFailure {
                Log.e(TAG, "startListening threw on $next", it)
                handleError(VoiceError.CLIENT)
                return
            }
        armWatchdog()
        onState(VoiceState.Listening)
    }

    private fun createRecognizer(next: VoiceEngine): SpeechRecognizer? = runCatching {
        if (next == VoiceEngine.ON_DEVICE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
        } else {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }
    }.onFailure { Log.e(TAG, "could not create a $next recogniser", it) }.getOrNull()

    private fun buildIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        // Indian English handles local words and accents markedly better.
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
    }

    private fun armWatchdog() {
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, WATCHDOG_MS)
    }

    private fun cancelWatchdog() = handler.removeCallbacks(watchdog)

    private fun releaseRecognizer() {
        val current = recognizer ?: return
        recognizer = null
        // Never tear down from inside the recogniser's own callback.
        handler.post {
            runCatching { current.cancel() }
            runCatching { current.destroy() }
        }
    }

    private fun handleError(error: Int) {
        cancelWatchdog()
        if (settled) return
        Log.w(TAG, "error $error on $engine (attempt $attempt)")

        when (val recovery = VoiceErrorPolicy.recover(error, engine, attempt)) {
            is VoiceRecovery.NeedsPermission -> {
                settled = true
                releaseRecognizer()
                onState(VoiceState.Failed(VoiceErrorPolicy.messageFor(error, engine)))
                onNeedsPermission()
            }

            is VoiceRecovery.Retry -> {
                recovery.notice?.let { onState(VoiceState.Retrying(it)) }
                releaseRecognizer()
                handler.post { startWith(recovery.engine) }
            }

            is VoiceRecovery.Fail -> {
                settled = true
                releaseRecognizer()
                onState(VoiceState.Failed(recovery.message))
            }
        }
    }

    fun stop() {
        settled = true
        cancelWatchdog()
        recognizer?.let { runCatching { it.stopListening() } }
        releaseRecognizer()
    }

    /** Must be called when the screen goes away — a leaked recogniser holds the mic. */
    fun destroy() = stop()

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            cancelWatchdog()
            Log.d(TAG, "ready for speech on $engine")
            onState(VoiceState.Listening)
        }

        override fun onBeginningOfSpeech() {
            cancelWatchdog()
            Log.d(TAG, "beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            Log.d(TAG, "end of speech")
            onState(VoiceState.Processing)
        }

        override fun onError(error: Int) = handleError(error)

        override fun onResults(results: Bundle?) {
            cancelWatchdog()
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            Log.d(TAG, "final result: '$text'")
            if (text.isBlank()) {
                handleError(VoiceError.NO_MATCH)
                return
            }
            settled = true
            releaseRecognizer()
            onState(VoiceState.Processing)
            onFinalText(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            cancelWatchdog()
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) onState(VoiceState.Hearing(text))
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    companion object {
        private const val TAG = "PaisaVoice"
        private const val WATCHDOG_MS = 12_000L
        const val LANGUAGE = "en-IN"

        /** The intent for the system's own "Speak now" screen. */
        fun systemDialogIntent(): Intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the expense")
            }

        /** Pulls the transcript out of a system dialog result. */
        fun transcriptFrom(data: Intent?): String? = data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
