package com.pavithran.paisa.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recovery matrix for every error the recogniser can report, from every
 * engine. Positive cases check that a recoverable failure moves down the
 * fallback chain; negative cases check that an unrecoverable one stops with a
 * message a person can act on, and never loops.
 */
class VoiceErrorPolicyTest {

    private fun recover(error: Int, engine: VoiceEngine, attempt: Int = 1) =
        VoiceErrorPolicy.recover(error, engine, attempt)

    // --- positive: the fallback chain ------------------------------------

    @Test
    fun `missing offline pack falls back to the bound service`() {
        assertEquals(
            VoiceRecovery.Retry(VoiceEngine.SYSTEM_SERVICE),
            recover(VoiceError.LANGUAGE_UNAVAILABLE, VoiceEngine.ON_DEVICE)
        )
    }

    @Test
    fun `unsupported language on the service falls back to the system dialog`() {
        assertEquals(
            VoiceRecovery.Retry(VoiceEngine.SYSTEM_DIALOG),
            recover(VoiceError.LANGUAGE_NOT_SUPPORTED, VoiceEngine.SYSTEM_SERVICE)
        )
    }

    @Test
    fun `cannot check support is treated like a missing pack`() {
        assertEquals(
            VoiceRecovery.Retry(VoiceEngine.SYSTEM_SERVICE),
            recover(VoiceError.CANNOT_CHECK_SUPPORT, VoiceEngine.ON_DEVICE)
        )
    }

    @Test
    fun `no bindable recogniser falls back to the system dialog`() {
        assertEquals(
            VoiceRecovery.Retry(VoiceEngine.SYSTEM_DIALOG),
            recover(VoiceError.NONE_AVAILABLE, VoiceEngine.SYSTEM_SERVICE)
        )
    }

    @Test
    fun `no network on the service retries offline with an explanation`() {
        val recovery = recover(VoiceError.NETWORK, VoiceEngine.SYSTEM_SERVICE)
        assertTrue(recovery is VoiceRecovery.Retry)
        recovery as VoiceRecovery.Retry
        assertEquals(VoiceEngine.ON_DEVICE, recovery.engine)
        assertTrue(recovery.notice!!.contains("offline", ignoreCase = true))
    }

    @Test
    fun `server error retries offline`() {
        assertEquals(
            VoiceRecovery.Retry(VoiceEngine.ON_DEVICE),
            recover(VoiceError.SERVER, VoiceEngine.SYSTEM_SERVICE)
        )
    }

    @Test
    fun `client error walks the whole chain before giving up`() {
        assertEquals(
            VoiceRecovery.Retry(VoiceEngine.SYSTEM_SERVICE),
            recover(VoiceError.CLIENT, VoiceEngine.ON_DEVICE)
        )
        assertEquals(
            VoiceRecovery.Retry(VoiceEngine.SYSTEM_DIALOG),
            recover(VoiceError.CLIENT, VoiceEngine.SYSTEM_SERVICE)
        )
        assertTrue(recover(VoiceError.CLIENT, VoiceEngine.SYSTEM_DIALOG) is VoiceRecovery.Fail)
    }

    @Test
    fun `a busy recogniser retries the same engine`() {
        assertEquals(
            VoiceRecovery.Retry(VoiceEngine.SYSTEM_SERVICE),
            recover(VoiceError.RECOGNIZER_BUSY, VoiceEngine.SYSTEM_SERVICE)
        )
    }

    // --- negative: failures that must stop, with a usable message --------

    @Test
    fun `permission failure asks for permission from any engine or attempt`() {
        VoiceEngine.entries.forEach { engine ->
            assertEquals(
                VoiceRecovery.NeedsPermission,
                recover(VoiceError.INSUFFICIENT_PERMISSIONS, engine, attempt = 9)
            )
        }
    }

    @Test
    fun `nothing said is not retried`() {
        assertEquals(
            VoiceRecovery.Fail("Didn't catch that — try again"),
            recover(VoiceError.NO_MATCH, VoiceEngine.SYSTEM_SERVICE)
        )
        assertEquals(
            VoiceRecovery.Fail("No speech detected"),
            recover(VoiceError.SPEECH_TIMEOUT, VoiceEngine.ON_DEVICE)
        )
    }

    @Test
    fun `offline attempt without network gives up rather than looping`() {
        assertTrue(recover(VoiceError.NETWORK, VoiceEngine.ON_DEVICE) is VoiceRecovery.Fail)
    }

    @Test
    fun `microphone trouble stops immediately`() {
        assertEquals(
            VoiceRecovery.Fail("Microphone problem — try again"),
            recover(VoiceError.AUDIO, VoiceEngine.SYSTEM_SERVICE)
        )
    }

    @Test
    fun `rate limiting stops with an explanation`() {
        val recovery = recover(VoiceError.TOO_MANY_REQUESTS, VoiceEngine.SYSTEM_SERVICE)
        assertTrue(recovery is VoiceRecovery.Fail)
        assertTrue((recovery as VoiceRecovery.Fail).message.contains("try again"))
    }

    @Test
    fun `a phone with no recognition at all is told to type instead`() {
        val recovery = recover(VoiceError.NONE_AVAILABLE, VoiceEngine.SYSTEM_DIALOG)
        assertTrue(recovery is VoiceRecovery.Fail)
        assertTrue((recovery as VoiceRecovery.Fail).message.contains("Type", ignoreCase = true))
    }

    @Test
    fun `the attempt cap stops every retryable error from looping`() {
        val retryable = listOf(
            VoiceError.LANGUAGE_UNAVAILABLE,
            VoiceError.CLIENT,
            VoiceError.NETWORK,
            VoiceError.SERVER,
            VoiceError.RECOGNIZER_BUSY,
            VoiceError.NONE_AVAILABLE
        )
        retryable.forEach { error ->
            VoiceEngine.entries.forEach { engine ->
                val recovery = recover(error, engine, attempt = VoiceErrorPolicy.MAX_ATTEMPTS)
                assertTrue(
                    "error $error from $engine should stop at the cap",
                    recovery is VoiceRecovery.Fail
                )
            }
        }
    }

    // --- messages --------------------------------------------------------

    @Test
    fun `every known error has a message that is not a bare code`() {
        val known = listOf(
            VoiceError.NETWORK_TIMEOUT, VoiceError.NETWORK, VoiceError.AUDIO,
            VoiceError.SERVER, VoiceError.CLIENT, VoiceError.SPEECH_TIMEOUT,
            VoiceError.NO_MATCH, VoiceError.RECOGNIZER_BUSY,
            VoiceError.INSUFFICIENT_PERMISSIONS, VoiceError.TOO_MANY_REQUESTS,
            VoiceError.SERVER_DISCONNECTED, VoiceError.LANGUAGE_NOT_SUPPORTED,
            VoiceError.LANGUAGE_UNAVAILABLE, VoiceError.CANNOT_CHECK_SUPPORT,
            VoiceError.CANNOT_LISTEN_TO_DOWNLOAD_EVENTS, VoiceError.NONE_AVAILABLE
        )
        known.forEach { error ->
            val message = VoiceErrorPolicy.messageFor(error, VoiceEngine.SYSTEM_SERVICE)
            assertTrue("error $error has no message", message.isNotBlank())
            assertFalse("error $error leaks a raw code", message.contains("error $error"))
        }
    }

    @Test
    fun `an unknown code still reports something specific`() {
        val message = VoiceErrorPolicy.messageFor(99, VoiceEngine.SYSTEM_SERVICE)
        assertTrue(message.contains("99"))
    }
}
