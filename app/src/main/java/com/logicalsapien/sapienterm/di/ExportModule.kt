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

package com.logicalsapien.sapienterm.di

import android.content.Context
import android.content.SharedPreferences
import com.logicalsapien.sapienterm.data.ConnectionGroupRepository
import com.logicalsapien.sapienterm.data.CredentialRepository
import com.logicalsapien.sapienterm.data.HostRepository
import com.logicalsapien.sapienterm.data.ProfileRepository
import com.logicalsapien.sapienterm.data.QuickCommandRepository
import com.logicalsapien.sapienterm.data.export.AutoBackupManager
import com.logicalsapien.sapienterm.data.export.ExportManager
import com.logicalsapien.sapienterm.data.export.ImportManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing export/import managers for data backup and restore.
 */
@Module
@InstallIn(SingletonComponent::class)
object ExportModule {

    @Provides
    @Singleton
    fun provideExportManager(
        @ApplicationContext context: Context,
        hostRepository: HostRepository,
        quickCommandRepository: QuickCommandRepository,
        credentialRepository: CredentialRepository,
        connectionGroupRepository: ConnectionGroupRepository,
        profileRepository: ProfileRepository,
        prefs: SharedPreferences,
        dispatchers: CoroutineDispatchers
    ): ExportManager = ExportManager(
        context,
        hostRepository,
        quickCommandRepository,
        credentialRepository,
        connectionGroupRepository,
        profileRepository,
        prefs,
        dispatchers
    )

    @Provides
    @Singleton
    fun provideImportManager(
        @ApplicationContext context: Context,
        hostRepository: HostRepository,
        quickCommandRepository: QuickCommandRepository,
        credentialRepository: CredentialRepository,
        connectionGroupRepository: ConnectionGroupRepository,
        profileRepository: ProfileRepository,
        prefs: SharedPreferences,
        dispatchers: CoroutineDispatchers
    ): ImportManager = ImportManager(
        context,
        hostRepository,
        quickCommandRepository,
        credentialRepository,
        connectionGroupRepository,
        profileRepository,
        prefs,
        dispatchers
    )

    @Provides
    @Singleton
    fun provideAutoBackupManager(
        @ApplicationContext context: Context,
        prefs: SharedPreferences,
        hostRepository: HostRepository,
        quickCommandRepository: QuickCommandRepository,
        credentialRepository: CredentialRepository,
        connectionGroupRepository: ConnectionGroupRepository,
        dispatchers: CoroutineDispatchers
    ): AutoBackupManager = AutoBackupManager(
        context,
        prefs,
        hostRepository,
        quickCommandRepository,
        credentialRepository,
        connectionGroupRepository,
        dispatchers
    )
}
