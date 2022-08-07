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

class SearchViewModel : ViewModel() {
    private val _searchedAnimeList = MutableLiveData<ArrayList<SimpleAnime>>()
    val searchedAnimeList: LiveData<ArrayList<SimpleAnime>> = _searchedAnimeList

    fun searchAnime(searchUrl: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AnimeRepository().searchAnimeFromSite(searchUrl).apply {
                    _searchedAnimeList.postValue(this@apply)
                }
            }
        }
    }
}