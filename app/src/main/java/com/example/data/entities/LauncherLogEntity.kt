package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_logs")
data class LauncherLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // INFO, WARN, ERROR, SUCCESS
    val tag: String,
    val message: String
)
