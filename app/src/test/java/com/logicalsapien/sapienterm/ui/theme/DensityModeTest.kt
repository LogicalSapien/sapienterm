package com.logicalsapien.sapienterm.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class DensityModeTest {

    @Test
    fun `DEFAULT is COMFORTABLE`() {
        assertEquals(DensityMode.COMFORTABLE, DensityMode.DEFAULT)
    }

    @Test
    fun `fromStorageString roundtrips every mode`() {
        DensityMode.entries.forEach { m ->
            assertEquals(m, DensityMode.fromStorageString(m.storageString))
        }
    }

    @Test
    fun `fromStorageString falls back to DEFAULT for null or unknown`() {
        assertEquals(DensityMode.DEFAULT, DensityMode.fromStorageString(null))
        assertEquals(DensityMode.DEFAULT, DensityMode.fromStorageString("bogus"))
    }
}
