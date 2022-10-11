package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.animescrap.model.AnimeStreamLink
import com.talent.animescrap.repo.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AnimeStreamViewModel @Inject constructor(
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _animeStreamLink: MutableLiveData<AnimeStreamLink> = MutableLiveData()
    val animeStreamLink: LiveData<AnimeStreamLink> = _animeStreamLink

    fun setAnimeLink(animeUrl: String, animeEpCode: String, extras: List<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                animeRepository.getStreamLink(animeUrl, animeEpCode, extras).apply {
                    _animeStreamLink.postValue(this@apply)
                }
            }
        }

    }
}