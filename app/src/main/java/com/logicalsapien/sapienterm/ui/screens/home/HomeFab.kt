/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@Composable
fun HomeFab(
    segment: HomeSegment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens
    val contentDesc = when (segment) {
        HomeSegment.HOSTS -> stringResource(R.string.home_fab_add_host)
        HomeSegment.COMMANDS -> stringResource(R.string.home_fab_add_command)
        HomeSegment.KEYS -> stringResource(R.string.home_fab_add_key)
    }
    FloatingActionButton(
        onClick = onClick,
        containerColor = tokens.primary,
        contentColor = tokens.textOnPrimary,
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = contentDesc)
    }
}
