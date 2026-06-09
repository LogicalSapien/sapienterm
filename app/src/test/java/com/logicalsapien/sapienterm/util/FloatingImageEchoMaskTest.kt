package com.logicalsapien.sapienterm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingImageEchoMaskTest {

    private val payload = "A".repeat(500)

    @Test
    fun lineEcho_matchesFullLine() {
        assertTrue(lineEchoMatchesFloatingImagePayload(payload, payload))
    }

    @Test
    fun lineEcho_matchesWrappedRowSubstring() {
        val row = payload.substring(100, 180)
        assertTrue(lineEchoMatchesFloatingImagePayload(row, payload))
    }

    @Test
    fun lineEcho_matchesShortLastRowWithEndsWith() {
        val tail = payload.takeLast(12)
        assertTrue(lineEchoMatchesFloatingImagePayload(tail, payload))
    }

    @Test
    fun lineEcho_matchesPromptPrefixPlusBase64Chunk() {
        val chunk = payload.substring(0, 64)
        val line = "user@host:~$ $chunk"
        assertTrue(lineEchoMatchesFloatingImagePayload(line, payload))
    }

    @Test
    fun lineEcho_rejectsUnrelatedLine() {
        assertFalse(lineEchoMatchesFloatingImagePayload("just some normal shell output here", payload))
    }

    @Test
    fun base64SuffixFromLine_stripsPrompt() {
        assertEquals("XYZ123", base64SuffixFromLine("prompt$ XYZ123"))
    }

    @Test
    fun base64SuffixFromLine_pureBase64() {
        assertEquals("AB+/=", base64SuffixFromLine("AB+/="))
    }
}
