package com.logicalsapien.sapienterm.data

import com.logicalsapien.sapienterm.data.dao.PromptTemplateDao
import com.logicalsapien.sapienterm.data.entity.PromptTemplate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptTemplateRepository @Inject constructor(
    private val dao: PromptTemplateDao,
) {
    fun observeAll(): Flow<List<PromptTemplate>> = dao.observeAll()

    suspend fun getAll(): List<PromptTemplate> = dao.getAll()

    suspend fun insert(template: PromptTemplate): Long = dao.insert(template)

    suspend fun delete(template: PromptTemplate) = dao.delete(template)

    suspend fun seedBuiltins() {
        if (dao.countBuiltin() > 0) return
        val builtins = listOf(
            PromptTemplate(title = "Review code", template = "Review this code and suggest improvements:\n\n", sortOrder = 0, isBuiltin = true),
            PromptTemplate(title = "Explain error", template = "Explain this error and how to fix it:\n\n", sortOrder = 1, isBuiltin = true),
            PromptTemplate(title = "Write tests", template = "Write unit tests for:\n\n", sortOrder = 2, isBuiltin = true),
            PromptTemplate(title = "Summarise", template = "Summarise the key points of:\n\n", sortOrder = 3, isBuiltin = true),
            PromptTemplate(title = "Refactor", template = "Refactor this for readability:\n\n", sortOrder = 4, isBuiltin = true),
        )
        builtins.forEach { dao.insert(it) }
    }
}
