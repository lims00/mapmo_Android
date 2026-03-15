package com.a6w.memo.di

import com.a6w.memo.data.repository.AddressSearchRepositoryImpl
import com.a6w.memo.data.repository.GeofenceRepositoryImpl
import com.a6w.memo.data.repository.LabelRepositoryImpl
import com.a6w.memo.data.repository.MapmoListRepositoryImpl
import com.a6w.memo.data.repository.MapmoRepositoryImpl
import com.a6w.memo.domain.repository.AddressSearchRepository
import com.a6w.memo.domain.repository.GeofenceRepository
import com.a6w.memo.domain.repository.LabelRepository
import com.a6w.memo.domain.repository.MapmoListRepository
import com.a6w.memo.domain.repository.MapmoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGeofenceRepository(
        impl: GeofenceRepositoryImpl
    ): GeofenceRepository

    @Binds
    @Singleton
    abstract fun bindLabelRepository(
        impl: LabelRepositoryImpl
    ): LabelRepository

    @Binds
    @Singleton
    abstract fun bindMapmoRepository(
        impl: MapmoRepositoryImpl
    ): MapmoRepository

    @Binds
    @Singleton
    abstract fun bindMapmoListRepository(
        impl: MapmoListRepositoryImpl
    ): MapmoListRepository

    @Binds
    @Singleton
    abstract fun bindAddressSearchRepository(
        impl: AddressSearchRepositoryImpl
    ): AddressSearchRepository
}