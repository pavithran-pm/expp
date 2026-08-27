package com.pavithran.paisa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pavithran.paisa.data.Dates
import com.pavithran.paisa.data.Expense
import com.pavithran.paisa.data.Money
import com.pavithran.paisa.ui.components.styleFor
import com.pavithran.paisa.ui.theme.Emerald500
import com.pavithran.paisa.ui.theme.Emerald900
import com.pavithran.paisa.ui.theme.ReviewAmber

private val quickPhrases = listOf("chai 20", "auto 80", "250 lunch", "1.2k petrol", "swiggy 450")

@Composable
fun LogScreen(
    expenses: List<Expense>,
    todayTotal: Double,
    monthTotal: Double,
    onLog: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val todayCount = remember(expenses) {
        val today = Dates.dayLabel(System.currentTimeMillis())
        expenses.count { Dates.dayLabel(it.timestamp) == today }
    }

    fun submit() {
        if (input.isNotBlank()) {
            onLog(input)
            input = ""
            keyboard?.hide()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TodayCard(todayTotal = todayTotal, monthTotal = monthTotal, entries = todayCount)

        InputBar(
            value = input,
            onValueChange = { input = it },
            onSubmit = { submit() }
        )

        if (input.isBlank()) {
            QuickPhrases(onPick = { input = it })
        }

        Spacer(Modifier.height(4.dp))

        if (expenses.isEmpty()) {
            EmptyState()
        } else {
            ExpenseList(expenses = expenses, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun TodayCard(todayTotal: Double, monthTotal: Double, entries: Int) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Emerald900, Emerald500)))
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Column {
            Text(
                text = "SPENT TODAY",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = Money.format(todayTotal),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("This month  ${Money.format(monthTotal)}")
                StatPill(if (entries == 1) "1 entry today" else "$entries entries today")
            }
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputBar(value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            label = { Text("What did you spend on?") },
            placeholder = { Text("250 lunch at Saravana Bhavan") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )
        Spacer(Modifier.width(10.dp))
        FilledIconButton(
            onClick = onSubmit,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Log expense"
            )
        }
    }
}

@Composable
private fun QuickPhrases(onPick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickPhrases.forEach { phrase ->
            AssistChip(
                onClick = { onPick(phrase) },
                label = { Text(phrase) },
                shape = RoundedCornerShape(50),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.EditNote,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("Nothing logged yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Say it or type it — \"two fifty lunch at Saravana Bhavan\" is enough.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExpenseList(
    expenses: List<Expense>,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
    showDateHeaders: Boolean = true
) {
    val grouped = remember(expenses) { expenses.groupBy { Dates.dayLabel(it.timestamp) } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Room for the mic button so the last row can always scroll clear of it.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (day, rows) ->
            if (showDateHeaders) {
                item(key = "header-$day") { DayHeader(day, rows.sumOf { it.amount }) }
            }
            items(rows, key = { it.id }) { expense ->
                SwipeableExpenseCard(expense = expense, onEdit = onEdit, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun DayHeader(label: String, total: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = Money.format(total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableExpenseCard(
    expense: Expense,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete(expense)
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        ExpenseCard(expense = expense, onClick = { onEdit(expense) })
    }
}

@Composable
fun ExpenseCard(expense: Expense, onClick: () -> Unit) {
    val style = styleFor(expense.category)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(style.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.merchant ?: expense.category,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = if (expense.needsReview) {
                        "\u201c${expense.rawText}\u201d"
                    } else {
                        "${expense.category} · ${Dates.timeLabel(expense.timestamp)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (expense.amount > 0) Money.format(expense.amount) else "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (expense.needsReview) {
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ReviewAmber.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Check",
                            style = MaterialTheme.typography.labelMedium,
                            color = ReviewAmber
                        )
                    }
                }
            }
        }
    }
}
