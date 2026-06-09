/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.logicalsapien.sapienterm.ui.screens.console

import androidx.annotation.StringRes
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.service.TerminalBridge
import com.logicalsapien.sapienterm.service.TerminalKeyListener
import org.connectbot.terminal.VTermKey

/**
 * Keys that can be placed on the custom shortcut strip.
 * Includes Ctrl sequences, More-keys panel actions, and session UI.
 * Persisted by id in [com.logicalsapien.sapienterm.data.CustomBottomBarLayout].
 */
enum class BottomBarShortcutAction(
    val id: String,
    val displayLabel: String,
    @StringRes val descriptionRes: Int
) {
    CTRL_S("ctrl_s", "^S", R.string.bottom_bar_action_ctrl_s),
    CTRL_B("ctrl_b", "^B", R.string.bottom_bar_action_ctrl_b),
    CTRL_D("ctrl_d", "^D", R.string.bottom_bar_action_ctrl_d),
    CTRL_C("ctrl_c", "^C", R.string.bottom_bar_action_ctrl_c),
    CTRL_Z("ctrl_z", "^Z", R.string.bottom_bar_action_ctrl_z),
    CTRL_A("ctrl_a", "^A", R.string.bottom_bar_action_ctrl_a),
    CTRL_E("ctrl_e", "^E", R.string.bottom_bar_action_ctrl_e),
    CTRL_K("ctrl_k", "^K", R.string.bottom_bar_action_ctrl_k),
    CTRL_U("ctrl_u", "^U", R.string.bottom_bar_action_ctrl_u),
    CTRL_L("ctrl_l", "^L", R.string.bottom_bar_action_ctrl_l),
    TAB("tab", "Tab", R.string.bottom_bar_action_tab),
    ESC("esc", "Esc", R.string.bottom_bar_action_esc),
    ARROW_LEFT("arrow_left", "\u2190", R.string.bottom_bar_action_arrow_left),
    ARROW_RIGHT("arrow_right", "\u2192", R.string.bottom_bar_action_arrow_right),
    ARROW_UP("arrow_up", "\u2191", R.string.bottom_bar_action_arrow_up),
    ARROW_DOWN("arrow_down", "\u2193", R.string.bottom_bar_action_arrow_down),
    HOME("home", "Home", R.string.bottom_bar_action_home),
    END("end", "End", R.string.bottom_bar_action_end),
    PAGE_UP("page_up", "PgUp", R.string.bottom_bar_action_page_up),
    PAGE_DOWN("page_down", "PgDn", R.string.bottom_bar_action_page_down),
    ALT("alt", "Alt", R.string.bottom_bar_action_alt),
    CTRL_TOGGLE("ctrl_toggle", "Ctrl", R.string.bottom_bar_action_ctrl_toggle),
    PIPE("pipe", "|", R.string.bottom_bar_action_pipe),
    DASH("dash", "-", R.string.bottom_bar_action_dash),
    SLASH("slash", "/", R.string.bottom_bar_action_slash),
    TILDE("tilde", "~", R.string.bottom_bar_action_tilde),
    FONT_DEC("font_dec", "A−", R.string.bottom_bar_action_font_dec),
    FONT_INC("font_inc", "A+", R.string.bottom_bar_action_font_inc),

    /** Floating text input (same as mic / text-in dialog). */
    TEXT_INPUT("text_input", "Edit", R.string.bottom_bar_action_text_input),

    /** Toggle quick-commands panel. */
    COMMANDS("commands", "Cmds", R.string.bottom_bar_action_commands),

    /** Toggle command history panel. */
    HISTORY("history", "Hist", R.string.bottom_bar_action_history),

    /** Send Enter. */
    ENTER("enter", "\u23CE", R.string.bottom_bar_action_enter),

    /** Toggle the “more shortcuts” panel (Esc, Tab, PgUp, font, etc.). */
    MORE_KEYS("more_keys", "\u22EF", R.string.bottom_bar_action_more_keys);

    companion object {
        fun fromId(value: String?): BottomBarShortcutAction? = entries.find { it.id == value?.trim() }
    }
}

