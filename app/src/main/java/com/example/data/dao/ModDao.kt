package com.example.data.dao

import androidx.room.*
import com.example.data.entities.ModEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModDao {
    @Query("SELECT * FROM mods ORDER BY loadOrder ASC, dateAdded DESC")
    fun getAllMods(): Flow<List<ModEntity>>

    @Query("SELECT * FROM mods WHERE isEnabled = 1 ORDER BY loadOrder ASC")
    fun getEnabledMods(): Flow<List<ModEntity>>

    @Query("SELECT * FROM mods WHERE id = :id")
    suspend fun getModById(id: String): ModEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMod(mod: ModEntity)

    @Update
    suspend fun updateMod(mod: ModEntity)

    @Delete
    suspend fun deleteMod(mod: ModEntity)

    @Query("DELETE FROM mods WHERE id = :id")
    suspend fun deleteModById(id: String)

    @Query("UPDATE mods SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setModEnabled(id: String, isEnabled: Boolean)

    @Query("UPDATE mods SET loadOrder = :order WHERE id = :id")
    suspend fun updateLoadOrder(id: String, order: Int)
}
