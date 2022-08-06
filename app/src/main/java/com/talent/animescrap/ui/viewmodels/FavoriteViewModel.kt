package com.talent.animescrap.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.model.SimpleAnime
import com.talent.animescrap.repo.AnimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {

    private val _favoriteAnimeList = MutableLiveData<ArrayList<SimpleAnime>>().apply {
        getFavorites()
    }

    fun getFavorites() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AnimeRepository().getFavoritesFromRoom(getApplication() as Context).apply {
                    withContext(Dispatchers.Main) {
                        _favoriteAnimeList.value = this@apply
                    }
                }
            }
        }
    }

    val favoriteAnimeList: LiveData<ArrayList<SimpleAnime>> = _favoriteAnimeList
}