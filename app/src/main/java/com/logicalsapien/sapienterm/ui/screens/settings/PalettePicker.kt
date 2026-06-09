/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.data.prefs.AppearancePreferences
import com.logicalsapien.sapienterm.ui.theme.NeoTerminalTheme
import com.logicalsapien.sapienterm.ui.theme.QuietLuxuryTheme
import com.logicalsapien.sapienterm.ui.theme.SapienTheme
import com.logicalsapien.sapienterm.ui.theme.SapienThemeEntryPoint
import com.logicalsapien.sapienterm.ui.theme.SoftProTheme
import com.logicalsapien.sapienterm.ui.theme.ThemeId
import com.logicalsapien.sapienterm.ui.theme.ThemeTokens
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * Horizontal row of three palette swatches that write the user's pick into
 * [AppearancePreferences.themeId]. Lives above the category grid in the
 * settings hub so the user can immediately see the effect of switching.
 */
@Composable
fun PalettePicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SapienThemeEntryPoint::class.java
        ).appearancePreferences()
    }
    val currentId by prefs.themeId.collectAsState(initial = ThemeId.DEFAULT)
    val scope = rememberCoroutineScope()
    val tokens = SapienTheme.tokens

    val entries = listOf(
        ThemeId.NEO_TERMINAL to NeoTerminalTheme,
        ThemeId.QUIET_LUXURY to QuietLuxuryTheme,
        ThemeId.SOFT_PRO to SoftProTheme
    )

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Palette",
            color = tokens.textMuted,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEach { (id, preview) ->
                PaletteChip(
                    label = id.displayName(),
                    preview = preview,
                    selected = id == currentId,
                    onClick = {
                        scope.launch { prefs.setThemeId(id) }
                    }
                )
            }
        }
    }
}

@Composable
private fun PaletteChip(
    label: String,
    preview: ThemeTokens,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = SapienTheme.tokens
    val borderColor: Color = if (selected) tokens.primary else tokens.surfaceBorder
    Surface(
        shape = RoundedCornerShape(tokens.cornerMedium),
        color = tokens.surface,
        modifier = Modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(tokens.cornerMedium)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Swatch: a small circle of the preview theme's primary on its surface
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(preview.surface, CircleShape)
                    .border(1.dp, preview.surfaceBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(preview.primary, CircleShape)
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = label,
                color = tokens.textPrimary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun ThemeId.displayName(): String = when (this) {
    ThemeId.NEO_TERMINAL -> "Neo-Terminal"
    ThemeId.QUIET_LUXURY -> "Quiet Luxury"
    ThemeId.SOFT_PRO -> "Soft Pro"
}
