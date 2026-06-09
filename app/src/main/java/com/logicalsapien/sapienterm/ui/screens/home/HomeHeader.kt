/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewHeadline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.ui.theme.DensityMode
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@Composable
fun HomeHeader(
    density: DensityMode,
    onCycleDensity: () -> Unit,
    onSortByColor: () -> Unit,
    onDisconnectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = tokens.textPrimary)) { append("Sapien") }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = tokens.primary)) { append("Term") }
            },
            fontFamily = tokens.fontFamilyHeading
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCycleDensity) {
                val icon = when (density) {
                    DensityMode.COMFORTABLE -> Icons.Outlined.ViewHeadline
                    DensityMode.COMPACT -> Icons.Outlined.ViewAgenda
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.home_density_toggle_a11y),
                    tint = tokens.textMuted
                )
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = tokens.textMuted
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_overflow_sort)) },
                    onClick = {
                        menuOpen = false
                        onSortByColor()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_overflow_disconnect_all)) },
                    onClick = {
                        menuOpen = false
                        onDisconnectAll()
                    }
                )
            }
        }
    }
}
