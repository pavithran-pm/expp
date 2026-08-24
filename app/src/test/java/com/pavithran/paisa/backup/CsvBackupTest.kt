package com.pavithran.paisa.backup

import com.pavithran.paisa.data.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvBackupTest {

    private val rows = listOf(
        Expense(
            id = 1,
            timestamp = 1_700_000_000_000,
            rawText = "250 lunch at saravana bhavan",
            amount = 250.0,
            category = "Food",
            merchant = "Saravana Bhavan"
        ),
        Expense(
            id = 2,
            timestamp = 1_700_086_400_000,
            rawText = "blah",
            amount = 0.0,
            category = "Other",
            merchant = null,
            needsReview = true
        )
    )

    @Test
    fun `round trips through csv`() {
        val restored = CsvBackup.fromCsv(CsvBackup.toCsv(rows))
        assertEquals(2, restored.size)
        assertEquals(250.0, restored[0].amount, 0.001)
        assertEquals("Saravana Bhavan", restored[0].merchant)
        assertEquals("250 lunch at saravana bhavan", restored[0].rawText)
        assertTrue(restored[1].needsReview)
        assertEquals(null, restored[1].merchant)
    }

    @Test
    fun `ids are reset so an import never collides`() {
        val restored = CsvBackup.fromCsv(CsvBackup.toCsv(rows))
        assertTrue(restored.all { it.id == 0L })
    }

    @Test
    fun `commas and quotes inside fields survive`() {
        val tricky = listOf(
            rows[0].copy(
                rawText = "500 at raja stores, t nagar",
                merchant = "Raja \"Super\" Stores, T Nagar"
            )
        )
        val restored = CsvBackup.fromCsv(CsvBackup.toCsv(tricky))
        assertEquals(1, restored.size)
        assertEquals("500 at raja stores, t nagar", restored[0].rawText)
        assertEquals("Raja \"Super\" Stores, T Nagar", restored[0].merchant)
    }

    @Test
    fun `header is optional on import`() {
        val body = CsvBackup.toCsv(rows).lines().drop(1).joinToString("\n")
        assertEquals(2, CsvBackup.fromCsv(body).size)
    }
}
