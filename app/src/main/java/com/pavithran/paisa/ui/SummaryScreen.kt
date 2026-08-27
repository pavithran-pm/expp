package com.pavithran.paisa.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pavithran.paisa.data.CategoryTotal
import com.pavithran.paisa.data.Dates
import com.pavithran.paisa.data.Money
import com.pavithran.paisa.ui.components.styleFor
import com.pavithran.paisa.ui.theme.Emerald500
import com.pavithran.paisa.ui.theme.Emerald900
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SummaryScreen(viewModel: PaisaViewModel) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val total by viewModel.monthTotal.collectAsStateWithLifecycle()
    val allCategories by viewModel.monthCategories.collectAsStateWithLifecycle()
    // A row flagged for review has no amount yet; an empty bar is just noise.
    val categories = allCategories.filter { it.total > 0 }

    val today = LocalDate.now(Dates.zone)
    val daysElapsed = if (month == YearMonth.now(Dates.zone)) {
        today.dayOfMonth
    } else {
        month.lengthOfMonth()
    }
    val dailyAverage = if (daysElapsed > 0) total / daysElapsed else 0.0
    val top = categories.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MonthSelector(
                label = Dates.monthLabel(month),
                canGoForward = month.isBefore(YearMonth.now(Dates.zone)),
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
        }

        item {
            TotalCard(
                total = total,
                dailyAverage = dailyAverage,
                topCategory = top?.category,
                topShare = top?.let { if (total > 0) it.total / total * 100 else 0.0 }
            )
        }

        if (categories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nothing spent this month", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Log something and the breakdown appears here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "BY CATEGORY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(categories, key = { it.category }) { row ->
                CategoryRow(row = row, total = total, largest = categories.first().total)
            }
        }
    }
}

@Composable
private fun MonthSelector(
    label: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month"
                )
            }
            Text(label, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onNext, enabled = canGoForward) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month"
                )
            }
        }
    }
}

@Composable
private fun TotalCard(
    total: Double,
    dailyAverage: Double,
    topCategory: String?,
    topShare: Double?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Emerald900, Emerald500)))
            .padding(22.dp)
    ) {
        Column {
            Text(
                "TOTAL SPENT",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                Money.format(total),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Daily average", Money.format(dailyAverage))
                if (topCategory != null && topShare != null) {
                    MiniStat("Biggest", "$topCategory · ${"%.0f".format(topShare)}%")
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
private fun CategoryRow(row: CategoryTotal, total: Double, largest: Double) {
    val style = styleFor(row.category)
    val share = if (total > 0) row.total / total else 0.0
    val fraction by animateFloatAsState(
        targetValue = if (largest > 0) (row.total / largest).toFloat() else 0f,
        label = "bar"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(style.color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = row.category,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Money.format(row.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${"%.0f".format(share * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(style.color)
                )
            }
        }
    }
}
