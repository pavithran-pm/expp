package com.pavithran.paisa

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.pavithran.paisa.backup.CsvBackup
import com.pavithran.paisa.data.ExpenseRepository
import com.pavithran.paisa.ui.PaisaApp
import com.pavithran.paisa.ui.theme.PaisaTheme
import com.pavithran.paisa.widget.PaisaWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val repo = ExpenseRepository.from(applicationContext)
                    CsvBackup.export(applicationContext, uri, repo.getAll())
                }
            }
            toast(
                result.fold(
                    onSuccess = { "Exported $it expenses" },
                    onFailure = { "Export failed: ${it.message}" }
                )
            )
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val repo = ExpenseRepository.from(applicationContext)
                    val rows = CsvBackup.import(applicationContext, uri)
                    repo.insertAll(rows)
                    rows.size
                }
            }
            PaisaWidget.refresh(applicationContext)
            toast(
                result.fold(
                    onSuccess = { "Imported $it expenses" },
                    onFailure = { "Import failed: ${it.message}" }
                )
            )
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Reminder is a nicety; nothing to do if it is declined. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            PaisaTheme {
                PaisaApp(
                    onExportRequested = {
                        exportLauncher.launch("paisa-${LocalDate.now()}.csv")
                    },
                    onImportRequested = {
                        importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                    }
                )
            }
        }
    }

    private fun toast(message: String) =
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
}
