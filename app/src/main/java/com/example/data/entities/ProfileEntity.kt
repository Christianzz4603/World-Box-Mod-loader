package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val isActive: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)
