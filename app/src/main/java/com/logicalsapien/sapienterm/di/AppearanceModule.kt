/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

internal val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sapien_appearance"
)

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppearanceDataStore

@Module
@InstallIn(SingletonComponent::class)
object AppearanceModule {

    @Provides
    @Singleton
    @AppearanceDataStore
    fun provideAppearanceDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.appearanceDataStore
}
