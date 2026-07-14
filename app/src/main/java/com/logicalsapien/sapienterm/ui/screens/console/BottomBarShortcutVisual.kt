/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.logicalsapien.sapienterm.ui.screens.console

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.service.ModifierLevel
import com.logicalsapien.sapienterm.service.ModifierState

private val IconDp = 22.dp

/**
 * Compact keycap text for built-in strips (for example tmux ^B / ^D) — icon-sized, no separate word label.
 */
@Composable
fun BottomBarKeyCap(
    text: String,
    color: Color,
    accessibilityLabel: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.sp
        ),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        modifier = modifier
            .size(IconDp)
            .semantics { contentDescription = accessibilityLabel }
    )
}

/**
 * Which popup panel is open above the bottom bar (quick commands, history, more keys).
 */
enum class BottomBarPanel {
    NONE,
    COMMANDS,
    HISTORY,
    MORE_KEYS
}

/**
 * Icon-first (or single-glyph) visuals for custom strip actions. Ctrl+letter chords use compact keycap text.
 */
@Composable
fun BottomBarShortcutVisual(
    action: BottomBarShortcutAction,
    panel: BottomBarPanel,
    modifierState: ModifierState?,
    tintActive: Color,
    tintIdle: Color,
    modifier: Modifier = Modifier
) {
    val ctrlTint = if (modifierState != null && action == BottomBarShortcutAction.CTRL_TOGGLE) {
        when (modifierState.ctrlState) {
            ModifierLevel.OFF -> tintIdle
            ModifierLevel.TRANSIENT, ModifierLevel.LOCKED -> tintActive
        }
    } else {
        tintIdle
    }
    val altTint = if (modifierState != null && action == BottomBarShortcutAction.ALT) {
        when (modifierState.altState) {
            ModifierLevel.OFF -> tintIdle
            ModifierLevel.TRANSIENT, ModifierLevel.LOCKED -> tintActive
        }
    } else {
        tintIdle
    }
    val desc = stringResource(action.descriptionRes)
    Box(modifier = modifier.size(IconDp), contentAlignment = Alignment.Center) {
        when (action) {
            BottomBarShortcutAction.TEXT_INPUT -> Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.COMMANDS -> Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = desc,
                tint = if (panel == BottomBarPanel.COMMANDS) tintActive else tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.HISTORY -> Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = desc,
                tint = if (panel == BottomBarPanel.HISTORY) tintActive else tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.MORE_KEYS -> Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = desc,
                tint = if (panel == BottomBarPanel.MORE_KEYS) tintActive else tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.ENTER -> Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.TAB -> Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardTab,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.BACKSPACE -> Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.ESC -> GlyphOnly(
                text = "\u238B",
                accessibilityLabel = desc,
                color = tintIdle
            )

            BottomBarShortcutAction.ARROW_LEFT -> Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.ARROW_RIGHT -> Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.ARROW_UP -> Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.ARROW_DOWN -> Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.HOME -> Icon(
                imageVector = Icons.Default.FirstPage,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.END -> Icon(
                imageVector = Icons.AutoMirrored.Filled.LastPage,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            // Double chevrons — distinct from single ArrowUpward/ArrowDownward (cursor) and ExpandLess (looks like “up”).
            BottomBarShortcutAction.PAGE_UP -> Icon(
                imageVector = Icons.Filled.KeyboardDoubleArrowUp,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.PAGE_DOWN -> Icon(
                imageVector = Icons.Filled.KeyboardDoubleArrowDown,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.ALT -> GlyphOnly(
                text = "\u2325",
                accessibilityLabel = desc,
                color = altTint
            )

            BottomBarShortcutAction.CTRL_TOGGLE -> Keycap(
                text = stringResource(R.string.bottom_bar_ctrl_compact),
                accessibilityLabel = desc,
                color = ctrlTint
            )

            BottomBarShortcutAction.FONT_DEC -> Icon(
                imageVector = Icons.Default.TextDecrease,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.FONT_INC -> Icon(
                imageVector = Icons.Default.TextIncrease,
                contentDescription = desc,
                tint = tintIdle,
                modifier = Modifier.size(IconDp)
            )

            BottomBarShortcutAction.PIPE,
            BottomBarShortcutAction.DASH,
            BottomBarShortcutAction.SLASH,
            BottomBarShortcutAction.TILDE -> GlyphOnly(
                text = action.displayLabel,
                accessibilityLabel = desc,
                color = tintIdle
            )

            BottomBarShortcutAction.CTRL_S -> Keycap("^S", desc, tintIdle)

            BottomBarShortcutAction.CTRL_B -> Keycap("^B", desc, tintIdle)

            BottomBarShortcutAction.CTRL_D -> Keycap("^D", desc, tintIdle)

            BottomBarShortcutAction.CTRL_C -> Keycap("^C", desc, tintIdle)

            BottomBarShortcutAction.CTRL_Z -> Keycap("^Z", desc, tintIdle)

            BottomBarShortcutAction.CTRL_A -> Keycap("^A", desc, tintIdle)

            BottomBarShortcutAction.CTRL_E -> Keycap("^E", desc, tintIdle)

            BottomBarShortcutAction.CTRL_K -> Keycap("^K", desc, tintIdle)

            BottomBarShortcutAction.CTRL_U -> Keycap("^U", desc, tintIdle)

            BottomBarShortcutAction.CTRL_L -> Keycap("^L", desc, tintIdle)
        }
    }
}

@Composable
private fun Keycap(text: String, accessibilityLabel: String, color: Color) {
    BottomBarKeyCap(text = text, color = color, accessibilityLabel = accessibilityLabel)
}

@Composable
private fun GlyphOnly(text: String, accessibilityLabel: String, color: Color) {
    Text(
        text = text,
        // titleMedium was far too large for IconDp; glyphs looked clipped or “missing”.
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp,
            lineHeight = 14.sp
        ),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .size(IconDp)
            .semantics { contentDescription = accessibilityLabel }
    )
}
