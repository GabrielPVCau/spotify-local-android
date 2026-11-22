package com.spotifylocal.android.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotifylocal.android.data.MergedSyncData
import com.spotifylocal.android.data.MusicScanner
import com.spotifylocal.android.data.Playlist
import com.spotifylocal.android.data.PreferencesRepository
import com.spotifylocal.android.data.SortOption
import com.spotifylocal.android.data.SyncManager
import com.spotifylocal.android.data.Track
import com.spotifylocal.android.playback.PlayerController
import com.spotifylocal.android.playback.PlayerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BottomTab(val label: String) {
    HOME("Início"),
    PLAYLISTS("Playlists"),
    SEARCH("Buscar")
}

data class MainUiState(
    val folderUri: Uri? = null,
    val folderName: String = "",
    val isScanning: Boolean = false,
    val allTracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val currentTab: BottomTab = BottomTab.HOME,
    val selectedPlaylistId: String? = null,
    val sortOption: SortOption = SortOption.DEFAULT,
    val pinnedSortOption: SortOption = SortOption.DEFAULT,
    val playlistFilterQuery: String = "",
    val searchQuery: String = "",
    val isFullPlayerExpanded: Boolean = false,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    private val syncManager = SyncManager(application, prefs.getOrCreateDeviceId())
    private val scanner = MusicScanner(application, syncManager)

    val playerController = PlayerController(application) { completedRelPath ->
        onTrackCompleted(completedRelPath)
    }

    val playerState: StateFlow<PlayerUiState> = playerController.state

    private val _uiState = MutableStateFlow(
        MainUiState(
            folderUri = prefs.getMusicFolderUri(),
            sortOption = prefs.getPinnedSortOption(),
            pinnedSortOption = prefs.getPinnedSortOption()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        playerController.connect()
        _uiState.value.folderUri?.let { uri ->
            scanFolder(uri)
        }
    }

    fun onFolderSelected(uri: Uri) {
        prefs.setMusicFolderUri(uri)
        _uiState.update { it.copy(folderUri = uri, selectedPlaylistId = null) }
        scanFolder(uri)
    }

    fun rescan() {
        val uri = _uiState.value.folderUri ?: return
        scanFolder(uri)
    }

    private fun scanFolder(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, statusMessage = null) }
            val result = scanner.scanLibrary(uri)
            _uiState.update {
                it.copy(
                    isScanning = false,
                    allTracks = result.allTracks,
                    playlists = result.playlists,
                    folderName = result.folderName
                )
            }
        }
    }

    fun selectTab(tab: BottomTab) {
        _uiState.update {
            it.copy(
                currentTab = tab,
                selectedPlaylistId = null,
                playlistFilterQuery = ""
            )
        }
    }

    fun openPlaylist(playlistId: String) {
        _uiState.update {
            it.copy(
                selectedPlaylistId = playlistId,
                playlistFilterQuery = "",
                sortOption = it.pinnedSortOption
            )
        }
    }

    fun closePlaylist() {
        _uiState.update {
            it.copy(selectedPlaylistId = null, playlistFilterQuery = "")
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun pinCurrentSortAsDefault() {
        val current = _uiState.value.sortOption
        prefs.setPinnedSortOption(current)
        _uiState.update {
            it.copy(
                pinnedSortOption = current,
                statusMessage = "Ordenação padrão fixada: ${current.label}"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun setPlaylistFilterQuery(query: String) {
        _uiState.update { it.copy(playlistFilterQuery = query) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFullPlayerExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isFullPlayerExpanded = expanded) }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        playerController.playQueue(tracks, startIndex, shuffle)
    }

    fun toggleLike(track: Track) {
        val folderUri = _uiState.value.folderUri ?: return
        val newLiked = !track.liked
        viewModelScope.launch {
            val merged = syncManager.setTrackLiked(folderUri, track.relPath, newLiked)
            applyMergedSyncData(merged)
        }
    }

    fun togglePlaylistPin(playlist: Playlist) {
        if (playlist.isSpecial) return
        val folderUri = _uiState.value.folderUri ?: return
        viewModelScope.launch {
            val merged = syncManager.setPlaylistPinned(
                rootTreeUri = folderUri,
                playlistId = playlist.id,
                pinned = !playlist.pinned,
                sortOrder = playlist.sortOrder
            )
            applyMergedSyncData(merged)
        }
    }

    private fun onTrackCompleted(relPath: String) {
        val folderUri = _uiState.value.folderUri ?: return
        viewModelScope.launch {
            val merged = syncManager.recordPlay(folderUri, relPath)
            applyMergedSyncData(merged)
        }
    }

    private fun applyMergedSyncData(merged: MergedSyncData) {
        _uiState.update { state ->
            val updatedAll = state.allTracks.map { track ->
                val stats = merged.tracks[track.relPath]
                if (stats != null) {
                    track.copy(
                        playCount = stats.playCount,
                        lastPlayedAt = stats.lastPlayedAt,
                        liked = stats.liked,
                        likedUpdatedAt = stats.likedUpdatedAt
                    )
                } else {
                    track
                }
            }
            val trackMap = updatedAll.associateBy { it.relPath }
            val updatedPlaylists = state.playlists.map { playlist ->
                when (playlist.id) {
                    MusicScanner.PLAYLIST_LIKED_ID -> playlist.copy(
                        tracks = updatedAll.filter { it.liked }
                    )
                    MusicScanner.PLAYLIST_ALL_ID -> playlist.copy(
                        tracks = updatedAll
                    )
                    else -> {
                        val meta = merged.playlists[playlist.id]
                        playlist.copy(
                            tracks = playlist.tracks.map { trackMap[it.relPath] ?: it },
                            pinned = meta?.pinned ?: playlist.pinned,
                            sortOrder = meta?.sortOrder ?: playlist.sortOrder
                        )
                    }
                }
            }
            state.copy(allTracks = updatedAll, playlists = updatedPlaylists)
        }
    }

    fun getSortedFilteredTracks(
        tracks: List<Track>,
        sortOption: SortOption,
        filterQuery: String
    ): List<Track> {
        val filtered = if (filterQuery.isBlank()) {
            tracks
        } else {
            val q = filterQuery.trim().lowercase()
            tracks.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }

        return when (sortOption) {
            SortOption.DEFAULT -> filtered
            SortOption.MOST_PLAYED -> filtered.sortedWith(
                compareByDescending<Track> { it.playCount }.thenBy { it.title.lowercase() }
            )
            SortOption.LEAST_PLAYED -> filtered.sortedWith(
                compareBy<Track> { it.playCount }.thenBy { it.title.lowercase() }
            )
            SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOption.ARTIST_ASC -> filtered.sortedWith(
                compareBy<Track> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            )
            SortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.lastModified }
        }
    }

    override fun onCleared() {
        playerController.release()
        super.onCleared()
    }
}
