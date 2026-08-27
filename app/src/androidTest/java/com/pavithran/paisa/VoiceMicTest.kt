package com.pavithran.paisa

import android.Manifest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The microphone path on a device with no usable speech input. What matters
 * here is that a failing recogniser is handled: the app must not crash, must
 * not stay stuck listening, and must still accept typing afterwards.
 */
@RunWith(AndroidJUnit4::class)
class VoiceMicTest {

    @get:Rule(order = 0)
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun tapMic() = rule.onNodeWithContentDescription("Log by voice").performClick()

    @Test
    fun tappingTheMicDoesNotCrash() {
        tapMic()
        rule.waitForIdle()
        Thread.sleep(4_000)
        // Whatever the recogniser did, the screen is still there and usable.
        rule.onNodeWithContentDescription("Log by voice").assertExists()
    }

    @Test
    fun typingStillWorksAfterAFailedMicAttempt() {
        tapMic()
        Thread.sleep(4_000)
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("35 chai after voice")
        rule.onNodeWithContentDescription("Log expense").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("₹35", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun rapidDoubleTapIsSurvivable() {
        tapMic()
        tapMic()
        rule.waitForIdle()
        Thread.sleep(3_000)
        rule.onNodeWithContentDescription("Log by voice").assertExists()
    }
}
