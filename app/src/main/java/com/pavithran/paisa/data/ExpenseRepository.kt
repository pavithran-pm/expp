package com.pavithran.paisa.data

import android.content.Context
import com.pavithran.paisa.parse.ExpenseParser
import kotlinx.coroutines.flow.Flow

/**
 * The single place that turns text into a stored expense. Voice and typing both
 * come through here, so there is never a second parsing path.
 */
class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeAll(): Flow<List<Expense>> = dao.observeAll()

    fun observeNeedsReview(): Flow<List<Expense>> = dao.observeNeedsReview()

    fun observeReviewCount(): Flow<Int> = dao.observeReviewCount()

    fun observeTotalBetween(from: Long, to: Long): Flow<Double> = dao.observeTotalBetween(from, to)

    fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotal>> =
        dao.observeCategoryTotals(from, to)

    suspend fun totalBetween(from: Long, to: Long): Double = dao.totalBetween(from, to)

    suspend fun earliestTimestamp(): Long? = dao.earliestTimestamp()

    suspend fun getAll(): List<Expense> = dao.getAll()

    /**
     * Always saves. An entry the parser did not understand is stored with
     * needsReview = true rather than rejected.
     */
    suspend fun logRawText(rawText: String, timestamp: Long = System.currentTimeMillis()): Expense {
        val parsed = ExpenseParser.parse(rawText)
        val expense = Expense(
            timestamp = timestamp,
            rawText = rawText.trim(),
            amount = parsed.amount ?: 0.0,
            category = parsed.category,
            merchant = parsed.merchant,
            needsReview = !parsed.confident
        )
        val id = dao.insert(expense)
        return expense.copy(id = id)
    }

    suspend fun insert(expense: Expense): Long = dao.insert(expense)

    suspend fun insertAll(expenses: List<Expense>) = dao.insertAll(expenses)

    /** Editing an entry is the user confirming it, so the review flag clears. */
    suspend fun update(expense: Expense) = dao.update(expense.copy(needsReview = false))

    suspend fun delete(expense: Expense) = dao.delete(expense)

    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        fun from(context: Context): ExpenseRepository =
            ExpenseRepository(PaisaDatabase.get(context).expenseDao())
    }
}
