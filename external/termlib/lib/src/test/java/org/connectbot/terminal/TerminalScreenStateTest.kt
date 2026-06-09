/*
 * ConnectBot Terminal
 * Copyright 2025 Kenny Root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.connectbot.terminal

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalScreenStateTest {

    private val fg = Color.White
    private val bg = Color.Black

    @Test
    fun updateSnapshot_clampsScrollbackPositionWhenBufferShrinks() {
        val scroll5 = List(5) { r -> TerminalLine.empty(r, 10, fg, bg) }
        val snap = TerminalSnapshot.empty(2, 10, fg, bg).copy(scrollback = scroll5)
        val state = TerminalScreenState(snap)
        state.scrollToTop()
        assertEquals(5, state.scrollbackPosition)

        val shrunk = snap.copy(scrollback = scroll5.take(2))
        state.updateSnapshot(shrunk)
        assertEquals(2, state.scrollbackPosition)
    }

    @Test
    fun updateSnapshot_preservesScrollWhenBufferStillFits() {
        val scroll5 = List(5) { r -> TerminalLine.empty(r, 10, fg, bg) }
        val snap = TerminalSnapshot.empty(2, 10, fg, bg).copy(scrollback = scroll5)
        val state = TerminalScreenState(snap)
        state.scrollBy(2)
        assertEquals(2, state.scrollbackPosition)

        state.updateSnapshot(snap.copy())
        assertEquals(2, state.scrollbackPosition)
    }

    @Test
    fun scrollBy_coercesToValidRange() {
        val snap = TerminalSnapshot.empty(2, 10, fg, bg)
        val state = TerminalScreenState(snap)
        state.scrollBy(100)
        assertEquals(0, state.scrollbackPosition)
        assertTrue(state.isAtBottom())
    }

    @Test
    fun scrollToBottomAndTop_matchBounds() {
        val scroll3 = List(3) { r -> TerminalLine.empty(r, 10, fg, bg) }
        val snap = TerminalSnapshot.empty(2, 10, fg, bg).copy(scrollback = scroll3)
        val state = TerminalScreenState(snap)
        state.scrollToTop()
        assertEquals(3, state.scrollbackPosition)
        state.scrollToBottom()
        assertEquals(0, state.scrollbackPosition)
        assertTrue(state.isAtBottom())
    }
}
