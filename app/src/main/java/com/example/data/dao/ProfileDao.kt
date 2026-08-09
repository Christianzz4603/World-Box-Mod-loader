package com.example.data.dao

import androidx.room.*
import com.example.data.entities.ModEntity
import com.example.data.entities.ProfileEntity
import com.example.data.entities.ProfileModCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdTimestamp ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileSync(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun clearActiveProfiles()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun setActiveProfile(id: String)

    @Transaction
    suspend fun switchActiveProfile(id: String) {
        clearActiveProfiles()
        setActiveProfile(id)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileModCrossRef(crossRef: ProfileModCrossRef)

    @Query("DELETE FROM profile_mod_cross_ref WHERE profileId = :profileId AND modId = :modId")
    suspend fun deleteProfileModCrossRef(profileId: String, modId: String)

    @Query("SELECT m.* FROM mods m INNER JOIN profile_mod_cross_ref ref ON m.id = ref.modId WHERE ref.profileId = :profileId AND ref.isEnabled = 1 ORDER BY ref.loadOrder ASC")
    fun getModsForProfile(profileId: String): Flow<List<ModEntity>>

    @Query("SELECT * FROM profile_mod_cross_ref WHERE profileId = :profileId")
    suspend fun getProfileModRefs(profileId: String): List<ProfileModCrossRef>
}
