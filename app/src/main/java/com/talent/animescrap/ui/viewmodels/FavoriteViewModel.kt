package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap_common.model.SimpleAnime
import com.talent.animescrap.repo.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _favoriteAnimeList = MutableLiveData<List<SimpleAnime>>().apply {
        getFavorites()
    }
    val favoriteAnimeList: LiveData<List<SimpleAnime>> = _favoriteAnimeList

    fun getFavorites() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                animeRepository.getFavoritesFromRoom().collect {
                    _favoriteAnimeList.postValue(it)
                }
            }
        }
    }

}