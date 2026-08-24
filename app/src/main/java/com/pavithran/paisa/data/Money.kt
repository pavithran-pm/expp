package com.pavithran.paisa.data

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Indian digit grouping: 1,25,000 rather than 125,000. */
object Money {
    private val format: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    private val withPaise: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    fun format(amount: Double): String =
        if (amount % 1.0 == 0.0) format.format(amount) else withPaise.format(amount)
}

object Dates {
    val zone: ZoneId = ZoneId.systemDefault()

    private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    fun toLocalDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    fun dayLabel(timestamp: Long, today: LocalDate = LocalDate.now(zone)): String {
        val date = toLocalDate(timestamp)
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(dayFormat)
        }
    }

    fun timeLabel(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(timeFormat)

    fun monthLabel(month: YearMonth): String = month.atDay(1).format(monthFormat)

    fun dayRange(date: LocalDate): LongRange {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start..end
    }

    fun monthRange(month: YearMonth): LongRange {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start..end
    }
}
