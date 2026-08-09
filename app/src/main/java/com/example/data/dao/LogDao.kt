package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.entities.LauncherLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM launcher_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<LauncherLogEntity>>

    @Insert
    suspend fun insertLog(log: LauncherLogEntity)

    @Query("DELETE FROM launcher_logs")
    suspend fun clearLogs()
}
