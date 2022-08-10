package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.repo.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AnimeDetailsViewModel @Inject constructor(
    private val animeRepository: AnimeRepository
) : ViewModel() {
    private val _animeDetails = MutableLiveData<AnimeDetails>()
    val animeDetails: LiveData<AnimeDetails> = _animeDetails

    fun getAnimeDetails(contentLink: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                animeRepository.getAnimeDetailsFromSite(contentLink).apply {
                    _animeDetails.postValue(this@apply)
                }
            }
        }
    }


    private val _isAnimeFav = MutableLiveData<Boolean>()
    val isAnimeFav: LiveData<Boolean> = _isAnimeFav

    fun checkFavorite(animeLink: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                animeRepository.checkFavoriteFromRoom(animeLink).apply {
                    if (this) _isAnimeFav.postValue(true)
                    else _isAnimeFav.postValue(false)
                }
            }
        }
    }

    fun removeFav(animeLink: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                animeRepository.removeFavFromRoom(animeLink)
                _isAnimeFav.postValue(false)
            }
        }
    }

    fun addToFav(animeLink: String, animeName: String, animeCoverLink: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                animeRepository.addFavToRoom(animeLink, animeName, animeCoverLink)
                _isAnimeFav.postValue(true)
            }
        }

    }
}