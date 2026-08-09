package com.example.core

import android.content.Context
import com.example.data.entities.ModEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ModLoader(private val context: Context) {

    val activeModsDir = File(context.filesDir, "active_mods").apply {
        if (!exists()) mkdirs()
    }

    val loaderConfigDir = File(context.filesDir, "ncms_loader").apply {
        if (!exists()) mkdirs()
    }

    suspend fun syncActiveMods(enabledMods: List<ModEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Clean previous active mods directory content
            activeModsDir.listFiles()?.forEach { file ->
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }

            val modsConfigArray = JSONArray()

            // Copy each enabled mod to active_mods directory
            enabledMods.sortedBy { it.loadOrder }.forEachIndexed { index, mod ->
                val sourceDir = File(mod.localPath)
                if (sourceDir.exists()) {
                    val destDir = File(activeModsDir, mod.id)
                    if (sourceDir.isDirectory) {
                        sourceDir.copyRecursively(destDir, overwrite = true)
                    } else {
                        destDir.mkdirs()
                        sourceDir.copyTo(File(destDir, sourceDir.name), overwrite = true)
                    }

                    val modJson = JSONObject().apply {
                        put("id", mod.id)
                        put("modId", mod.modId)
                        put("name", mod.name)
                        put("author", mod.author)
                        put("version", mod.version)
                        put("format", mod.format)
                        put("loadOrder", index)
                        put("enabled", true)
                    }
                    modsConfigArray.put(modJson)
                }
            }

            // Write active configuration file `mods_config.json`
            val configJson = JSONObject().apply {
                put("version", "1.0")
                put("updatedAt", System.currentTimeMillis())
                put("activeModCount", enabledMods.size)
                put("mods", modsConfigArray)
            }

            val configFile = File(activeModsDir, "mods_config.json")
            configFile.writeText(configJson.toString(2))

            // Also mirror to ncms_loader dir for WorldBox Android mod loader hooks
            val ncmsConfigFile = File(loaderConfigDir, "ncms_config.json")
            ncmsConfigFile.writeText(configJson.toString(2))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
