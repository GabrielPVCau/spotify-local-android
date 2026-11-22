package com.spotifylocal.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.spotifylocal.android.ui.components.FullPlayerScreen
import com.spotifylocal.android.ui.components.MiniPlayer
import com.spotifylocal.android.ui.screens.HomeScreen
import com.spotifylocal.android.ui.screens.OnboardingScreen
import com.spotifylocal.android.ui.screens.PlaylistDetailScreen
import com.spotifylocal.android.ui.screens.PlaylistsScreen
import com.spotifylocal.android.ui.screens.SearchScreen
import com.spotifylocal.android.ui.theme.SpotifyBlack
import com.spotifylocal.android.ui.theme.SpotifyGreen
import com.spotifylocal.android.ui.theme.SpotifySurface
import com.spotifylocal.android.ui.theme.SpotifyTextPrimary
import com.spotifylocal.android.ui.theme.SpotifyTextSecondary

@Composable
fun SpotifyLocalApp(
    viewModel: MainViewModel,
    onPickFolder: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    if (uiState.folderUri == null) {
        OnboardingScreen(onPickFolder = onPickFolder)
        return
    }

    val currentTrack = remember(uiState.allTracks, playerState.currentTrackRelPath) {
        uiState.allTracks.firstOrNull { it.relPath == playerState.currentTrackRelPath }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = SpotifyBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    if (currentTrack != null) {
                        MiniPlayer(
                            track = currentTrack,
                            playerState = playerState,
                            onPlayPause = { viewModel.playerController.togglePlayPause() },
                            onNext = { viewModel.playerController.next() },
                            onToggleLike = { viewModel.toggleLike(currentTrack) },
                            onClick = { viewModel.setFullPlayerExpanded(true) }
                        )
                    }
                    NavigationBar(containerColor = SpotifySurface) {
                        BottomTab.entries.forEach { tab ->
                            val icon = when (tab) {
                                BottomTab.HOME -> Icons.Default.Home
                                BottomTab.PLAYLISTS -> Icons.Default.LibraryMusic
                                BottomTab.SEARCH -> Icons.Default.Search
                            }
                            NavigationBarItem(
                                selected = uiState.currentTab == tab && uiState.selectedPlaylistId == null,
                                onClick = { viewModel.selectTab(tab) },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SpotifyGreen,
                                    selectedTextColor = SpotifyGreen,
                                    unselectedIconColor = SpotifyTextSecondary,
                                    unselectedTextColor = SpotifyTextSecondary,
                                    indicatorColor = SpotifyBlack
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val selectedPlaylist = uiState.selectedPlaylistId?.let { id ->
                    uiState.playlists.firstOrNull { it.id == id }
                }

                if (selectedPlaylist != null) {
                    val displayedTracks = viewModel.getSortedFilteredTracks(
                        tracks = selectedPlaylist.tracks,
                        sortOption = uiState.sortOption,
                        filterQuery = uiState.playlistFilterQuery
                    )
                    PlaylistDetailScreen(
                        playlist = selectedPlaylist,
                        tracks = displayedTracks,
                        currentTrackRelPath = playerState.currentTrackRelPath,
                        sortOption = uiState.sortOption,
                        pinnedSortOption = uiState.pinnedSortOption,
                        filterQuery = uiState.playlistFilterQuery,
                        onBack = { viewModel.closePlaylist() },
                        onPlayAll = { viewModel.playTracks(displayedTracks, 0, shuffle = false) },
                        onShuffleAll = { viewModel.playTracks(displayedTracks, 0, shuffle = true) },
                        onTrackClick = { index -> viewModel.playTracks(displayedTracks, index, shuffle = false) },
                        onToggleLike = { viewModel.toggleLike(it) },
                        onSortSelected = { viewModel.setSortOption(it) },
                        onPinSort = { viewModel.pinCurrentSortAsDefault() },
                        onFilterChange = { viewModel.setPlaylistFilterQuery(it) },
                        onTogglePlaylistPin = { viewModel.togglePlaylistPin(selectedPlaylist) }
                    )
                } else {
                    when (uiState.currentTab) {
                        BottomTab.HOME -> HomeScreen(
                            uiState = uiState,
                            currentTrackRelPath = playerState.currentTrackRelPath,
                            onOpenPlaylist = { viewModel.openPlaylist(it) },
                            onPlayTrack = { tracks, index -> viewModel.playTracks(tracks, index) },
                            onToggleLike = { viewModel.toggleLike(it) },
                            onRescan = { viewModel.rescan() },
                            onChangeFolder = onPickFolder
                        )
                        BottomTab.PLAYLISTS -> PlaylistsScreen(
                            playlists = uiState.playlists,
                            isScanning = uiState.isScanning,
                            onOpenPlaylist = { viewModel.openPlaylist(it) },
                            onRescan = { viewModel.rescan() }
                        )
                        BottomTab.SEARCH -> SearchScreen(
                            query = uiState.searchQuery,
                            allTracks = uiState.allTracks,
                            playlists = uiState.playlists,
                            currentTrackRelPath = playerState.currentTrackRelPath,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onOpenPlaylist = { viewModel.openPlaylist(it) },
                            onPlayTrack = { tracks, index -> viewModel.playTracks(tracks, index) },
                            onToggleLike = { viewModel.toggleLike(it) }
                        )
                    }
                }
            }
        }

        if (uiState.isFullPlayerExpanded && currentTrack != null) {
            FullPlayerScreen(
                track = currentTrack,
                playerState = playerState,
                onDismiss = { viewModel.setFullPlayerExpanded(false) },
                onPlayPause = { viewModel.playerController.togglePlayPause() },
                onNext = { viewModel.playerController.next() },
                onPrevious = { viewModel.playerController.previous() },
                onSeek = { viewModel.playerController.seekTo(it) },
                onToggleShuffle = { viewModel.playerController.toggleShuffle() },
                onCycleRepeat = { viewModel.playerController.cycleRepeatMode() },
                onToggleLike = { viewModel.toggleLike(currentTrack) }
            )
        }
    }
}
