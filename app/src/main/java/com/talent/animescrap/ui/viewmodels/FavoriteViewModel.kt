package com.talent.animescrap.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.talent.animescrap.model.Photos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteViewModel : ViewModel() {

    private var aList1 = arrayListOf<Photos>()

    private val _animeList1 = MutableLiveData<ArrayList<Photos>>().apply {

        CoroutineScope(Dispatchers.IO).launch {
            getLatestAnime()
            withContext(Dispatchers.Main) {
                value = aList1

            }
        }
    }

    private fun getLatestAnime() {
        val picInfo = arrayListOf<Photos>()

        //to make fav list

        aList1 = picInfo
    }

    val animeFavoriteList: LiveData<ArrayList<Photos>> = _animeList1
}