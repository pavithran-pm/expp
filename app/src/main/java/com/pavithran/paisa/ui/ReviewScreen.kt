package com.pavithran.paisa.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pavithran.paisa.data.Expense

@Composable
fun ReviewScreen(
    items: List<Expense>,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            EmptyState(
                title = "Nothing to review",
                subtitle = "Entries the parser wasn't sure about land here."
            )
        } else {
            Text(
                "${items.size} entr${if (items.size == 1) "y" else "ies"} need a quick check",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            ExpenseList(
                expenses = items,
                onEdit = onEdit,
                onDelete = onDelete,
                showDateHeaders = false
            )
        }
    }
}
