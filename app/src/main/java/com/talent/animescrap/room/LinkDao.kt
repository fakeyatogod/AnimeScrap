package com.talent.animescrap.room

import androidx.room.*

@Dao
interface LinkDao {
    @Query("SELECT * FROM fav_table")
    fun getLinks(): List<FavRoomModel>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(fav: FavRoomModel)

    @Delete
    fun deleteOne(fav: FavRoomModel)

}