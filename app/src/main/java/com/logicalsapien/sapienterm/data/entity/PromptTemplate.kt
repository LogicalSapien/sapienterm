package com.logicalsapien.sapienterm.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_templates")
data class PromptTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val template: String,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_builtin", defaultValue = "0")
    val isBuiltin: Boolean = false,
)
