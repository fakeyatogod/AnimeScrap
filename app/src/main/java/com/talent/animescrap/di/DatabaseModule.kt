package com.talent.animescrap.di

import android.app.Application
import androidx.room.Room
import com.talent.animescrap.room.LinkDao
import com.talent.animescrap.room.LinksRoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        application: Application,
    ): LinksRoomDatabase {
        return Room
            .databaseBuilder(application, LinksRoomDatabase::class.java, "fav-db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideLinkDao(appDatabase: LinksRoomDatabase): LinkDao = appDatabase.linkDao()

}