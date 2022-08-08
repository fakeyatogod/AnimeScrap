package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.repo.AnimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AnimeStreamViewModel : ViewModel() {

    private val _animeStreamLink: MutableLiveData<String> = MutableLiveData()
    val animeStreamLink: LiveData<String> = _animeStreamLink

    fun setAnimeLink(animeEpUrl: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AnimeRepository().getStreamLink(animeEpUrl).apply {
                    _animeStreamLink.postValue(this@apply)
                }
            }
        }

    }
}