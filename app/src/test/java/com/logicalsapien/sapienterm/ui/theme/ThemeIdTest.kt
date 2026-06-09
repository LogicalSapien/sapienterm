package com.logicalsapien.sapienterm.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeIdTest {

    @Test
    fun `NEO_TERMINAL is the default`() {
        assertEquals(ThemeId.NEO_TERMINAL, ThemeId.DEFAULT)
    }

    @Test
    fun `fromStorageString maps each id back to its enum`() {
        ThemeId.entries.forEach { id ->
            assertEquals(id, ThemeId.fromStorageString(id.storageString))
        }
    }

    @Test
    fun `fromStorageString falls back to DEFAULT for null or unknown`() {
        assertEquals(ThemeId.DEFAULT, ThemeId.fromStorageString(null))
        assertEquals(ThemeId.DEFAULT, ThemeId.fromStorageString("no_such_theme"))
    }

    @Test
    fun `storage strings are unique`() {
        val strings = ThemeId.entries.map { it.storageString }
        assertEquals(strings.size, strings.toSet().size)
    }

    @Test
    fun `display order is unique`() {
        val orders = ThemeId.entries.map { it.displayOrder }
        assertTrue(orders.size == orders.toSet().size)
        assertEquals((0 until ThemeId.entries.size).toSet(), orders.toSet())
    }
}
