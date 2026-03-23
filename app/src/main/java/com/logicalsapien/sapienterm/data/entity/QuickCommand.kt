/*
 * SapienTerm: modern SSH client for Android
 * Copyright 2025 SapienTerm contributors
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

package com.logicalsapien.sapienterm.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Quick command entity for storing reusable terminal commands.
 *
 * Quick commands allow users to save frequently used commands for quick
 * execution from the terminal toolbar. Commands can be organized by category
 * and ordered for easy access.
 *
 * @property id Database ID of the quick command
 * @property title Display name of the command
 * @property command The actual command string to execute
 * @property category Optional category for grouping commands
 * @property createdAt Timestamp when the command was created
 * @property sortOrder Sort order for display (lower values appear first)
 */
@Entity(tableName = "quick_commands")
data class QuickCommand(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val command: String,

    val category: String? = null,

    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0
)
