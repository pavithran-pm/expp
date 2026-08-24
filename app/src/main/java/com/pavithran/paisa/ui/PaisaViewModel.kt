package com.pavithran.paisa.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pavithran.paisa.data.CategoryTotal
import com.pavithran.paisa.data.Dates
import com.pavithran.paisa.data.Expense
import com.pavithran.paisa.data.ExpenseRepository
import com.pavithran.paisa.data.Money
import com.pavithran.paisa.voice.VoiceRecognizer
import com.pavithran.paisa.voice.VoiceState
import com.pavithran.paisa.widget.PaisaWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** One-shot message for the snackbar, optionally with an Undo action. */
data class UiMessage(
    val text: String,
    val undo: (() -> Unit)? = null
)

class PaisaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ExpenseRepository.from(app)

    private val messages = Channel<UiMessage>(Channel.BUFFERED)
    val uiMessages = messages.receiveAsFlow()

    val expenses: StateFlow<List<Expense>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reviewItems: StateFlow<List<Expense>> = repo.observeNeedsReview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reviewCount: StateFlow<Int> = repo.observeReviewCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val todayTotal: StateFlow<Double> = Dates.dayRange(LocalDate.now(Dates.zone)).let { range ->
        repo.observeTotalBetween(range.first, range.last)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private val _selectedMonth = MutableStateFlow(YearMonth.now(Dates.zone))
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthTotal: StateFlow<Double> = _selectedMonth
        .flatMapLatest { month ->
            val range = Dates.monthRange(month)
            repo.observeTotalBetween(range.first, range.last)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthCategories: StateFlow<List<CategoryTotal>> = _selectedMonth
        .flatMapLatest { month ->
            val range = Dates.monthRange(month)
            repo.observeCategoryTotals(range.first, range.last)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var recognizer: VoiceRecognizer? = null

    // --- logging ----------------------------------------------------------

    /** Never rejects input: unparsed entries are saved and flagged for review. */
    fun log(rawText: String, timestamp: Long = System.currentTimeMillis()) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            val saved = repo.logRawText(rawText, timestamp)
            refreshWidget()
            messages.send(
                UiMessage(
                    text = describe(saved),
                    undo = { deleteSilently(saved) }
                )
            )
        }
    }

    fun update(expense: Expense) {
        viewModelScope.launch {
            repo.update(expense)
            refreshWidget()
            messages.send(UiMessage("Saved"))
        }
    }

    fun delete(expense: Expense) {
        viewModelScope.launch {
            repo.delete(expense)
            refreshWidget()
            messages.send(
                UiMessage(
                    text = "Deleted ${Money.format(expense.amount)}",
                    undo = {
                        viewModelScope.launch {
                            repo.insert(expense.copy(id = 0))
                            refreshWidget()
                        }
                    }
                )
            )
        }
    }

    private fun deleteSilently(expense: Expense) {
        viewModelScope.launch {
            repo.delete(expense)
            refreshWidget()
        }
    }

    private fun describe(expense: Expense): String = buildString {
        append(if (expense.amount > 0) Money.format(expense.amount) else "No amount")
        append(" · ")
        append(expense.category)
        expense.merchant?.let { append(" · $it") }
        if (expense.needsReview) append(" · needs review")
    }

    fun showMessage(text: String) {
        viewModelScope.launch { messages.send(UiMessage(text)) }
    }

    // --- month selection --------------------------------------------------

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun previousMonth() = selectMonth(_selectedMonth.value.minusMonths(1))

    fun nextMonth() {
        val next = _selectedMonth.value.plusMonths(1)
        if (!next.isAfter(YearMonth.now(Dates.zone))) selectMonth(next)
    }

    // --- voice ------------------------------------------------------------

    fun startListening() {
        val app = getApplication<Application>()
        recognizer?.destroy()
        recognizer = VoiceRecognizer(
            context = app,
            onState = { state -> _voiceState.value = state },
            onFinalText = { text ->
                log(text)
                _voiceState.value = VoiceState.Idle
            }
        ).also { it.start() }
    }

    fun stopListening() {
        recognizer?.destroy()
        recognizer = null
        _voiceState.value = VoiceState.Idle
    }

    fun clearVoiceError() {
        if (_voiceState.value is VoiceState.Failed) _voiceState.value = VoiceState.Idle
    }

    private suspend fun refreshWidget() {
        runCatching { PaisaWidget.refresh(getApplication()) }
    }

    override fun onCleared() {
        recognizer?.destroy()
        recognizer = null
        super.onCleared()
    }
}
