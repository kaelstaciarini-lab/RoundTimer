package com.roundtimer.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Preset>>

    @Insert
    suspend fun insert(preset: Preset): Long

    @Delete
    suspend fun delete(preset: Preset)
}
