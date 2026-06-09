/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.data.search

import com.logicalsapien.sapienterm.data.dao.CredentialDao
import com.logicalsapien.sapienterm.data.dao.HostDao
import com.logicalsapien.sapienterm.data.dao.QuickCommandDao
import com.logicalsapien.sapienterm.data.entity.Host
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

@Singleton
class SearchRepository @Inject constructor(
    private val hostDao: HostDao,
    private val commandDao: QuickCommandDao,
    private val credentialDao: CredentialDao
) {
    /**
     * Search across hosts, quick commands, and credentials.
     * Blank query returns [SearchResults.EMPTY] — callers should render recents/pinned instead.
     */
    fun search(query: String): Flow<SearchResults> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return flowOf(SearchResults.EMPTY)
        val needle = trimmed.lowercase()

        return combine(
            hostDao.observeAll(),
            commandDao.observeAll(),
            credentialDao.observeAll()
        ) { hosts, commands, credentials ->
            SearchResults(
                hosts = hosts.filter { host ->
                    host.nickname.lowercase().contains(needle) ||
                        host.hostname.lowercase().contains(needle) ||
                        host.username.lowercase().contains(needle)
                },
                commands = commands.filter { cmd ->
                    cmd.title.lowercase().contains(needle) ||
                        cmd.command.lowercase().contains(needle) ||
                        (cmd.category?.lowercase()?.contains(needle) == true)
                },
                credentials = credentials.filter { cred ->
                    cred.label.lowercase().contains(needle)
                }
            )
        }
    }

    fun recentHosts(limit: Int = 5): Flow<List<Host>> = hostDao.observeRecent(limit)

    fun pinnedHosts(): Flow<List<Host>> = hostDao.observePinned()
}
