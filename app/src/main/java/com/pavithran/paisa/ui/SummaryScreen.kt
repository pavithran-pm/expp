package com.pavithran.paisa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pavithran.paisa.data.Dates
import com.pavithran.paisa.data.Money
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SummaryScreen(viewModel: PaisaViewModel) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val total by viewModel.monthTotal.collectAsStateWithLifecycle()
    val categories by viewModel.monthCategories.collectAsStateWithLifecycle()

    val today = LocalDate.now(Dates.zone)
    val daysElapsed = if (month == YearMonth.now(Dates.zone)) {
        today.dayOfMonth
    } else {
        month.lengthOfMonth()
    }
    val dailyAverage = if (daysElapsed > 0) total / daysElapsed else 0.0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(Dates.monthLabel(month), style = MaterialTheme.typography.titleMedium)
            IconButton(
                onClick = viewModel::nextMonth,
                enabled = month.isBefore(YearMonth.now(Dates.zone))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Total spent", style = MaterialTheme.typography.labelLarge)
        Text(
            Money.format(total),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Daily average ${Money.format(dailyAverage)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        if (categories.isEmpty()) {
            EmptyState(
                title = "No spending this month",
                subtitle = "Log something and it will show up here."
            )
        } else {
            val max = categories.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categories.forEach { row ->
                    val share = if (total > 0) row.total / total * 100 else 0.0
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(row.category, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${Money.format(row.total)}  ·  ${"%.0f".format(share)}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        // A proportional Box beats pulling in a charting library.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((row.total / max).toFloat().coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}
