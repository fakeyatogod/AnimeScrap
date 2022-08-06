package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.repo.AnimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnimeDetailsViewModel : ViewModel() {
    private val _animeDetails = MutableLiveData<AnimeDetails>()

    fun getAnimeDetails(contentLink: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AnimeRepository().getAnimeDetailsFromSite(contentLink).apply {
                    withContext(Dispatchers.Main) {
                        _animeDetails.value = this@apply
                    }
                }
            }
        }
    }

    val animeDetails: LiveData<AnimeDetails> = _animeDetails
}