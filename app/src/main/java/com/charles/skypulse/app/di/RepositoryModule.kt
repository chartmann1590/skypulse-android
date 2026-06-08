package com.charles.skypulse.app.di

import com.charles.skypulse.app.data.repository.AircraftRepository
import com.charles.skypulse.app.data.repository.AircraftRepositoryImpl
import com.charles.skypulse.app.data.repository.FeedbackRepository
import com.charles.skypulse.app.data.repository.FeedbackRepositoryImpl
import com.charles.skypulse.app.domain.entitlement.EntitlementRepository
import com.charles.skypulse.app.domain.entitlement.FreeEntitlementRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAircraftRepository(impl: AircraftRepositoryImpl): AircraftRepository

    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(impl: FeedbackRepositoryImpl): FeedbackRepository

    companion object {
        // Billing not implemented yet (Plan §12) — everyone is on the free tier.
        @Provides
        @Singleton
        fun provideEntitlementRepository(): EntitlementRepository = FreeEntitlementRepository()
    }
}
