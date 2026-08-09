package com.example.data.entities

import androidx.room.Entity

@Entity(
    tableName = "profile_mod_cross_ref",
    primaryKeys = ["profileId", "modId"]
)
data class ProfileModCrossRef(
    val profileId: String,
    val modId: String,
    val isEnabled: Boolean = true,
    val loadOrder: Int = 0
)
