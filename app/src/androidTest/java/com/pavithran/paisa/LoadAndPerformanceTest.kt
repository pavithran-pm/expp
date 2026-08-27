package com.pavithran.paisa

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pavithran.paisa.data.Expense
import com.pavithran.paisa.data.PaisaDatabase
import com.pavithran.paisa.parse.ExpenseParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Load and performance characteristics of the storage layer, at a size a
 * personal expense tracker would reach after many years: 10,000 rows is
 * roughly nine years at three expenses a day.
 *
 * Thresholds are deliberately loose — a CI emulator is far slower than a
 * phone — so a failure means a real regression, not a slow runner.
 */
@RunWith(AndroidJUnit4::class)
class LoadAndPerformanceTest {

    private lateinit var db: PaisaDatabase

    private val sentences = listOf(
        "250 lunch at saravana bhavan", "1.2k petrol", "chai 20", "swiggy 450",
        "recharge 299", "two fifty auto", "medicine 150 apollo", "dmart 1250",
        "movie ticket 400", "haircut 200", "qwerty nonsense"
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DB_NAME)
        db = Room.databaseBuilder(context, PaisaDatabase::class.java, DB_NAME).build()
    }

    @After
    fun tearDown() {
        db.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DB_NAME)
    }

    private fun report(metric: String, value: Any) = Log.i(TAG, "$metric=$value")

    private fun rows(count: Int, startAt: Long): List<Expense> {
        val day = 24 * 60 * 60 * 1000L
        return (0 until count).map { i ->
            val raw = sentences[i % sentences.size]
            val parsed = ExpenseParser.parse(raw)
            Expense(
                timestamp = startAt - (i / 3) * day,
                rawText = raw,
                amount = parsed.amount ?: 0.0,
                category = parsed.category,
                merchant = parsed.merchant,
                needsReview = !parsed.confident
            )
        }
    }

    @Test
    fun tenThousandExpensesStayFast() = runBlocking {
        val dao = db.expenseDao()
        val now = System.currentTimeMillis()

        val insertMs = measureTimeMillis {
            rows(LOAD_SIZE, now).chunked(500).forEach { dao.insertAll(it) }
        }
        report("bulk_insert_ms_for_$LOAD_SIZE", insertMs)

        val singleInsertMs = measureTimeMillis {
            repeat(50) { dao.insert(rows(1, now).first()) }
        }
        report("single_insert_ms_for_50", singleInsertMs)

        var listed = 0
        val listMs = measureTimeMillis {
            listed = dao.observeAll().first().size
        }
        report("observe_all_first_emission_ms", listMs)
        assertEquals(LOAD_SIZE + 50, listed)

        val monthStart = now - 30L * 24 * 60 * 60 * 1000
        val aggregateMs = measureTimeMillis {
            dao.observeCategoryTotals(monthStart, now).first()
        }
        report("category_aggregation_ms", aggregateMs)

        val totalMs = measureTimeMillis { dao.totalBetween(monthStart, now) }
        report("month_total_ms", totalMs)

        val reviewMs = measureTimeMillis { dao.observeReviewCount().first() }
        report("review_count_ms", reviewMs)

        // The aggregation runs on every summary recomposition, so it is the one
        // that must stay quick even with years of data behind it.
        assertTrue("category aggregation took ${aggregateMs}ms", aggregateMs < 1_000)
        assertTrue("month total took ${totalMs}ms", totalMs < 1_000)
        assertTrue("review count took ${reviewMs}ms", reviewMs < 1_000)
        assertTrue("listing 10k rows took ${listMs}ms", listMs < 4_000)
    }

    @Test
    fun rapidLoggingDoesNotDegrade() = runBlocking {
        val dao = db.expenseDao()
        val now = System.currentTimeMillis()
        dao.insertAll(rows(2_000, now))

        // 100 saves in a row, as fast as the app could ever issue them.
        val burstMs = measureTimeMillis {
            repeat(100) { i ->
                dao.insert(rows(1, now - i).first())
            }
        }
        report("burst_100_inserts_ms", burstMs)
        assertTrue("100 inserts took ${burstMs}ms", burstMs < 5_000)

        val after = dao.observeAll().first().size
        assertEquals(2_100, after)
    }

    @Test
    fun parsingIsCheapEnoughToRunOnEveryKeystroke() {
        val iterations = 10_000
        val elapsed = measureTimeMillis {
            repeat(iterations) { i -> ExpenseParser.parse(sentences[i % sentences.size]) }
        }
        report("parse_${iterations}_ms", elapsed)
        val perParseMicros = elapsed * 1000.0 / iterations
        report("parse_micros_each", "%.1f".format(perParseMicros))
        // Keyword matching is a map lookup per word, not a regex per keyword,
        // which took roughly 1.9ms per sentence before.
        assertTrue("parsing averaged ${perParseMicros}µs", perParseMicros < 800)
    }

    private companion object {
        const val TAG = "PaisaPerf"
        const val DB_NAME = "load-test.db"
        const val LOAD_SIZE = 10_000
    }
}
