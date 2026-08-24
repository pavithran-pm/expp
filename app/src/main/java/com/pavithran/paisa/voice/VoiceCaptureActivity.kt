package com.pavithran.paisa.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pavithran.paisa.data.ExpenseRepository
import com.pavithran.paisa.data.Money
import com.pavithran.paisa.widget.PaisaWidget
import kotlinx.coroutines.launch

/**
 * Transparent activity launched by the widget: it starts listening immediately,
 * saves, and dismisses itself. Unlock, tap, speak — no main screen in between.
 */
class VoiceCaptureActivity : ComponentActivity() {

    private var recognizer: VoiceRecognizer? = null

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else finishWith("Microphone permission is needed")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startListening() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListening() {
        Toast.makeText(this, "Listening…", Toast.LENGTH_SHORT).show()
        recognizer = VoiceRecognizer(
            context = this,
            onState = { state ->
                if (state is VoiceState.Failed) finishWith(state.message)
            },
            onFinalText = { text -> save(text) }
        ).also { it.start() }
    }

    private fun save(rawText: String) {
        lifecycleScope.launch {
            val saved = ExpenseRepository.from(applicationContext).logRawText(rawText)
            PaisaWidget.refresh(applicationContext)
            val label = if (saved.amount > 0) {
                "${Money.format(saved.amount)} · ${saved.category}"
            } else {
                "Saved for review: $rawText"
            }
            finishWith(label)
        }
    }

    private fun finishWith(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        // A leaked recognizer holds the mic and breaks the next attempt.
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }
}
