package com.pavithran.paisa.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Expense::class], version = 1, exportSchema = true)
abstract class PaisaDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var instance: PaisaDatabase? = null

        fun get(context: Context): PaisaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PaisaDatabase::class.java,
                    "paisa.db"
                )
                    // No fallbackToDestructiveMigration: this app holds real data.
                    // Every schema change from here on ships a Migration.
                    .build()
                    .also { instance = it }
            }
    }
}
