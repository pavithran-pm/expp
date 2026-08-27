package com.pavithran.paisa

import android.Manifest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The microphone path on a device with no usable speech input. What matters
 * here is that a failing recogniser is handled: no crash, no stuck listening
 * state, and typing still works afterwards.
 */
@RunWith(AndroidJUnit4::class)
class VoiceMicTest {

    @get:Rule(order = 0)
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** The button's description flips to "Stop listening" once it is listening. */
    private fun tapMic() {
        val idle = rule.onAllNodesWithContentDescription("Log by voice").fetchSemanticsNodes()
        if (idle.isNotEmpty()) {
            rule.onNodeWithContentDescription("Log by voice").performClick()
        } else {
            rule.onNodeWithContentDescription("Stop listening").performClick()
        }
    }

    /** The fallback chain can hand over to the system's speech screen. */
    private fun returnToApp() {
        repeat(2) {
            if (device.currentPackageName != "com.pavithran.paisa") {
                device.pressBack()
                Thread.sleep(1_000)
            }
        }
        rule.waitForIdle()
    }

    private fun appIsAlive() =
        rule.onAllNodesWithText("SPENT TODAY", substring = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun tappingTheMicDoesNotCrash() {
        tapMic()
        Thread.sleep(5_000)
        returnToApp()
        assert(appIsAlive()) { "the log screen disappeared after tapping the mic" }
    }

    @Test
    fun typingStillWorksAfterAFailedMicAttempt() {
        tapMic()
        Thread.sleep(5_000)
        returnToApp()

        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("35 chai after voice")
        rule.onNodeWithContentDescription("Log expense").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("₹35", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun rapidDoubleTapIsSurvivable() {
        tapMic()
        tapMic()
        Thread.sleep(4_000)
        returnToApp()
        assert(appIsAlive()) { "the log screen disappeared after a double tap" }
    }
}
