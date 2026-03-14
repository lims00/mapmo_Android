package com.a6w.memo.di

import android.app.PendingIntent
import com.a6w.memo.data.repository.GeofenceRepositoryImpl
import com.a6w.memo.data.repository.LabelRepositoryImpl
import com.a6w.memo.data.repository.MapmoListRepositoryImpl
import com.a6w.memo.data.repository.MapmoRepositoryImpl
import com.a6w.memo.domain.repository.GeofenceRepository
import com.a6w.memo.domain.repository.LabelRepository
import com.a6w.memo.domain.repository.MapmoListRepository
import com.a6w.memo.domain.repository.MapmoRepository
import com.google.android.gms.location.GeofencingClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideGeofenceRepository(
        geofencingClient: GeofencingClient,
        geofencePendingIntent: PendingIntent,
    ): GeofenceRepository = GeofenceRepositoryImpl(geofencingClient, geofencePendingIntent)

    @Provides
    @Singleton
    fun provideMapmoRepository(): MapmoRepository = MapmoRepositoryImpl()

    @Provides
    @Singleton
    fun provideMapmoListRepository(): MapmoListRepository = MapmoListRepositoryImpl()

    @Provides
    @Singleton
    fun provideLabelRepository(): LabelRepository = LabelRepositoryImpl()
}