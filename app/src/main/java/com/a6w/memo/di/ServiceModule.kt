package com.a6w.memo.di

import com.a6w.memo.common.service.MapmoNotificationServiceImpl
import com.a6w.memo.domain.service.MapmoNotificationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindNotificationService(impl: MapmoNotificationServiceImpl): MapmoNotificationService
}