package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.repo.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val animeRepository: AnimeRepository
) : ViewModel() {
    private val _trendingAnimeList =
        MutableLiveData<ArrayList<SimpleAnime>>().apply { getTrendingAnimeList() }
    val trendingAnimeList: LiveData<ArrayList<SimpleAnime>> = _trendingAnimeList


    fun getTrendingAnimeList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                animeRepository.getTrendingAnimeFromSite().apply {
                    _trendingAnimeList.postValue(this@apply)
                }
            }
        }
    }

}