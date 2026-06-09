package com.logicalsapien.sapienterm.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.logicalsapien.sapienterm.data.entity.PromptTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptTemplateDao {
    @Query("SELECT * FROM prompt_templates ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<PromptTemplate>>

    @Query("SELECT * FROM prompt_templates ORDER BY sort_order ASC, id ASC")
    suspend fun getAll(): List<PromptTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: PromptTemplate): Long

    @Delete
    suspend fun delete(template: PromptTemplate)

    @Query("SELECT COUNT(*) FROM prompt_templates WHERE is_builtin = 1")
    suspend fun countBuiltin(): Int
}
