package com.pavithran.paisa.parse

/**
 * Result of parsing one spoken or typed sentence.
 *
 * [confident] is false whenever the app should still save the entry but flag it
 * for review: no amount found, or an amount with no recognisable category.
 */
data class ParseResult(
    val amount: Double?,
    val category: String,
    val merchant: String?,
    val confident: Boolean
)

/**
 * Turns "spent 250 on lunch at saravana bhavan" into structured data.
 *
 * Deliberately free of any Android import so it runs as a plain JVM unit test
 * in under a second.
 */
object ExpenseParser {

    const val OTHER = "Other"

    val categoryKeywords: Map<String, List<String>> = mapOf(
        "Food" to listOf(
            "lunch", "dinner", "breakfast", "tea", "chai", "coffee", "snack", "snacks",
            "hotel", "mess", "restaurant", "swiggy", "zomato", "biryani", "meals",
            "tiffin", "idli", "dosa", "parotta", "juice", "bakery", "cake", "food",
            "canteen", "sappadu", "kadai"
        ),
        "Transport" to listOf(
            "petrol", "diesel", "fuel", "auto", "uber", "ola", "rapido", "bus",
            "train", "cab", "taxi", "parking", "toll", "metro", "share auto",
            "ticket fare", "fare"
        ),
        "Groceries" to listOf(
            "groceries", "grocery", "vegetables", "vegetable", "milk", "provision",
            "provisions", "supermarket", "bigbasket", "dmart", "kirana", "rice",
            "eggs", "fruits"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "clothes", "shirt", "pant", "shoes", "myntra",
            "meesho", "dress", "saree"
        ),
        "Health" to listOf(
            "medicine", "medicines", "pharmacy", "doctor", "hospital", "apollo",
            "tablets", "tablet", "clinic", "scan", "lab", "test"
        ),
        "Bills" to listOf(
            "recharge", "bill", "bills", "electricity", "water", "rent", "emi",
            "internet", "wifi", "gas", "dth", "cylinder", "maintenance", "fees",
            "fee", "subscription"
        ),
        "Personal" to listOf(
            "haircut", "salon", "gym", "movie", "cinema", "ticket", "barber",
            "grooming", "books", "gift"
        )
    )

    /** All categories the UI offers, in a stable order. */
    val categories: List<String> = categoryKeywords.keys.toList() + OTHER

    /**
     * Keywords that are also real merchant names. These stay in the merchant
     * field instead of being consumed as category signals.
     */
    private val brandKeywords = setOf(
        "swiggy", "zomato", "amazon", "flipkart", "myntra", "meesho", "apollo",
        "dmart", "bigbasket", "uber", "ola", "rapido"
    )

    private val currencyNoise = setOf(
        "rupees", "rupee", "rs", "rs.", "inr", "₹", "bucks", "buck"
    )

    private val fillerWords = setOf(
        "spent", "spend", "spend it", "paid", "pay", "paying", "gave", "give",
        "on", "at", "for", "to", "in", "from", "of", "the", "a", "an", "and",
        "i", "my", "me", "was", "is", "it", "today", "yesterday", "morning",
        "evening", "night", "just", "some", "there", "here", "got", "took",
        "bought", "buy", "cost", "costs", "worth", "only", "about", "around"
    )

