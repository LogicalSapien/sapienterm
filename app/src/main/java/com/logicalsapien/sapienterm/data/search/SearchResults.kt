/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.data.search

import com.logicalsapien.sapienterm.data.entity.Credential
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.data.entity.QuickCommand

/**
 * Grouped global-search results across hosts, commands, and credentials.
 */
data class SearchResults(
    val hosts: List<Host> = emptyList(),
    val commands: List<QuickCommand> = emptyList(),
    val credentials: List<Credential> = emptyList()
) {
    val isEmpty: Boolean get() = hosts.isEmpty() && commands.isEmpty() && credentials.isEmpty()
    val totalCount: Int get() = hosts.size + commands.size + credentials.size

    companion object {
        val EMPTY = SearchResults()
    }
}
