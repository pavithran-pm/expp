package com.pavithran.paisa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch millis. Formatting is a display concern, never a storage one. */
    val timestamp: Long,
    /** Exactly what was said or typed. The safety net — never optional. */
    val rawText: String,
    val amount: Double,
    val category: String,
    val merchant: String? = null,
    val needsReview: Boolean = false
)

/** Row shape returned by the category aggregation query. */
data class CategoryTotal(
    val category: String,
    val total: Double
)