    private val units = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
        "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
        "nineteen" to 19
    )

    private val tens = mapOf(
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fourty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
    )

    private val scales = mapOf(
        "hundred" to 100, "hundreds" to 100,
        "thousand" to 1000, "thousands" to 1000, "k" to 1000,
        "lakh" to 100_000, "lakhs" to 100_000, "lac" to 100_000
    )

    /** Common speech-to-text manglings, mapped to what was actually said. */
    private val transcriptionAliases = mapOf(
        "twoifty" to "two fifty",
        "tooifty" to "two fifty",
        "onefifty" to "one fifty",
        "twofifty" to "two fifty",
        "threefifty" to "three fifty",
        "rs." to "rs",
        "/-" to " "
    )

    fun parse(input: String): ParseResult {
        val normalised = normalise(input)
        if (normalised.isBlank()) {
            return ParseResult(null, OTHER, null, confident = false)
        }

        val amountMatch = extractAmount(normalised)
        // Remove the amount from the string first, so "2k rent" never treats
        // "2k" as a merchant name.
        val withoutAmount = amountMatch?.let {
            normalised.removeRange(it.range).replace(Regex("\\s+"), " ").trim()
        } ?: normalised

        val categoryMatch = matchCategory(withoutAmount)
        val category = categoryMatch?.category ?: OTHER
        val merchant = extractMerchant(withoutAmount, categoryMatch?.category)

        val amount = amountMatch?.value
        val confident = amount != null && categoryMatch != null

        return ParseResult(
            amount = amount,
            category = category,
            merchant = merchant,
            confident = confident
        )
    }

    /** lowercase, expand known manglings, drop currency symbols, collapse spaces. */
    fun normalise(input: String): String {
        var text = input.lowercase().trim()
        text = text.replace("₹", " rs ")
        transcriptionAliases.forEach { (wrong, right) ->
            text = text.replace(wrong, right)
        }
        // "1,200" -> "1200"; keep decimals intact.
        text = text.replace(Regex("(?<=\\d),(?=\\d)"), "")
        text = text.replace(Regex("[^a-z0-9. ]"), " ")
        // "rs250" -> "rs 250", "2k" -> "2 k": speech-to-text glues these together.
        text = text.replace(Regex("(?<=[a-z])(?=\\d)"), " ")
        text = text.replace(Regex("(?<=\\d)(?=[a-z])"), " ")
        return text.replace(Regex("\\s+"), " ").trim()
    }

    private data class AmountMatch(val value: Double, val range: IntRange)

    private data class CategoryMatch(val category: String, val keyword: String)

    private fun extractAmount(text: String): AmountMatch? =
        extractDigitAmount(text) ?: extractWordAmount(text)

    /** 250, 99.50, 1.2k, 2k, "1200 rupees", "3 thousand". */
    private fun extractDigitAmount(text: String): AmountMatch? {
        val regex = Regex("(?<![a-z0-9.])(\\d+(?:\\.\\d+)?)\\s*(k|thousand|thousands|lakh|lakhs|lac|hundred|hundreds)?(?![a-z0-9])")
        val match = regex.find(text) ?: return null
        val base = match.groupValues[1].toDoubleOrNull() ?: return null
        val multiplier = match.groupValues[2].takeIf { it.isNotEmpty() }?.let { scales[it] } ?: 1
        return AmountMatch(base * multiplier, match.range)
    }

    /**
     * "two fifty" -> 250, "hundred" -> 100, "five hundred" -> 500,
     * "twenty five" -> 25, "fifteen hundred" -> 1500, "two thousand" -> 2000.
     */
    private fun extractWordAmount(text: String): AmountMatch? {
        val tokens = text.split(" ")
        var i = 0
        var startToken = -1
        var endToken = -1
        while (i < tokens.size) {
            if (isNumberWord(tokens[i])) {
                startToken = i
                var j = i
                while (j < tokens.size && isNumberWord(tokens[j])) j++
                endToken = j - 1
                break
            }
            i++
        }
        if (startToken < 0) return null

        val run = tokens.subList(startToken, endToken + 1)
        val value = wordsToNumber(run) ?: return null

        // Map the token run back onto character offsets in the original string.
        val startChar = tokens.take(startToken).sumOf { it.length + 1 }
        val endChar = startChar + run.joinToString(" ").length - 1
        return AmountMatch(value.toDouble(), startChar..endChar)
    }

    private fun isNumberWord(word: String) =
        units.containsKey(word) || tens.containsKey(word) || scales.containsKey(word)

    private fun wordsToNumber(words: List<String>): Int? {
        var total = 0
        var current = 0
        var seenAny = false
        var i = 0
        while (i < words.size) {
            val word = words[i]
            val unit = units[word]
            val ten = tens[word]
            val scale = scales[word]
            when {
                unit != null -> {
                    seenAny = true
                    val nextTen = words.getOrNull(i + 1)?.let { tens[it] }
                    // Indian shorthand: "two fifty" means 250, not 52.
                    if (unit in 1..9 && nextTen != null) {
                        current += unit * 100 + nextTen
                        i += 2
                        val trailing = words.getOrNull(i)?.let { units[it] }
                        if (trailing != null && trailing < 10) {
                            current += trailing
                            i++
                        }
                        continue
                    }
                    current += unit
                }
                ten != null -> {
                    seenAny = true
                    current += ten
                    val trailing = words.getOrNull(i + 1)?.let { units[it] }
                    if (trailing != null && trailing in 1..9) {
                        current += trailing
                        i++
                    }
                }
                scale != null -> {
                    seenAny = true
                    if (scale == 100) {
                        current = if (current == 0) 100 else current * 100
                    } else {
                        total += (if (current == 0) 1 else current) * scale
                        current = 0
                    }
                }
            }
            i++
        }
        if (!seenAny) return null
        val value = total + current
        return if (value == 0) null else value
    }

    /** Matches on word boundaries so "auto" never fires inside "automobile". */
    private fun matchCategory(text: String): CategoryMatch? {
        var best: CategoryMatch? = null
        var bestIndex = Int.MAX_VALUE
        categoryKeywords.forEach { (category, keywords) ->
            keywords.forEach { keyword ->
                val index = Regex("\\b${Regex.escape(keyword)}\\b").find(text)?.range?.first
                if (index != null && index < bestIndex) {
                    bestIndex = index
                    best = CategoryMatch(category, keyword)
                }
            }
        }
        return best
    }

    private fun extractMerchant(text: String, matchedCategory: String?): String? {
        var remaining = text
        // Strip every keyword of the matched category, not just the first one,
        // so "movie ticket 400" does not leave "Ticket" behind as a merchant.
        categoryKeywords[matchedCategory].orEmpty()
            .filterNot { it in brandKeywords }
            .forEach { keyword ->
                remaining = remaining.replace(Regex("\\b${Regex.escape(keyword)}\\b"), " ")
            }
        val words = remaining.split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it in fillerWords }
            .filterNot { it in currencyNoise }
            .filterNot { isNumberWord(it) }
            .filterNot { it.all { ch -> ch.isDigit() || ch == '.' } }
        if (words.isEmpty()) return null
        return words.joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
}
