package com.roundtimer.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Preset::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
}
