/*
 * SapienSSH: modern SSH client for Android
 * Copyright 2025 SapienSSH contributors
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

package com.logicalsapien.sapienssh.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Connection group entity for organizing hosts into labeled folders.
 *
 * Connection groups allow users to organize their SSH/Telnet hosts into
 * color-coded groups for easier navigation in the host list.
 *
 * @property id Database ID of the connection group
 * @property name Display name of the group
 * @property color Optional hex color string for the group label (e.g., "#FF5722")
 * @property sortOrder Sort order for display (lower values appear first)
 * @property createdAt Timestamp when the group was created
 */
@Entity(tableName = "connection_groups")
data class ConnectionGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val color: String? = null,

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
)
