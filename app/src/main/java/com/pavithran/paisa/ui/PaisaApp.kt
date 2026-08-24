package com.pavithran.paisa.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pavithran.paisa.R
import com.pavithran.paisa.data.Expense
import com.pavithran.paisa.voice.VoiceState

private enum class Tab { Log, Summary, Review }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaisaApp(
    viewModel: PaisaViewModel = viewModel(),
    onExportRequested: () -> Unit = {},
    onImportRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(Tab.Log) }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val reviewItems by viewModel.reviewItems.collectAsStateWithLifecycle()
    val reviewCount by viewModel.reviewCount.collectAsStateWithLifecycle()
    val todayTotal by viewModel.todayTotal.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

    var pendingVoiceStart by remember { mutableStateOf(false) }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingVoiceStart) {
            viewModel.startListening()
        } else if (!granted) {
            viewModel.showMessage("Microphone access is needed to log by voice")
        }
        pendingVoiceStart = false
    }

    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { message ->
            val result = snackbarHost.showSnackbar(
                message = message.text,
                actionLabel = if (message.undo != null) "Undo" else null,
                withDismissAction = message.undo == null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) message.undo?.invoke()
        }
    }

    LaunchedEffect(voiceState) {
        (voiceState as? VoiceState.Failed)?.let {
            snackbarHost.showSnackbar(it.message)
            viewModel.clearVoiceError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Export to CSV") },
                            onClick = { menuOpen = false; onExportRequested() }
                        )
                        DropdownMenuItem(
                            text = { Text("Import from CSV") },
                            onClick = { menuOpen = false; onImportRequested() }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Log,
                    onClick = { tab = Tab.Log },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    label = { Text("Log") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Summary,
                    onClick = { tab = Tab.Summary },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("Summary") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Review,
                    onClick = { tab = Tab.Review },
                    icon = {
                        BadgedBox(badge = {
                            if (reviewCount > 0) Badge { Text("$reviewCount") }
                        }) {
                            Icon(Icons.Default.FactCheck, contentDescription = null)
                        }
                    },
                    label = { Text("Review") }
                )
            }
        },
        floatingActionButton = {
            val listening = voiceState is VoiceState.Listening ||
                voiceState is VoiceState.Hearing ||
                voiceState is VoiceState.Processing
            val transition = rememberInfiniteTransition(label = "mic")
            val pulse by transition.animateFloat(
                initialValue = 1f,
                targetValue = if (listening) 1.15f else 1f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                label = "pulse"
            )
            FloatingActionButton(
                modifier = Modifier.size(72.dp).scale(pulse),
                containerColor = if (listening) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                onClick = {
                    if (listening) {
                        viewModel.stopListening()
                    } else {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            viewModel.startListening()
                        } else {
                            pendingVoiceStart = true
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (listening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (listening) "Stop listening" else "Log by voice",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                VoiceStatusBar(voiceState)
                when (tab) {
                    Tab.Log -> LogScreen(
                        expenses = expenses,
                        todayTotal = todayTotal,
                        onLog = viewModel::log,
                        onEdit = { editing = it },
                        onDelete = viewModel::delete
                    )

                    Tab.Summary -> SummaryScreen(viewModel)

                    Tab.Review -> ReviewScreen(
                        items = reviewItems,
                        onEdit = { editing = it },
                        onDelete = viewModel::delete
                    )
                }
            }
        }
    }

    editing?.let { expense ->
        EditExpenseSheet(
            expense = expense,
            onDismiss = { editing = null },
            onSave = {
                viewModel.update(it)
                editing = null
            },
            onDelete = {
                viewModel.delete(expense)
                editing = null
            }
        )
    }
}
