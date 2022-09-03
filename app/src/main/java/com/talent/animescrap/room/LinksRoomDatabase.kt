package com.talent.animescrap.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavRoomModel::class], version = 3)
abstract class LinksRoomDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
}