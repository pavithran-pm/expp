package com.pavithran.paisa.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert suspend fun insert(expense: Expense): Long

    @Insert suspend fun insertAll(expenses: List<Expense>)

    @Update suspend fun update(expense: Expense)

    @Delete suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAll(): List<Expense>

    @Query("SELECT * FROM expenses WHERE needsReview = 1 ORDER BY timestamp DESC")
    fun observeNeedsReview(): Flow<List<Expense>>

    @Query("SELECT COUNT(*) FROM expenses WHERE needsReview = 1")
    fun observeReviewCount(): Flow<Int>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun byId(id: Long): Expense?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE timestamp BETWEEN :from AND :to")
    fun observeTotalBetween(from: Long, to: Long): Flow<Double>

    // Aggregation belongs in SQL, not in Kotlin.
    @Query(
        """
        SELECT category, SUM(amount) AS total FROM expenses
        WHERE timestamp BETWEEN :from AND :to
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotal>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE timestamp BETWEEN :from AND :to")
    suspend fun totalBetween(from: Long, to: Long): Double

    @Query("SELECT MIN(timestamp) FROM expenses")
    suspend fun earliestTimestamp(): Long?

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
