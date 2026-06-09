/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    placeholderText: String? = null
) {
    val tokens = SapienTheme.tokens
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (autoFocus) runCatching { focusRequester.requestFocus() }
    }

    val placeholder = placeholderText ?: stringResource(R.string.search_field_placeholder)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(placeholder, color = tokens.textMuted) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = tokens.textMuted)
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_field_clear_a11y),
                        tint = tokens.textMuted
                    )
                }
            }
        } else null,
        shape = RoundedCornerShape(tokens.cornerLarge),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = tokens.surface,
            unfocusedContainerColor = tokens.surface,
            focusedBorderColor = tokens.primary,
            unfocusedBorderColor = tokens.surfaceBorder,
            focusedTextColor = tokens.textPrimary,
            unfocusedTextColor = tokens.textPrimary,
            cursorColor = tokens.primary
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusRequester(focusRequester)
    )
}
