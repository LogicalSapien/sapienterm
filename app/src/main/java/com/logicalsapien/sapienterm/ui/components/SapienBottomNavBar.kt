/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.logicalsapien.sapienterm.ui.navigation.Screen
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

/**
 * 3-tab bottom nav (Home / Search / Settings) styled from the active theme tokens.
 * Pill-shaped indicator behind the selected tab.
 */
@Composable
fun SapienBottomNavBar(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.surface)
            .border(width = 1.dp, color = tokens.surfaceBorder, shape = RoundedCornerShape(0.dp))
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Screen.bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route
            val shape = RoundedCornerShape(percent = 50)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(horizontal = 4.dp)
                    .clip(shape)
                    .background(if (isSelected) tokens.primarySubtle else tokens.surface)
                    .clickable {
                        if (!isSelected) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.icon,
                        contentDescription = stringResource(screen.labelRes),
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) tokens.primary else tokens.textMuted
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(screen.labelRes),
                        color = if (isSelected) tokens.primary else tokens.textMuted,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
