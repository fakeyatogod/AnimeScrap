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

class LatestViewModel : ViewModel() {
    private val _latestAnimeList = MutableLiveData<ArrayList<SimpleAnime>>().apply {
        getLatestAnimeList()
    }

    fun getLatestAnimeList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AnimeRepository().getLatestAnimeFromSite().apply {
                    withContext(Dispatchers.Main) {
                        _latestAnimeList.value = this@apply
                    }
                }
            }
        }
    }

    val latestAnimeList: LiveData<ArrayList<SimpleAnime>> = _latestAnimeList
}