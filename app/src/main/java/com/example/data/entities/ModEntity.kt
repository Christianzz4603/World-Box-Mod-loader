package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mods")
data class ModEntity(
    @PrimaryKey
    val id: String, // Unique UUID or package identifier
    val modId: String,
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val category: String,
    val localPath: String,
    val format: String, // NCMOD, ZIP, DLL, JSON
    val isAndroidCompatible: Boolean,
    val compatibilityNotes: String,
    val targetGameVersion: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val iconUrl: String? = null,
    val fileSize: Long = 0L,
    val isEnabled: Boolean = true,
    val loadOrder: Int = 0
)