/**
 * Runs a custom bottom-bar action: terminal bytes, navigation keys, modifier toggles,
 * font zoom, session UI (text input, panels), or Enter.
 */
fun performCustomBottomBarAction(
    action: BottomBarShortcutAction,
    bridge: TerminalBridge,
    keyHandler: TerminalKeyListener,
    onOpenTextInput: () -> Unit,
    onOpenCommandsPanel: () -> Unit,
    onOpenHistoryPanel: () -> Unit,
    onOpenMorePanel: () -> Unit
) {
    when (action) {
        BottomBarShortcutAction.CTRL_S -> bridge.sendCtrlS()
        BottomBarShortcutAction.CTRL_B -> bridge.sendCtrlB()
        BottomBarShortcutAction.CTRL_D -> bridge.sendCtrlD()
        BottomBarShortcutAction.CTRL_C -> bridge.sendEscapeSequence("\u0003")
        BottomBarShortcutAction.CTRL_Z -> bridge.sendEscapeSequence("\u001A")
        BottomBarShortcutAction.CTRL_A -> bridge.sendEscapeSequence("\u0001")
        BottomBarShortcutAction.CTRL_E -> bridge.sendEscapeSequence("\u0005")
        BottomBarShortcutAction.CTRL_K -> bridge.sendEscapeSequence("\u000B")
        BottomBarShortcutAction.CTRL_U -> bridge.sendEscapeSequence("\u0015")
        BottomBarShortcutAction.CTRL_L -> bridge.sendEscapeSequence("\u000C")
        BottomBarShortcutAction.TAB -> bridge.sendEscapeSequence("\u0009")
        BottomBarShortcutAction.ESC -> bridge.sendEscapeSequence("\u001b")
        BottomBarShortcutAction.ARROW_LEFT -> keyHandler.sendPressedKey(VTermKey.LEFT)
        BottomBarShortcutAction.ARROW_RIGHT -> keyHandler.sendPressedKey(VTermKey.RIGHT)
        BottomBarShortcutAction.ARROW_UP -> keyHandler.sendPressedKey(VTermKey.UP)
        BottomBarShortcutAction.ARROW_DOWN -> keyHandler.sendPressedKey(VTermKey.DOWN)
        BottomBarShortcutAction.HOME -> keyHandler.sendPressedKey(VTermKey.HOME)
        BottomBarShortcutAction.END -> keyHandler.sendPressedKey(VTermKey.END)
        BottomBarShortcutAction.PAGE_UP -> bridge.sendPageUp()
        BottomBarShortcutAction.PAGE_DOWN -> bridge.sendPageDown()
        BottomBarShortcutAction.ALT -> keyHandler.metaPress(TerminalKeyListener.OUR_ALT_ON, true)
        BottomBarShortcutAction.CTRL_TOGGLE -> keyHandler.metaPress(TerminalKeyListener.OUR_CTRL_ON, true)
        BottomBarShortcutAction.PIPE -> bridge.injectString("|")
        BottomBarShortcutAction.DASH -> bridge.injectString("-")
        BottomBarShortcutAction.SLASH -> bridge.injectString("/")
        BottomBarShortcutAction.TILDE -> bridge.injectString("~")
        BottomBarShortcutAction.FONT_DEC -> bridge.decreaseFontSize()
        BottomBarShortcutAction.FONT_INC -> bridge.increaseFontSize()
        BottomBarShortcutAction.TEXT_INPUT -> onOpenTextInput()
        BottomBarShortcutAction.COMMANDS -> onOpenCommandsPanel()
        BottomBarShortcutAction.HISTORY -> onOpenHistoryPanel()
        BottomBarShortcutAction.ENTER -> keyHandler.sendPressedKey(VTermKey.ENTER)
        BottomBarShortcutAction.MORE_KEYS -> onOpenMorePanel()
    }
}
