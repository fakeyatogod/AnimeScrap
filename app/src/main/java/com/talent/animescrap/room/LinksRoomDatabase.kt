package com.talent.animescrap.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavLinks::class], version = 1)
abstract class LinksRoomDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
}