package com.talent.animescrap.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.talent.animescrap.model.Photos
import com.talent.animescrap.room.LinksRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {


    private val _favoriteAnimeList = MutableLiveData<ArrayList<Photos>>().apply {
        getFavorites()
    }

    private suspend fun getFavoritesFromRoom() = withContext(Dispatchers.IO) {
        println("GET FAV")
        val db = Room.databaseBuilder(
            getApplication() as Context, LinksRoomDatabase::class.java, "fav-db"
        ).build()

        val listOfFaves = arrayListOf<Photos>()
        val linkDao = db.linkDao()
        val favList = linkDao.getLinks()
        for (fav in favList) {
            val anime = Photos(fav.nameString, fav.picLinkString, fav.linkString)
            listOfFaves.add(anime)
        }

        db.close()
        return@withContext listOfFaves
    }

    fun getFavorites() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getFavoritesFromRoom().apply {
                    withContext(Dispatchers.Main) {
                        _favoriteAnimeList.value = this@apply
                    }
                }
            }
        }
    }

    val favoriteAnimeList: LiveData<ArrayList<Photos>> = _favoriteAnimeList
}