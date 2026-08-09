package com.example.core

import org.json.JSONObject
import java.io.File

data class ModCompatibilityReport(
    val isCompatible: Boolean,
    val format: String,
    val modName: String,
    val modAuthor: String,
    val modVersion: String,
    val description: String,
    val targetGameVersion: String,
    val issues: List<String>,
    val warnings: List<String>,
    val summary: String
)

class CompatibilityChecker {

    fun inspectExtractedMod(modDir: File, gameVersion: String?): ModCompatibilityReport {
        val files = modDir.walkTopDown().toList()
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        var modName = modDir.name
        var author = "Unknown"
        var version = "1.0.0"
        var description = "No description provided."
        var targetGameVer = gameVersion ?: "0.22.x"
        var format = "STANDARD_DIR"

        // Step 1: Search for metadata files (mod.json, manifest.json, ncms.json)
        val manifestFile = files.firstOrNull {
            it.name.equals("mod.json", ignoreCase = true) ||
            it.name.equals("manifest.json", ignoreCase = true) ||
            it.name.equals("ncms.json", ignoreCase = true)
        }

        if (manifestFile != null && manifestFile.exists()) {
            try {
                format = if (manifestFile.name.contains("ncms", ignoreCase = true)) "NCMS" else "JSON_MANIFEST"
                val jsonStr = manifestFile.readText()
                val json = JSONObject(jsonStr)

                modName = json.optString("name", json.optString("title", modName))
                author = json.optString("author", json.optString("creator", author))
                version = json.optString("version", json.optString("mod_version", version))
                description = json.optString("description", description)
                targetGameVer = json.optString("targetGameVersion", json.optString("gameVersion", targetGameVer))

                val requiresNcms = json.optBoolean("requiresNCMS", true)
                if (requiresNcms) {
                    warnings.add("Requires NCMS (Nameable Custom Mod System) mod loader on Android.")
                }
            } catch (e: Exception) {
                warnings.add("Failed to parse mod manifest JSON cleanly: ${e.localizedMessage}")
            }
        }

        // Step 2: Check binary file types
        val dllFiles = files.filter { it.extension.equals("dll", ignoreCase = true) }
        val soFiles = files.filter { it.extension.equals("so", ignoreCase = true) }
        val ncmodFiles = files.filter { it.extension.equals("ncmod", ignoreCase = true) }

        if (ncmodFiles.isNotEmpty()) {
            format = "NCMOD"
        }

        if (soFiles.isNotEmpty()) {
            // Native shared objects
            val hasArm64 = files.any { it.path.contains("arm64-v8a", ignoreCase = true) }
            val hasArmv7 = files.any { it.path.contains("armeabi-v7a", ignoreCase = true) }
            val hasX86 = files.any { it.path.contains("x86", ignoreCase = true) }

            if (!hasArm64 && !hasArmv7 && hasX86) {
                issues.add("Contains x86 native libraries only. Incompatible with ARM Android devices.")
            }
        }

        if (dllFiles.isNotEmpty() && soFiles.isEmpty() && manifestFile == null) {
            // Raw C# desktop DLL without Android NCMS bridge
            warnings.add("Contains C# assembly .dll files. Compatibility depends on Mono/Unity Android runtime or NCMS loader.")
        }

        // Step 3: Check game version compatibility if specified
        if (!gameVersion.isNullOrBlank() && targetGameVer.isNotBlank()) {
            val gameMajor = gameVersion.split(".").take(2).joinToString(".")
            val modMajor = targetGameVer.split(".").take(2).joinToString(".")
            if (gameMajor.isNotEmpty() && modMajor.isNotEmpty() && gameMajor != modMajor) {
                warnings.add("Target game version mismatch: Mod targets $targetGameVer, WorldBox is $gameVersion.")
            }
        }

        val isCompatible = issues.isEmpty()
        val summaryStr = when {
            issues.isNotEmpty() -> "Incompatible: ${issues.joinToString("; ")}"
            warnings.isNotEmpty() -> "Compatible with warnings: ${warnings.joinToString("; ")}"
            else -> "Fully Android compatible ($format)."
        }

        return ModCompatibilityReport(
            isCompatible = isCompatible,
            format = format,
            modName = modName,
            modAuthor = author,
            modVersion = version,
            description = description,
            targetGameVersion = targetGameVer,
            issues = issues,
            warnings = warnings,
            summary = summaryStr
        )
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
