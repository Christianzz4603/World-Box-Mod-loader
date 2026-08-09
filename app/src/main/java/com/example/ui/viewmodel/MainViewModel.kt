package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.*
import com.example.data.AppDatabase
import com.example.data.LauncherRepository
import com.example.data.entities.LauncherLogEntity
import com.example.data.entities.ModEntity
import com.example.data.entities.ProfileEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed class LaunchState {
    object Idle : LaunchState()
    object SyncingMods : LaunchState()
    data class Success(val message: String) : LaunchState()
    data class Error(val message: String, val canImportApk: Boolean = false) : LaunchState()
}

sealed class ApkImportState {
    object Idle : ApkImportState()
    data class Progress(val fraction: Float, val status: String) : ApkImportState()
    data class Success(val info: ManagedApkInfo) : ApkImportState()
    data class Error(val message: String) : ApkImportState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getInstance(application)
    val repository = LauncherRepository(
        modDao = database.modDao(),
        profileDao = database.profileDao(),
        logDao = database.logDao()
    )

    val detector = WorldBoxDetector(application)
    val apkManager = ApkManager(application)
    val archiveExtractor = ArchiveExtractor()
    val compatibilityChecker = CompatibilityChecker()
    val modLoader = ModLoader(application)
    val downloadManager = DownloadManager(application)
    val modInstaller = ModInstaller(application, repository, archiveExtractor, compatibilityChecker)
    val worldBoxLauncher = WorldBoxLauncher(application, detector, apkManager, modLoader)

    // UI States
    private val _worldBoxInfo = MutableStateFlow(detector.detectWorldBox())
    val worldBoxInfo: StateFlow<WorldBoxInfo> = _worldBoxInfo.asStateFlow()

    private val _managedApkInfo = MutableStateFlow(apkManager.getManagedApkInfo())
    val managedApkInfo: StateFlow<ManagedApkInfo> = _managedApkInfo.asStateFlow()

    val mods: StateFlow<List<ModEntity>> = repository.allMods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledMods: StateFlow<List<ModEntity>> = repository.enabledMods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<ProfileEntity>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<ProfileEntity?> = repository.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val logs: StateFlow<List<LauncherLogEntity>> = repository.launcherLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _launchState = MutableStateFlow<LaunchState>(LaunchState.Idle)
    val launchState: StateFlow<LaunchState> = _launchState.asStateFlow()

    private val _apkImportState = MutableStateFlow<ApkImportState>(ApkImportState.Idle)
    val apkImportState: StateFlow<ApkImportState> = _apkImportState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        refreshDetector()
        viewModelScope.launch {
            repository.createDefaultProfileIfNone()
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun refreshDetector() {
        _worldBoxInfo.value = detector.detectWorldBox()
        _managedApkInfo.value = apkManager.getManagedApkInfo()
    }

    fun launchGame() {
        viewModelScope.launch {
            _launchState.value = LaunchState.SyncingMods
            val activeProf = activeProfile.value
            val profileName = activeProf?.name ?: "Default Profile"
            val enabledList = enabledMods.value

            val result = worldBoxLauncher.launchGame(enabledList, profileName)
            when (result) {
                is LaunchResult.Success -> {
                    _launchState.value = LaunchState.Success(result.message)
                    repository.log("SUCCESS", "Launcher", result.message)
                }
                is LaunchResult.Failure -> {
                    _launchState.value = LaunchState.Error(result.reason, result.canImportApk)
                    repository.log("ERROR", "Launcher", result.reason)
                }
            }
        }
    }

    fun resetLaunchState() {
        _launchState.value = LaunchState.Idle
    }

    fun toggleModEnabled(mod: ModEntity) {
        viewModelScope.launch {
            repository.setModEnabled(mod.id, !mod.isEnabled)
        }
    }

    fun deleteMod(mod: ModEntity) {
        viewModelScope.launch {
            // Delete local directory
            val file = File(mod.localPath)
            if (file.exists()) {
                file.deleteRecursively()
            }
            repository.deleteMod(mod)
        }
    }

    fun updateLoadOrder(modId: String, newOrder: Int) {
        viewModelScope.launch {
            repository.updateLoadOrder(modId, newOrder)
        }
    }

    fun createProfile(name: String, description: String) {
        viewModelScope.launch {
            repository.createProfile(name, description)
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            repository.switchActiveProfile(profileId)
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    fun importApkFromUri(uri: Uri) {
        viewModelScope.launch {
            _apkImportState.value = ApkImportState.Progress(0.0f, "Starting import...")
            val result = apkManager.importApkFromUri(uri) { fraction, msg ->
                _apkImportState.value = ApkImportState.Progress(fraction, msg)
            }
            result.fold(
                onSuccess = { info ->
                    _apkImportState.value = ApkImportState.Success(info)
                    _managedApkInfo.value = info
                    repository.log("SUCCESS", "ApkManager", info.statusMessage)
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: "Import failed"
                    _apkImportState.value = ApkImportState.Error(msg)
                    repository.log("ERROR", "ApkManager", msg)
                }
            )
        }
    }

    fun resetApkImportState() {
        _apkImportState.value = ApkImportState.Idle
    }

    fun clearManagedApk() {
        viewModelScope.launch {
            apkManager.clearManagedApk()
            _managedApkInfo.value = apkManager.getManagedApkInfo()
            repository.log("INFO", "ApkManager", "Cleared managed WorldBox APK")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
