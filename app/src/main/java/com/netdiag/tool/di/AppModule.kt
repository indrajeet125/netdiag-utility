package com.netdiag.tool.di

import android.content.Context
import com.netdiag.tool.data.repository.NetworkRepository
import com.netdiag.tool.data.repository.PingRepository
import com.netdiag.tool.data.repository.TelephonyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkRepository(@ApplicationContext context: Context): NetworkRepository {
        return NetworkRepository(context)
    }

    @Provides
    @Singleton
    fun provideTelephonyRepository(@ApplicationContext context: Context): TelephonyRepository {
        return TelephonyRepository(context)
    }

    @Provides
    @Singleton
    fun providePingRepository(@ApplicationContext context: Context): PingRepository {
        return PingRepository(context)
    }
}
