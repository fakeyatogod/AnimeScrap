package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.repo.AnimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrendingViewModel : ViewModel() {
    private val _trendingAnimeList =
        MutableLiveData<ArrayList<SimpleAnime>>().apply { getTrendingAnimeList() }
    val trendingAnimeList: LiveData<ArrayList<SimpleAnime>> = _trendingAnimeList


    fun getTrendingAnimeList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AnimeRepository().getTrendingAnimeFromSite().apply {
                    _trendingAnimeList.postValue(this@apply)
                }
            }
        }
    }

}