/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DensityMode(val storageString: String) {
    COMFORTABLE("comfortable"),
    COMPACT("compact");

    companion object {
        val DEFAULT: DensityMode = COMFORTABLE

        fun fromStorageString(value: String?): DensityMode {
            if (value == null) return DEFAULT
            return entries.firstOrNull { it.storageString.equals(value, ignoreCase = true) } ?: DEFAULT
        }
    }
}

data class DensityTokens(
    val mode: DensityMode,
    val cardPadding: Dp,
    val cardGap: Dp,
    val rowHeight: Dp,
    val showSubtitle: Boolean,
    val showMetadata: Boolean
) {
    companion object {
        val Comfortable: DensityTokens = DensityTokens(
            mode = DensityMode.COMFORTABLE,
            cardPadding = 12.dp,
            cardGap = 8.dp,
            rowHeight = 48.dp,
            showSubtitle = true,
            showMetadata = true
        )

        val Compact: DensityTokens = DensityTokens(
            mode = DensityMode.COMPACT,
            cardPadding = 6.dp,
            cardGap = 2.dp,
            rowHeight = 32.dp,
            showSubtitle = false,
            showMetadata = false
        )

        fun forMode(mode: DensityMode): DensityTokens = when (mode) {
            DensityMode.COMFORTABLE -> Comfortable
            DensityMode.COMPACT -> Compact
        }
    }
}
