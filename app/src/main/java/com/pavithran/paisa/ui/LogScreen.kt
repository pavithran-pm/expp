package com.pavithran.paisa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pavithran.paisa.data.Dates
import com.pavithran.paisa.data.Expense
import com.pavithran.paisa.data.Money
import com.pavithran.paisa.ui.theme.ReviewAmber

@Composable
fun LogScreen(
    expenses: List<Expense>,
    todayTotal: Double,
    onLog: (String) -> Unit,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        if (input.isNotBlank()) {
            onLog(input)
            input = ""
            keyboard?.hide()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("What did you spend on?") },
                placeholder = { Text("250 lunch at Saravana Bhavan") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { submit() }) {
                Icon(Icons.Default.Add, contentDescription = "Log expense")
            }
        }

        Text(
            text = "Today · ${Money.format(todayTotal)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        HorizontalDivider()

        if (expenses.isEmpty()) {
            EmptyState(
                title = "Nothing logged yet",
                subtitle = "Type a sentence above, or tap the mic and say it."
            )
        } else {
            ExpenseList(expenses = expenses, onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
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
        // Leave room for the mic FAB so the last row is never hidden behind it.
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        grouped.forEach { (day, rows) ->
            if (showDateHeaders) {
                item(key = "header-$day") { DateHeader(day, rows.sumOf { it.amount }) }
            }
            items(rows, key = { it.id }) { expense ->
                SwipeableExpenseRow(
                    expense = expense,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DateHeader(label: String, total: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(Money.format(total), style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableExpenseRow(
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
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        ExpenseRow(expense = expense, onClick = { onEdit(expense) })
    }
}

@Composable
fun ExpenseRow(expense: Expense, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Amber dot marks an entry the parser was unsure about.
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (expense.needsReview) ReviewAmber else MaterialTheme.colorScheme.surface)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildString {
                    append(expense.category)
                    expense.merchant?.let { append(" · $it") }
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${Dates.timeLabel(expense.timestamp)} · ${expense.rawText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (expense.amount > 0) Money.format(expense.amount) else "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SectionSpacer() = Spacer(Modifier.height(12.dp))
