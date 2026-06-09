package com.logicalsapien.sapienterm.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class HostCategoryColorTest {

    @Test
    fun `fromStorageString maps every canonical storage string to its enum value`() {
        assertEquals(HostCategoryColor.GRAY, HostCategoryColor.fromStorageString("gray"))
        assertEquals(HostCategoryColor.RED, HostCategoryColor.fromStorageString("red"))
        assertEquals(HostCategoryColor.ORANGE, HostCategoryColor.fromStorageString("orange"))
        assertEquals(HostCategoryColor.YELLOW, HostCategoryColor.fromStorageString("yellow"))
        assertEquals(HostCategoryColor.GREEN, HostCategoryColor.fromStorageString("green"))
        assertEquals(HostCategoryColor.BLUE, HostCategoryColor.fromStorageString("blue"))
        assertEquals(HostCategoryColor.PURPLE, HostCategoryColor.fromStorageString("purple"))
        assertEquals(HostCategoryColor.VIOLET, HostCategoryColor.fromStorageString("violet"))
    }

    @Test
    fun `fromStorageString returns GRAY for null or unknown`() {
        assertEquals(HostCategoryColor.GRAY, HostCategoryColor.fromStorageString(null))
        assertEquals(HostCategoryColor.GRAY, HostCategoryColor.fromStorageString("bogus"))
    }

    @Test
    fun `storageString roundtrips through fromStorageString`() {
        HostCategoryColor.entries.forEach { c ->
            assertEquals(c, HostCategoryColor.fromStorageString(c.storageString))
        }
    }

    @Test
    fun `every category has a defined display order`() {
        val orders = HostCategoryColor.entries.map { it.displayOrder }
        assertEquals(orders.size, orders.toSet().size) // no duplicates
        assertEquals((0 until HostCategoryColor.entries.size).toSet(), orders.toSet()) // contiguous 0..n-1
    }
}
