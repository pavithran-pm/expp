package com.pavithran.paisa.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseParserTest {

    private fun check(
        input: String,
        amount: Double?,
        category: String,
        merchant: String? = null,
        confident: Boolean = true
    ) {
        val result = ExpenseParser.parse(input)
        assertEquals("amount for '$input'", amount, result.amount)
        assertEquals("category for '$input'", category, result.category)
        assertEquals("merchant for '$input'", merchant, result.merchant)
        assertEquals("confidence for '$input'", confident, result.confident)
    }

    // --- plain digits -----------------------------------------------------

    @Test fun `digits and category`() = check("250 lunch", 250.0, "Food")

    @Test fun `full sentence with merchant`() =
        check("spent 250 on lunch at saravana bhavan", 250.0, "Food", "Saravana Bhavan")

    @Test fun `category before amount`() = check("auto 80", 80.0, "Transport")

    @Test fun `decimal amount`() = check("99.50 coffee", 99.5, "Food")

    @Test fun `amount after keyword`() = check("petrol 1200", 1200.0, "Transport")

    @Test fun `comma separated amount`() = check("dmart 1,250", 1250.0, "Groceries", "Dmart")

    @Test fun `rupee symbol is stripped`() = check("₹250 lunch", 250.0, "Food")

    @Test fun `rupees word is stripped`() =
        check("paid 500 rupees for groceries", 500.0, "Groceries")

    @Test fun `rs glued to amount`() = check("rs250 chai", 250.0, "Food")

    // --- shorthand --------------------------------------------------------

    @Test fun `k shorthand with decimal`() = check("1.2k petrol", 1200.0, "Transport")

    @Test fun `k shorthand whole`() = check("2k rent", 2000.0, "Bills")

    @Test fun `k shorthand after merchant`() = check("amazon 1.5k", 1500.0, "Shopping", "Amazon")

    @Test fun `digit with thousand word`() = check("3 thousand rent", 3000.0, "Bills")

    @Test fun `digit with lakh word`() = check("2 lakh rent", 200000.0, "Bills")

    // --- word numbers -----------------------------------------------------

    @Test fun `indian shorthand two fifty`() = check("two fifty lunch", 250.0, "Food")

    @Test fun `indian shorthand one fifty`() = check("one fifty petrol", 150.0, "Transport")

    @Test fun `indian shorthand with trailing unit`() =
        check("two fifty five auto", 255.0, "Transport")

    @Test fun `bare hundred`() = check("hundred rupees tea", 100.0, "Food")

    @Test fun `five hundred`() = check("five hundred shirt", 500.0, "Shopping")

    @Test fun `two hundred fifty`() = check("two hundred fifty biryani", 250.0, "Food")

    @Test fun `tens then unit`() = check("twenty five chai", 25.0, "Food")

    @Test fun `fifteen hundred`() = check("fifteen hundred rent", 1500.0, "Bills")

    @Test fun `two thousand`() = check("two thousand emi", 2000.0, "Bills")

    @Test fun `bare fifty in a sentence`() = check("spent fifty on chai", 50.0, "Food")

    @Test fun `mangled transcription twoifty`() = check("twoifty lunch", 250.0, "Food")

    // --- categories -------------------------------------------------------

    @Test fun `swiggy is food`() = check("swiggy 450", 450.0, "Food", "Swiggy")

    @Test fun `hotel means restaurant in india`() = check("hotel 300", 300.0, "Food")

    @Test fun `recharge is bills`() = check("recharge 299", 299.0, "Bills")

    @Test fun `medicine with merchant`() =
        check("medicine 150 apollo", 150.0, "Health", "Apollo")

    @Test fun `electricity bill`() = check("electricity bill 1450", 1450.0, "Bills")

    @Test fun `milk is groceries`() = check("milk 60", 60.0, "Groceries")

    @Test fun `movie is personal`() = check("movie ticket 400", 400.0, "Personal")

    @Test fun `uber is transport`() = check("uber 240", 240.0, "Transport", "Uber")

    @Test fun `parking is transport`() = check("parking 40", 40.0, "Transport")

    @Test fun `gym is personal`() = check("gym 1500", 1500.0, "Personal")

    @Test fun `case is ignored`() = check("Swiggy 450", 450.0, "Food", "Swiggy")

    @Test fun `merchant survives extra words`() =
        check("tea 20 at raja stores", 20.0, "Food", "Raja Stores")

    @Test fun `vegetables from a market`() =
        check("vegetables 120 from market", 120.0, "Groceries", "Market")

    // --- word boundaries --------------------------------------------------

    @Test fun `auto does not match inside automobile`() =
        check("automobile 500", 500.0, "Other", "Automobile", confident = false)

    // --- confidence rules -------------------------------------------------

    @Test fun `amount without category is not confident`() =
        check("500", 500.0, "Other", null, confident = false)

    @Test fun `no amount at all is not confident`() =
        check("blah blah", null, "Other", "Blah Blah", confident = false)

    @Test fun `empty input`() = check("", null, "Other", null, confident = false)

    @Test fun `whitespace only input`() = check("    ", null, "Other", null, confident = false)

    @Test fun `category without amount is not confident`() {
        val result = ExpenseParser.parse("lunch")
        assertNull(result.amount)
        assertEquals("Food", result.category)
        assertFalse(result.confident)
    }

    // --- housekeeping -----------------------------------------------------

    @Test fun `normalise collapses whitespace and lowercases`() {
        assertEquals("250 lunch", ExpenseParser.normalise("  250   LUNCH  "))
    }

    @Test fun `other is offered as a category`() {
        assertTrue(ExpenseParser.categories.contains(ExpenseParser.OTHER))
        assertTrue(ExpenseParser.categories.contains("Food"))
    }
}
