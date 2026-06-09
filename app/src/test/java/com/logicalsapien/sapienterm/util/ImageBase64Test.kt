package com.logicalsapien.sapienterm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageBase64Test {

    @Test
    fun isLikelyBase64Payload_acceptsLongAlphabetString() {
        val s = "A".repeat(250)
        assertTrue(isLikelyBase64Payload(s))
    }

    @Test
    fun isLikelyBase64Payload_rejectsShortString() {
        assertFalse(isLikelyBase64Payload("A".repeat(50)))
    }

    @Test
    fun isLikelyBase64Payload_rejectsNormalSentence() {
        val s = "Hello world. ".repeat(30)
        assertFalse(isLikelyBase64Payload(s))
    }

    @Test
    fun pastePlaceholder_detectsAndStripsImeTokens() {
        val junk = "[Pasted text #1 +1 lines][Pasted text #2 +1 lines]"
        assertTrue(containsPastePlaceholderTokens(junk))
        assertEquals("", stripPastePlaceholderTokens(junk))
        assertFalse(containsPastePlaceholderTokens("hello"))
    }
}
