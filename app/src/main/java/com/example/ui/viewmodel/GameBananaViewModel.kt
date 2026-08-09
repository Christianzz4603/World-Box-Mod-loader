package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DownloadState
import com.example.network.GbModFile
import com.example.network.GbModItem
import com.example.network.GameBananaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GbUiState {
    object Loading : GbUiState()
    data class Success(val mods: List<GbModItem>, val page: Int, val hasMore: Boolean) : GbUiState()
    data class Error(val message: String) : GbUiState()
}

sealed class GbDownloadProgressState {
    object Idle : GbDownloadProgressState()
    data class Downloading(
        val modName: String,
        val fileName: String,
        val progressFraction: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long,
        val statusMessage: String
    ) : GbDownloadProgressState()
    data class Completed(val modName: String, val message: String) : GbDownloadProgressState()
    data class Error(val modName: String, val message: String) : GbDownloadProgressState()
}

class GameBananaViewModel(application: Application) : AndroidViewModel(application) {

    private val gbRepository = GameBananaRepository()

    private val _uiState = MutableStateFlow<GbUiState>(GbUiState.Loading)
    val uiState: StateFlow<GbUiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<GbDownloadProgressState>(GbDownloadProgressState.Idle)
    val downloadState: StateFlow<GbDownloadProgressState> = _downloadState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSort = MutableStateFlow("new") // "new", "popular", "rating"
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    private val _selectedModDetails = MutableStateFlow<Pair<GbModItem, List<GbModFile>>?>(null)
    val selectedModDetails: StateFlow<Pair<GbModItem, List<GbModFile>>?> = _selectedModDetails.asStateFlow()

    private val _isLoadingDetails = MutableStateFlow(false)
    val isLoadingDetails: StateFlow<Boolean> = _isLoadingDetails.asStateFlow()

    private var currentPage = 1

    init {
        loadMods()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        currentPage = 1
        loadMods()
    }

    fun setSort(sort: String) {
        _selectedSort.value = sort
        currentPage = 1
        loadMods()
    }

    fun loadMods(page: Int = 1) {
        viewModelScope.launch {
            if (page == 1) {
                _uiState.value = GbUiState.Loading
            }
            currentPage = page
            val result = gbRepository.fetchWorldBoxMods(
                page = page,
                sort = _selectedSort.value,
                search = _searchQuery.value
            )
            result.fold(
                onSuccess = { items ->
                    _uiState.value = GbUiState.Success(
                        mods = items,
                        page = page,
                        hasMore = items.size >= 10
                    )
                },
                onFailure = { error ->
                    _uiState.value = GbUiState.Error(error.localizedMessage ?: "Failed to connect to GameBanana.")
                }
            )
        }
    }

    fun selectModForDetails(modItem: GbModItem) {
        viewModelScope.launch {
            _isLoadingDetails.value = true
            val result = gbRepository.getModFilesAndDetails(modItem.id)
            result.fold(
                onSuccess = { (profile, files) ->
                    val resolvedFiles = if (files.isNotEmpty()) {
                        files
                    } else {
                        // Construct fallback file entry if profile files array was empty or differently formatted
                        listOf(
                            GbModFile(
                                id = modItem.id,
                                fileName = "${modItem.name ?: "Mod"}_v${modItem.version ?: "1.0"}.zip",
                                fileSize = 0L,
                                downloadUrl = "https://gamebanana.com/dl/${modItem.id}",
                                description = modItem.teaser ?: "WorldBox Mod Archive"
                            )
                        )
                    }
                    _selectedModDetails.value = Pair(modItem, resolvedFiles)
                },
                onFailure = {
                    // Fallback details if profile API endpoint fails
                    val fallbackFiles = listOf(
                        GbModFile(
                            id = modItem.id,
                            fileName = "${modItem.name ?: "Mod"}.zip",
                            fileSize = 0L,
                            downloadUrl = "https://gamebanana.com/dl/${modItem.id}",
                            description = "Direct GameBanana download"
                        )
                    )
                    _selectedModDetails.value = Pair(modItem, fallbackFiles)
                }
            )
            _isLoadingDetails.value = false
        }
    }

    fun clearSelectedModDetails() {
        _selectedModDetails.value = null
    }

    fun downloadAndInstallMod(
        mainVm: MainViewModel,
        modItem: GbModItem,
        file: GbModFile
    ) {
        viewModelScope.launch {
            val modName = modItem.name ?: "Mod"
            _downloadState.value = GbDownloadProgressState.Downloading(
                modName = modName,
                fileName = file.fileName,
                progressFraction = 0.0f,
                bytesDownloaded = 0L,
                totalBytes = file.fileSize,
                speedBytesPerSec = 0L,
                statusMessage = "Connecting to GameBanana download server..."
            )

            val downloadFlow = mainVm.downloadManager.downloadFile(
                url = file.downloadUrl,
                targetFileName = "${modItem.id}_${file.fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}",
                expectedMd5 = file.md5Checksum
            )

            downloadFlow.collect { state ->
                when (state) {
                    is DownloadState.Progress -> {
                        _downloadState.value = GbDownloadProgressState.Downloading(
                            modName = modName,
                            fileName = file.fileName,
                            progressFraction = state.progressFraction,
                            bytesDownloaded = state.bytesDownloaded,
                            totalBytes = state.totalBytes,
                            speedBytesPerSec = state.speedBytesPerSec,
                            statusMessage = "Downloading: ${state.bytesDownloaded / 1024} KB / ${if (state.totalBytes > 0) state.totalBytes / 1024 else '?'} KB"
                        )
                    }
                    is DownloadState.Success -> {
                        _downloadState.value = GbDownloadProgressState.Downloading(
                            modName = modName,
                            fileName = file.fileName,
                            progressFraction = 0.9f,
                            bytesDownloaded = state.downloadedFile.length(),
                            totalBytes = state.downloadedFile.length(),
                            speedBytesPerSec = 0L,
                            statusMessage = "Extracting & validating Android compatibility..."
                        )

                        val installResult = mainVm.modInstaller.installModFromArchive(
                            archiveFile = state.downloadedFile,
                            gameBananaId = modItem.id,
                            gameVersion = mainVm.worldBoxInfo.value.versionName,
                            iconUrl = modItem.previewMedia?.images?.firstOrNull()?.getFullUrl()
                        ) { progress, statusMsg ->
                            _downloadState.value = GbDownloadProgressState.Downloading(
                                modName = modName,
                                fileName = file.fileName,
                                progressFraction = 0.9f + (progress * 0.1f),
                                bytesDownloaded = state.downloadedFile.length(),
                                totalBytes = state.downloadedFile.length(),
                                speedBytesPerSec = 0L,
                                statusMessage = statusMsg
                            )
                        }

                        when (installResult) {
                            is com.example.core.InstallResult.Success -> {
                                _downloadState.value = GbDownloadProgressState.Completed(
                                    modName = modName,
                                    message = installResult.message
                                )
                            }
                            is com.example.core.InstallResult.Failure -> {
                                _downloadState.value = GbDownloadProgressState.Error(
                                    modName = modName,
                                    message = installResult.reason
                                )
                            }
                        }
                    }
                    is DownloadState.Error -> {
                        _downloadState.value = GbDownloadProgressState.Error(
                            modName = modName,
                            message = state.message
                        )
                    }
                }
            }
        }
    }

    fun dismissDownloadProgress() {
        _downloadState.value = GbDownloadProgressState.Idle
    }
}
