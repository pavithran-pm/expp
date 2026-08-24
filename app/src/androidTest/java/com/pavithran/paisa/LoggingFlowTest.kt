package com.pavithran.paisa

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real device or emulator: types a sentence, and checks the parsed
 * expense actually reaches the list.
 */
@RunWith(AndroidJUnit4::class)
class LoggingFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun log(sentence: String) {
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput(sentence)
        rule.onNodeWithContentDescription("Log expense").performClick()
    }

    private fun waitForText(text: String, timeoutMs: Long = 10_000) {
        rule.waitUntil(timeoutMs) {
            rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun typedSentenceIsParsedAndSaved() {
        log("250 lunch at saravana bhavan")
        waitForText("₹250")
        waitForText("Food · Saravana Bhavan")
    }

    @Test
    fun shorthandAmountIsExpanded() {
        log("1.2k petrol")
        waitForText("₹1,200")
        waitForText("Transport")
    }

    @Test
    fun unparseableInputIsStillSaved() {
        log("qwerty nonsense")
        // Saved rather than rejected: the raw sentence is kept, flagged for review.
        waitForText("qwerty nonsense")
    }
}
