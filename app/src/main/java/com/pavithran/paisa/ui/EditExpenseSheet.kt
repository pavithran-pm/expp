package com.pavithran.paisa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pavithran.paisa.data.Dates
import com.pavithran.paisa.data.Expense
import com.pavithran.paisa.parse.ExpenseParser
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseSheet(
    expense: Expense,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf(if (expense.amount > 0) trimZeros(expense.amount) else "") }
    var category by remember { mutableStateOf(expense.category) }
    var merchant by remember { mutableStateOf(expense.merchant.orEmpty()) }
    var timestamp by remember { mutableStateOf(expense.timestamp) }
    var categoryOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Original", style = MaterialTheme.typography.labelMedium)
            Text(
                expense.rawText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryOpen,
                onExpandedChange = { categoryOpen = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = categoryOpen,
                    onDismissRequest = { categoryOpen = false }
                ) {
                    ExpenseParser.categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryOpen = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { datePickerOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date · ${Dates.dayLabel(timestamp)}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text("Delete")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSave(
                            expense.copy(
                                amount = amountText.toDoubleOrNull() ?: 0.0,
                                category = category,
                                merchant = merchant.trim().ifBlank { null },
                                timestamp = timestamp
                            )
                        )
                    }
                ) {
                    Text("Save")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (datePickerOpen) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { picked ->
                        // The picker reports UTC midnight; keep the original clock time.
                        val pickedDate = Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate()
                        val originalTime = Instant.ofEpochMilli(timestamp)
                            .atZone(Dates.zone).toLocalTime()
                        timestamp = pickedDate
                            .atTime(originalTime ?: LocalTime.NOON)
                            .atZone(Dates.zone)
                            .toInstant()
                            .toEpochMilli()
                    }
                    datePickerOpen = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerOpen = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun trimZeros(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
