package com.example.data

import com.example.data.dao.LogDao
import com.example.data.dao.ModDao
import com.example.data.dao.ProfileDao
import com.example.data.entities.LauncherLogEntity
import com.example.data.entities.ModEntity
import com.example.data.entities.ProfileEntity
import com.example.data.entities.ProfileModCrossRef
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LauncherRepository(
    private val modDao: ModDao,
    private val profileDao: ProfileDao,
    private val logDao: LogDao
) {
    val allMods: Flow<List<ModEntity>> = modDao.getAllMods()
    val enabledMods: Flow<List<ModEntity>> = modDao.getEnabledMods()
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()
    val activeProfile: Flow<ProfileEntity?> = profileDao.getActiveProfile()
    val launcherLogs: Flow<List<LauncherLogEntity>> = logDao.getAllLogs()

    suspend fun log(level: String, tag: String, message: String) {
        logDao.insertLog(
            LauncherLogEntity(
                level = level,
                tag = tag,
                message = message
            )
        )
    }

    suspend fun clearLogs() = logDao.clearLogs()

    suspend fun insertMod(mod: ModEntity) {
        modDao.insertMod(mod)
        log("INFO", "ModRepository", "Inserted/Updated mod: ${mod.name} (${mod.version})")
    }

    suspend fun deleteMod(mod: ModEntity) {
        modDao.deleteMod(mod)
        log("WARN", "ModRepository", "Deleted mod: ${mod.name}")
    }

    suspend fun setModEnabled(id: String, isEnabled: Boolean) {
        modDao.setModEnabled(id, isEnabled)
        log("INFO", "ModRepository", "Set mod $id enabled: $isEnabled")
    }

    suspend fun updateLoadOrder(id: String, order: Int) {
        modDao.updateLoadOrder(id, order)
    }

    suspend fun createDefaultProfileIfNone() {
        val active = profileDao.getActiveProfileSync()
        if (active == null) {
            val defaultProfile = ProfileEntity(
                id = UUID.randomUUID().toString(),
                name = "Default Profile",
                description = "Standard mod set for WorldBox",
                isActive = true
            )
            profileDao.insertProfile(defaultProfile)
            log("INFO", "ProfileRepository", "Created initial Default Profile")
        }
    }

    suspend fun createProfile(name: String, description: String): String {
        val id = UUID.randomUUID().toString()
        val profile = ProfileEntity(
            id = id,
            name = name,
            description = description,
            isActive = false
        )
        profileDao.insertProfile(profile)
        log("INFO", "ProfileRepository", "Created profile: $name")
        return id
    }

    suspend fun switchActiveProfile(profileId: String) {
        profileDao.switchActiveProfile(profileId)
        log("INFO", "ProfileRepository", "Switched active profile to: $profileId")
    }

    suspend fun deleteProfile(profile: ProfileEntity) {
        if (profile.isActive) {
            log("WARN", "ProfileRepository", "Cannot delete active profile")
            return
        }
        profileDao.deleteProfile(profile)
        log("WARN", "ProfileRepository", "Deleted profile: ${profile.name}")
    }
}
