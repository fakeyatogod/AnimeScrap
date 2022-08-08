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
class LatestViewModel @Inject constructor(
    private val animeRepository: AnimeRepository
) : ViewModel() {
    private val _latestAnimeList = MutableLiveData<ArrayList<SimpleAnime>>().apply {
        getLatestAnimeList()
    }
    val latestAnimeList: LiveData<ArrayList<SimpleAnime>> = _latestAnimeList

    fun getLatestAnimeList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                animeRepository.getLatestAnimeFromSite().apply {
                    _latestAnimeList.postValue(this@apply)
                }
            }
        }
    }

}