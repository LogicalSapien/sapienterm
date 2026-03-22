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
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Command history entity for tracking commands executed on each host.
 *
 * Stores executed commands per host to enable command recall and
 * auto-completion in the terminal.
 *
 * @property id Database ID of the history entry
 * @property hostId The host ID where the command was executed
 * @property command The command string that was executed
 * @property executedAt Timestamp when the command was executed
 */
@Entity(
    tableName = "command_history",
    indices = [Index(value = ["host_id"])]
)
data class CommandHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "host_id")
    val hostId: Long,

    val command: String,

    @ColumnInfo(name = "executed_at", defaultValue = "0")
    val executedAt: Long = System.currentTimeMillis()
)
