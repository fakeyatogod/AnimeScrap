package com.talent.animescrap.di

import com.talent.animescrap.repo.AnimeRepository
import com.talent.animescrap.repo.AnimeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi

@Module
@InstallIn(SingletonComponent::class)
@ExperimentalCoroutinesApi
abstract class RepositoryModule {

    @Binds
    abstract fun bindAnimeRepository(repository: AnimeRepositoryImpl): AnimeRepository

}