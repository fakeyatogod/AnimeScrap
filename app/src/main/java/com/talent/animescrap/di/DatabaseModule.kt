package com.talent.animescrap.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE fav_table ADD COLUMN favSource TEXT DEFAULT 'yugen'")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        application: Application,
    ): LinksRoomDatabase {
        return Room
            .databaseBuilder(application, LinksRoomDatabase::class.java, "fav-db")
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideLinkDao(appDatabase: LinksRoomDatabase): LinkDao = appDatabase.linkDao()

}